package dev.enric.remote.packet.query

import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.CommitNotFoundException
import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.remote.packet.message.data.BranchSyncStatusQueryData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus
import dev.enric.remote.packet.response.BranchSyncStatusResponseMessage
import dev.enric.util.index.BranchIndex
import java.net.Socket

/**
 * A query message for the status of a branch synchronization.
 * It does not contain any payload.
 *
 * It requests the status of the branch synchronization from the remote server.
 *
 * @param payload The payload of the message, which is empty for this query.
 */
class BranchSyncStatusQueryMessage(
    override var payload: BranchSyncStatusQueryData = BranchSyncStatusQueryData()
) : ITrackitMessage<BranchSyncStatusQueryData> {

    override val id: MessageType
        get() = MessageType.BRANCH_SYNC_STATUS_QUERY

    override fun encode(): ByteArray {
        return payload.encode()
    }

    override fun decode(data: ByteArray): BranchSyncStatusQueryData {
        return BranchSyncStatusQueryData.decode(data).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        Logger.debug("Executing BranchSyncStatusQuery with payload: $payload")

        val syncData = getBranchesSyncStatus(payload.objects)
        val response = BranchSyncStatusResponseMessage(BranchSyncStatusResponseData(syncData))

        RemoteChannel(socket).send(response)
    }

    private fun getBranchesSyncStatus(
        branchCommitList: List<Pair<String, List<String>>>
    ): List<Triple<String, BranchSyncStatus, Pair<List<String>, List<String>>>> {
        val localBranches = branchCommitList.map { it.first }
        val remoteBranches = BranchIndex.getAllBranches().map { Branch.newInstance(it).name }

        val result = mutableListOf<Triple<String, BranchSyncStatus, Pair<List<String>, List<String>>>>()

        // Filter remote branches to only include those that are not in local branches
        getRemoteBranchesOnly(remoteBranches, localBranches).forEach { branchName ->
            val branchSyncStatus = BranchSyncStatus.ONLY_REMOTE
            val missingInLocal = emptyList<String>()
            val missingInRemote = emptyList<String>()

            // Create a Triple with the branch name, status, and the pair of lists
            result.add(Triple(branchName, branchSyncStatus, Pair(missingInRemote, missingInLocal)))
            Logger.debug("Branch $branchName is only present in remote")
        }

        // Process the branches that are present in both remote and local
        branchCommitList.map { (branchName, localCommits) ->
            val branchSyncStatus = getBranchSyncStatus(branchName, localCommits)
            val triple = Triple(
                branchName,
                branchSyncStatus.first,
                Pair(branchSyncStatus.second.first, branchSyncStatus.second.second)
            )

            result.add(triple)
            Logger.debug("Branch $branchName has status ${branchSyncStatus.first}")
        }


        // Return the list of triples
        return result
    }

    /**
     * Get the branch synchronization status for a given branch.
     *
     * @param branchName The name of the branch sent from the client.
     * @param localCommits The list of local commits for the branch.
     *
     * @return A pair containing the branch synchronization status and a pair of lists:
     */
    private fun getBranchSyncStatus(
        branchName: String,
        localCommits: List<String>
    ): Pair<BranchSyncStatus, Pair<List<String>, List<String>>> {
        val branch = BranchIndex.getBranch(branchName) ?: return Pair(
            BranchSyncStatus.ONLY_LOCAL,
            Pair(emptyList(), emptyList())
        )

        val localHead = Hash(localCommits.first())
        val remoteHead: Hash? = try {
            BranchIndex.getBranchHead(branch.generateKey()).generateKey()
        } catch (e: CommitNotFoundException) {
            Logger.error("Error getting local hash for branch $branchName: ${e.message}")
            null
        }

        return when (remoteHead) {
            localHead -> Pair(BranchSyncStatus.SYNCED, Pair(emptyList(), emptyList()))
            null -> Pair(BranchSyncStatus.ONLY_LOCAL, Pair(emptyList(), emptyList()))
            else -> {
                val remoteHashCommitStr = remoteHead.string
                val remoteCommits = getCommitListFrom(remoteHashCommitStr).map { it.string }

                val missingInLocal = remoteCommits.filterNot { localCommits.contains(it) }
                val missingInRemote = localCommits.filterNot { remoteCommits.contains(it) }

                val status = when {
                    missingInLocal.isEmpty() && missingInRemote.isEmpty() -> BranchSyncStatus.SYNCED
                    missingInLocal.isEmpty() -> BranchSyncStatus.AHEAD
                    missingInRemote.isEmpty() -> BranchSyncStatus.BEHIND
                    else -> BranchSyncStatus.DIVERGED
                }

                Pair(status, Pair(missingInRemote, missingInLocal))
            }
        }
    }

    /**
     * Get the list of commits from a given hash.
     */
    fun getCommitListFrom(startHash: String): List<Hash> {
        val commitList = mutableListOf<Hash>()
        var currentHash: Hash? = Hash(startHash)

        while (currentHash != null) {
            commitList.add(currentHash)

            val commit = Commit.newInstance(currentHash)
            currentHash = commit.previousCommit
        }

        return commitList
    }

    /**
     * Get the remote branches that are not present in the local branches.
     */
    private fun getRemoteBranchesOnly(remoteBranches: List<String>, localBranches: List<String>): List<String> {
        return remoteBranches.filterNot { localBranches.contains(it) }
    }
}