package dev.enric.core.handler.remote

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.remote.message.SendObjectsMessage
import dev.enric.remote.message.query.StatusQueryMessage
import dev.enric.remote.message.response.StatusResponseMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.handler.RemoteClientListener
import dev.enric.remote.network.handler.RemoteConnection
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import java.net.Socket

/**
 * Handles the logic behind the `push` command.
 * This includes establishing a remote connection, querying the remote status,
 * determining which commits are missing on the remote, and collecting the set of objects
 * (commits, trees, contents, tags, and branches) that need to be pushed.
 */
class PushHandler : CommandHandler() {

    /**
     * Starts a remote SSH connection to the given host and folder path,
     * and launches a listener for incoming messages from the remote.
     *
     * @return A connected [Socket] to the remote server.
     */
    fun startRemoteConnection(): Socket {
        val socket = StartSSHRemote().connection(
            username = "test",
            password = "test",
            host = "localhost",
            port = 8088,
            path = "C:\\Users\\enric.velasco\\Desktop\\trackit\\tktFolder"
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

        RemoteChannel(socket).send(SendObjectsMessage(formattedObjects))
    }

    /**
     * Asks the remote server for the latest commit hash of the current branch.
     *
     * @param socket The open socket connected to the remote server.
     * @return The [Hash] of the latest commit on the remote side.
     */
    private suspend fun askForCommitHash(socket: Socket): Hash {
        return Hash(RemoteChannel(socket).request<StatusResponseMessage>(
            message = StatusQueryMessage(BranchIndex.getCurrentBranch().name)
        ).payload)
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

        Logger.info("${currentCommit?.generateKey()} $remoteCommitHash")

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
}
