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
import dev.enric.remote.packet.message.PushObjectsMessage
import dev.enric.remote.packet.message.PushTagIndexMessage
import dev.enric.remote.packet.message.data.MissingObjectCheckData
import dev.enric.remote.packet.message.data.PushData
import dev.enric.remote.packet.query.MissingObjectCheckQueryMessage
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.MissingObjectCheckResponseMessage
import dev.enric.remote.packet.response.StatusResponseMessage
import dev.enric.util.common.Utility
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.net.Socket
import kotlin.io.path.readText

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
     *
     * @return A socket connected to the remote server.
     */
    fun connectToRemote(): Socket {
        val socket = StartTCPRemote().connection(
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
     * Pushes the given objects to the remote server.
     *
     * @param socket An open socket connected to the remote server.
     * @param branch The current branch being pushed.
     * @param objects A list of pairs containing the object type and its serialized byte representation.
     */
    suspend fun pushObjects(socket: Socket, branch: Branch, objects: List<Pair<Hash.HashType, ByteArray>>) {
        val data = PushData(
            objects = objects,
            branchHeadHash = BranchIndex.getBranchHead(branch.generateKey()).generateKey().toString(),
            branchHash = branch.generateKey().toString()
        )

        RemoteChannel(socket).send(PushObjectsMessage(data))
    }

    suspend fun pushTagIndex(socket: Socket) {
        val data = RepositoryFolderManager().getTagIndexPath().readText()

        RemoteChannel(socket).send(PushTagIndexMessage(data))
    }

    /**
     * Sends to the remote all missing objects for the current branch:
     * - Commits
     * - Trees
     * - Contents
     * - Tags
     * - Branch metadata
     *
     * This is done by first collecting the objects that need to be pushed,
     * and then sending them to the remote server.
     *
     * @param socket An open socket connected to the remote server.
     * @param branch The current branch being pushed.
     */
    suspend fun pushBranchObjects(socket: Socket, branch: Branch) {
        val objectsToPush = collectPushableObjects(socket, branch)

        val formatted = objectsToPush.map { (hash, bytes) ->
            Hash.HashType.fromHash(hash) to bytes
        }

        pushObjects(socket, branch, formatted)
    }

    /**
     * Pushes the given users to the remote server.
     * This includes serializing the user objects and their associated roles and permissions.
     *
     * @param users A list of users to push.
     * @param socket An open socket connected to the remote server.
     */
    suspend fun pushUsers(socket: Socket, users: List<User>) {
        val objectsToPush = obtainUserPushObjects(users.map { it.generateKey() })

        val formatted = objectsToPush.map { (hash, bytes) ->
            Hash.HashType.fromHash(hash) to bytes
        }

        pushObjects(socket, BranchIndex.getCurrentBranch(), formatted)
    }

    /**
     * Pushes the given tags to the remote server.
     * This includes serializing the tag objects.
     *
     * @param tags A list of tag hashes to push.
     * @param socket An open socket connected to the remote server.
     */
    suspend fun pushTags(socket: Socket, tags: List<Hash>) {
        val objectsToPush = obtainTagPushObjects(tags)

        val formatted = objectsToPush.map { (hash, bytes) ->
            Hash.HashType.fromHash(hash) to bytes
        }

        pushObjects(socket, BranchIndex.getCurrentBranch(), formatted)
        pushTagIndex(socket)
    }

    /**
     * Gathers the objects needed for push based on what's missing remotely.
     *
     * @param socket An open socket connected to the remote server.
     * @param branch The current branch being pushed.
     * @return A map from object [Hash] to its serialized byte representation.
     */
    private suspend fun collectPushableObjects(socket: Socket, branch: Branch): HashMap<Hash, ByteArray> {
        val remoteHead = fetchRemoteHeadCommit(socket)
        val missingCommits = obtainPosteriorCommits(remoteHead, branch)
        return obtainBranchPushObjects(missingCommits)
    }

    /**
     * Requests the latest commit hash of the current branch from the remote.
     *
     * @param socket An open socket connected to the remote server.
     * @return The hash of the latest commit on the remote branch.
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
     * Serializes user objects and their associated roles and permissions for push.
     *
     * @param users A list of users to serialize.
     * @return A map from user [Hash] to its serialized byte representation.
     */
    private fun obtainUserPushObjects(users: List<Hash>): HashMap<Hash, ByteArray> {
        val objectsToPush = HashMap<Hash, ByteArray>()

        for (userHash in users) {
            val user = User.newInstance(userHash)

            val userRoles = user.roles.map { Role.newInstance(it) }
            val userBranchPermissions = user.roles.flatMap { Role.newInstance(it).getBranchPermissions() }
            val userRolePermissions = user.roles.flatMap { Role.newInstance(it).getRolePermissions() }

            // Add to new data
            objectsToPush[user.generateKey()] = user.encode().second

            for (role in userRoles) {
                objectsToPush[role.generateKey()] = role.encode().second
            }

            for (permission in userBranchPermissions) {
                objectsToPush[permission.generateKey()] = permission.encode().second
            }

            for (permission in userRolePermissions) {
                objectsToPush[permission.generateKey()] = permission.encode().second
            }
        }


        return objectsToPush
    }

    /**
     * Serializes tag objects for push.
     *
     * @param tags A list of tag hashes to serialize.
     * @return A map from tag [Hash] to its serialized byte representation.
     */
    private fun obtainTagPushObjects(tags: List<Hash>): HashMap<Hash, ByteArray> {
        val objectsToPush = HashMap<Hash, ByteArray>()

        for (tagHash in tags) {
            val tagObj = when (Hash.HashType.fromHash(tagHash)) {
                SIMPLE_TAG -> SimpleTag.newInstance(tagHash)
                COMPLEX_TAG -> ComplexTag.newInstance(tagHash)
                else -> continue
            }
            objectsToPush[tagObj.generateKey()] = tagObj.encode().second
        }


        return objectsToPush
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

        val localCommits = Utility.getCommitListFrom(localBranchHead.string)
        val remoteCommits = remoteHead?.string?.let { Utility.getCommitListFrom(it) } ?: emptyList()

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

        RemoteChannel(socket).send(PushObjectsMessage(data))

    }

    /**
     * Checks if the auto-push directive is enabled.
     */
    fun isAutoPushEnabled(): Boolean {
        return RemoteDirectivesConfig().loadAutopushDirective()
    }
}
