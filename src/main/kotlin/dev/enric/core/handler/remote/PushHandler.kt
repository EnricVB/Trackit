package dev.enric.core.handler.remote

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.admin.RemoteDirectivesConfig
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.handler.RemoteClientListener
import dev.enric.remote.network.handler.RemoteConnection
import dev.enric.remote.packet.message.PushMessage
import dev.enric.remote.packet.message.data.BranchSyncStatusQueryData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus
import dev.enric.remote.packet.message.data.PushData
import dev.enric.remote.packet.query.BranchSyncStatusQueryMessage
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.BranchSyncStatusResponseMessage
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
class PushHandler(
    val pushDirection: DataProtocol,
) : CommandHandler() {

    /**
     * Starts a remote SSH connection to the given host and folder path,
     * and launches a listener for incoming messages from the remote.
     *
     * @return A connected [Socket] to the remote server.
     */
    fun startRemoteConnection(): Socket {
        val socket = StartSSHRemote().connection(
            username = pushDirection.user
                ?: throw IllegalArgumentException("Username is required. Configure the Push URL in the config file."),
            password = pushDirection.password
                ?: throw IllegalArgumentException("Password is required. Configure the Push URL in the config file."),
            host = pushDirection.host
                ?: throw IllegalArgumentException("Host is required. Configure the Push URL in the config file."),
            port = pushDirection.port ?: 8088,
            path = pushDirection.path
                ?: throw IllegalArgumentException("Path is required. Configure the Push URL in the config file.")
        )

        RemoteClientListener(RemoteConnection(socket)).start()
        return socket
    }

    /**
     * Obtains the objects that need to be pushed to the remote server.
     * This includes commits, trees, contents, tags, and branches.
     *
     * @param socket The open socket connected to the remote server.
     * @param branch The current branch being pushed.
     * @return A map from object [Hash] to its serialized byte representation.
     */
    suspend fun obtainBranchPushObjects(socket: Socket, branch: Branch): HashMap<Hash, ByteArray> {
        val commitsToPush = obtainPosteriorCommits(
            remoteCommitHash = askForCommitHash(socket),
            branch = branch
        )

        return obtainBranchPushObjects(commitsToPush)
    }

    /**
     * Sends the objects to the remote server.
     *
     * @param socket The open socket connected to the remote server.
     * @param currentBranch The current branch being pushed.
     */
    suspend fun sendObjectsToRemote(socket: Socket, currentBranch: Branch) {
        val objectsToPush = obtainBranchPushObjects(socket, currentBranch)
        val formattedObjects = objectsToPush.map { (hash, bytes) ->
            Pair(Hash.HashType.fromHash(hash), bytes)
        }
        val branchHead = BranchIndex.getBranchHead(currentBranch.generateKey()).generateKey()
        val branchHash = currentBranch.generateKey()

        val data = PushData(
            objects = formattedObjects,
            branchHeadHash = branchHead.toString(),
            branchHash = branchHash.toString()
        )

        RemoteChannel(socket).send(PushMessage(data))
    }

    /**
     * Asks the remote server for the latest commit hash of the current branch.
     *
     * @param socket The open socket connected to the remote server.
     * @return The [Hash] of the latest commit on the remote side.
     */
    private suspend fun askForCommitHash(socket: Socket): Hash {
        return Hash(
            RemoteChannel(socket).request<StatusResponseMessage>(
                message = StatusQueryMessage(BranchIndex.getCurrentBranch().name)
            ).payload
        )
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
     * Asks the remote server for any pending pull requests on the current branch.
     *
     * @param socket The open socket connected to the remote server.
     *
     * @return A list of [Hash]es representing the pending commit's on the pull requests.
     *
     * Returns [BranchSyncStatus.SYNCED] if there are no pending pull requests.
     *
     * Returns [BranchSyncStatus.ONLY_LOCAL] if the local branch is not found on the remote.
     *
     * Returns [BranchSyncStatus.ONLY_REMOTE] if the remote branch is not found locally.
     *
     * Returns [BranchSyncStatus.DIVERGED] if the local and remote branches have diverged.
     *
     * Returns [BranchSyncStatus.AHEAD] if the local branch is ahead of the remote.
     *
     * Returns [BranchSyncStatus.BEHIND] if the local branch is behind the remote.
     */
    suspend fun askForRemoteBranchStatus(socket: Socket): BranchSyncStatus {
        val branchNames = BranchIndex.getAllBranches().map { Branch.newInstance(it).name }
        val branchCommitList = branchNames.map { branchName ->
            val branch = BranchIndex.getBranch(branchName)
                ?: return@map Pair(branchName, emptyList<String>())

            val branchHead = BranchIndex.getBranchHead(branch.generateKey())
            val commitList = getCommitListFrom(branchHead.generateKey().string)
            Pair(branchName, commitList)
        }

        val response = RemoteChannel(socket).request<BranchSyncStatusResponseMessage>(
            message = BranchSyncStatusQueryMessage(BranchSyncStatusQueryData(branchCommitList))
        )

        return if (response.payload.objects.isEmpty()) {
            BranchSyncStatus.ONLY_LOCAL
        } else {
            for ((branchName, status, _) in response.payload.objects) {
                if (branchName == BranchIndex.getCurrentBranch().name) {
                    return status
                }
            }

            return BranchSyncStatus.ONLY_REMOTE
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

    /**
     * Checks if the auto-push directive is enabled.
     */
    fun isAutoPushEnabled(): Boolean {
        return RemoteDirectivesConfig().loadAutopushDirective()
    }
}
