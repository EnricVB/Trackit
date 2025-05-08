package dev.enric.core.handler.remote

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.admin.RemoteDirectivesConfig
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.*
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.handler.RemoteClientListener
import dev.enric.remote.network.handler.RemoteConnection
import dev.enric.remote.packet.message.PushMessage
import dev.enric.remote.packet.message.data.BranchSyncStatusQueryData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus
import dev.enric.remote.packet.message.data.MissingObjectCheckData
import dev.enric.remote.packet.message.data.PushData
import dev.enric.remote.packet.query.BranchSyncStatusQueryMessage
import dev.enric.remote.packet.query.MissingObjectCheckQueryMessage
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.BranchSyncStatusResponseMessage
import dev.enric.remote.packet.response.MissingObjectCheckResponseMessage
import dev.enric.remote.packet.response.StatusResponseMessage
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import java.net.Socket

/**
 * Handles the logic behind the `push` command.
 * This includes establishing a remote connection, querying the remote status,
 * determining which commits are missing on the remote, and collecting the set of objects
 * (commits, trees, contents, tags, and branches) that need to be pushed.
 */
class PushHandler(val pushDirection: DataProtocol) : CommandHandler() {


    /**
     * Establishes and returns a socket connection to the remote.
     * Also starts the client listener for incoming messages.
     */
    fun connectToRemote(): Socket {
        val socket = StartSSHRemote().connection(
            username = pushDirection.user ?: error("Missing username in push URL configuration."),
            password = pushDirection.password ?: error("Missing password in push URL configuration."),
            host = pushDirection.host ?: error("Missing host in push URL configuration."),
            port = pushDirection.port ?: 8088,
            path = pushDirection.path ?: error("Missing path in push URL configuration.")
        )

        RemoteClientListener(RemoteConnection(socket)).start()
        return socket
    }

    /**
     * Sends to the remote all missing objects for the current branch:
     * - Commits
     * - Trees
     * - Contents
     * - Tags
     * - Branch metadata
     */
    suspend fun pushBranchObjects(socket: Socket, branch: Branch) {
        val objectsToPush = collectPushableObjects(socket, branch)

        val formatted = objectsToPush.map { (hash, bytes) ->
            Hash.HashType.fromHash(hash) to bytes
        }

        val data = PushData(
            objects = formatted,
            branchHeadHash = BranchIndex.getBranchHead(branch.generateKey()).generateKey().toString(),
            branchHash = branch.generateKey().toString()
        )

        RemoteChannel(socket).send(PushMessage(data))
    }

    /**
     * Gathers the objects needed for push based on what's missing remotely.
     */
    private suspend fun collectPushableObjects(socket: Socket, branch: Branch): HashMap<Hash, ByteArray> {
        val remoteHead = fetchRemoteHeadCommit(socket)
        val missingCommits = obtainPosteriorCommits(remoteHead, branch)
        return obtainBranchPushObjects(missingCommits)
    }

    /**
     * Requests the latest commit hash of the current branch from the remote.
     */
    private suspend fun fetchRemoteHeadCommit(socket: Socket): Hash {
        val branchName = BranchIndex.getCurrentBranch().name
        val response = RemoteChannel(socket).request<StatusResponseMessage>(
            StatusQueryMessage(branchName)
        )
        return Hash(response.payload)
    }

    /**
     * Determines which commits on the current branch are newer than the given remote commit hash.
     *
     * @param remoteCommitHash The last known commit hash on the remote side.
     * @param branch The current branch being pushed.
     * @return A list of commit [Hash]es that are ahead of the given one.
     */
    private fun obtainPosteriorCommits(remoteCommitHash: Hash, branch: Branch): List<Hash> {
        val commits = mutableListOf<Hash>()
        val branchHead = BranchIndex.getBranchHead(branch.generateKey())

        var currentCommit: Commit? = branchHead
        var currentCommitHash = currentCommit?.generateKey()

        while (currentCommit != null && currentCommitHash != remoteCommitHash) {
            if (currentCommitHash != null) {
                commits.add(currentCommitHash)
            }

            currentCommitHash = currentCommit.previousCommit
            currentCommit = currentCommit.previousCommit?.let { Commit.newInstance(it) }
        }

        return commits
    }

    /**
     * Collects all Trackit objects that need to be pushed to the remote,
     * based on the list of new commits.
     *
     * This includes:
     * - Commits
     * - Trees
     * - Contents
     * - Tags (Simple and Complex)
     * - Branch associated with each commit
     *
     * @param commitsToPush A list of commit hashes that need to be pushed.
     * @return A map from object [Hash] to its serialized byte representation.
     */
    private fun obtainBranchPushObjects(commitsToPush: List<Hash>): HashMap<Hash, ByteArray> {
        val objectsToPush = HashMap<Hash, ByteArray>()

        for (commitHash in commitsToPush) {
            // Add the commit object
            val commit = Commit.newInstance(commitHash)
            objectsToPush[commitHash] = commit.encode().second

            // Add the tree and its content
            commit.tree.forEach {
                val tree = Tree.newInstance(it)
                objectsToPush[tree.generateKey()] = tree.encode().second

                val content = Content.newInstance(tree.content)
                objectsToPush[content.generateKey()] = content.encode().second
            }

            // Add associated tags
            TagIndex.getTagsByCommit(commitHash).forEach {
                val tag = when (Hash.HashType.fromHash(it)) {
                    SIMPLE_TAG -> SimpleTag.newInstance(it)
                    COMPLEX_TAG -> ComplexTag.newInstance(it)
                    else -> return@forEach
                }
                objectsToPush[tag.generateKey()] = tag.encode().second
            }

            // Add branch object
            val branch = Branch.newInstance(commit.branch)
            objectsToPush[branch.generateKey()] = branch.encode().second
        }

        return objectsToPush
    }

    /**
     * Queries the remote server for the synchronization status of the current local branch.
     *
     * @param socket An open socket connected to the remote server.
     *
     * @return [BranchSyncStatus] representing the state of synchronization:
     * - [BranchSyncStatus.SYNCED]: No differences between local and remote.
     * - [BranchSyncStatus.AHEAD]: Local branch has commits not present in remote.
     * - [BranchSyncStatus.BEHIND]: Remote branch has commits not present locally.
     * - [BranchSyncStatus.DIVERGED]: Both branches have diverged.
     * - [BranchSyncStatus.ONLY_LOCAL]: Local branch doesn't exist remotely.
     * - [BranchSyncStatus.ONLY_REMOTE]: Remote branch doesn't exist locally.
     */
    suspend fun askForRemoteBranchStatus(socket: Socket): BranchSyncStatus {
        val localBranches = BranchIndex.getAllBranches()

        val commitMap = localBranches.associate { branchKey ->
            val branch = Branch.newInstance(branchKey)
            val head = BranchIndex.getBranchHead(branch.generateKey())
            val commits = getCommitListFrom(head.generateKey().string)
            branch.name to commits
        }

        val response = RemoteChannel(socket).request<BranchSyncStatusResponseMessage>(
            BranchSyncStatusQueryMessage(BranchSyncStatusQueryData(commitMap.toList()))
        )

        val currentBranchName = BranchIndex.getCurrentBranch().name
        val statusEntry = response.payload.objects
            .firstOrNull { (name, _, _) -> name == currentBranchName }

        return when {
            response.payload.objects.isEmpty() -> BranchSyncStatus.ONLY_LOCAL
            statusEntry != null -> statusEntry.second
            else -> BranchSyncStatus.ONLY_REMOTE
        }
    }

    /**
     * Get the list of commits from a given hash.
     */
    fun getCommitListFrom(startHash: String): List<String> {
        val commitList = mutableListOf<Hash>()
        var currentHash: Hash? = Hash(startHash)

        while (currentHash != null) {
            commitList.add(currentHash)

            val commit = Commit.newInstance(currentHash)
            currentHash = commit.previousCommit
        }

        return commitList.map { it.string }
    }

    suspend fun askForMissingData(socket: Socket, branch: Branch, remoteHead: Hash?): List<Pair<Hash, ByteArray>> {
        val newData = calculateNewObjects(
            localBranch = branch,
            remoteHead = remoteHead
        ).map { (hash, byteContent) ->
            hash.string to byteContent
        }

        return RemoteChannel(socket).request<MissingObjectCheckResponseMessage>(
            MissingObjectCheckQueryMessage(MissingObjectCheckData(newData))
        ).payload.objects.map { (hashStr, byteContent) ->
            Hash(hashStr) to byteContent
        }
    }

    fun calculateNewObjects(localBranch: Branch, remoteHead: Hash?): List<Pair<Hash, ByteArray>> {
        val newData = mutableListOf<Pair<Hash, ByteArray>>()
        val localBranchHead = BranchIndex.getBranchHead(localBranch.generateKey()).generateKey()

        val localCommits = getCommitListFrom(localBranchHead.string)
        val remoteCommits = remoteHead?.string?.let { getCommitListFrom(it) } ?: emptyList()

        val newCommits = localCommits.filterNot { it in remoteCommits }.map { Hash(it) }

        for (commitHash in newCommits) {
            // Add the commit object
            val commit = Commit.newInstance(commitHash)
            val author = User.newInstance(commit.author)
            val confirmer = User.newInstance(commit.confirmer)

            val authorBranchPermissions = author.roles.flatMap { Role.newInstance(it).getBranchPermissions() }
            val confirmerBranchPermissions = confirmer.roles.flatMap { Role.newInstance(it).getBranchPermissions() }

            val authorRolePermissions = author.roles.flatMap { Role.newInstance(it).getRolePermissions() }
            val confirmerRolePermissions = confirmer.roles.flatMap { Role.newInstance(it).getRolePermissions() }

            val roles = (author.roles + confirmer.roles).map { Role.newInstance(it) }
            val branchPermissions = (authorBranchPermissions + confirmerBranchPermissions).distinct()
            val rolePermissions = (authorRolePermissions + confirmerRolePermissions).distinct()

            // Add to new data
            newData.add(Pair(author.generateKey(), author.encode().second))
            newData.add(Pair(confirmer.generateKey(), confirmer.encode().second))

            for (role in roles) {
                newData.add(Pair(role.generateKey(), role.encode().second))
            }

            for (permission in branchPermissions) {
                newData.add(Pair(permission.generateKey(), permission.encode().second))
            }

            for (permission in rolePermissions) {
                newData.add(Pair(permission.generateKey(), permission.encode().second))
            }
        }

        return newData
    }

    suspend fun sendMissingData(socket: Socket, missingData: List<Pair<Hash.HashType, ByteArray>>) {
        val data = PushData(
            objects = missingData,
            branchHeadHash = BranchIndex.getBranchHead(BranchIndex.getCurrentBranch().generateKey()).generateKey()
                .toString(),
            branchHash = BranchIndex.getCurrentBranch().generateKey().toString()
        )

        RemoteChannel(socket).send(PushMessage(data))

    }

    /**
     * Checks if the auto-push directive is enabled.
     */
    fun isAutoPushEnabled(): Boolean {
        return RemoteDirectivesConfig().loadAutopushDirective()
    }
}
