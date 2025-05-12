package dev.enric.util.common

import dev.enric.core.handler.admin.RemotePathConfig
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.RemoteDirectionNotFoundException
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.packet.message.data.BranchSyncStatusQueryData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus
import dev.enric.remote.packet.query.BranchSyncStatusQueryMessage
import dev.enric.remote.packet.response.BranchSyncStatusResponseMessage
import dev.enric.util.index.BranchIndex
import java.net.Socket

object RemoteUtil {
    /**
     * Loads the remote push URL from the configuration file and validates it.
     *
     * @return The validated [DataProtocol] object representing the remote push URL.
     * @throws RemoteDirectionNotFoundException if the remote push URL is missing or invalid.
     */
    fun loadAndValidateRemotePushUrl(): DataProtocol {
        val (remotePush, _) = RemotePathConfig().load()

        return remotePush?.let { DataProtocol.validateRequest(it) }
            ?.let { DataProtocol.toDataProtocol(it) }
            ?: throw RemoteDirectionNotFoundException(
                "Remote push URL is missing or invalid. Set it using the 'trackit config' command."
            )
    }

    /**
     * Queries the remote server for the synchronization status of the current local branch.
     *
     * @param socket An open socket connected to the remote server.
     *
     * @return Triple with
     * [BranchSyncStatus] representing the state of synchronization:
     * - [BranchSyncStatus.SYNCED]: No differences between local and remote.
     * - [BranchSyncStatus.AHEAD]: Local branch has commits not present in remote.
     * - [BranchSyncStatus.BEHIND]: Remote branch has commits not present locally.
     * - [BranchSyncStatus.DIVERGED]: Both branches have diverged.
     * - [BranchSyncStatus.ONLY_LOCAL]: Local branch doesn't exist remotely.
     * - [BranchSyncStatus.ONLY_REMOTE]: Remote branch doesn't exist locally.
     * List of commits that are missing on the remote.
     * List of commits that are missing locally.
     */
    suspend fun askForRemoteBranchStatus(branchName : String, socket: Socket): Triple<BranchSyncStatus, List<String>, List<String>> {
        val localBranches = BranchIndex.getAllBranches()

        val commitMap = localBranches.associate { branchKey ->
            val branch = Branch.newInstance(branchKey)
            val head = BranchIndex.getBranchHead(branch.generateKey())
            val commits = Utility.getCommitListFrom(head.generateKey().string)
            branch.name to commits
        }

        val response = RemoteChannel(socket).request<BranchSyncStatusResponseMessage>(
            BranchSyncStatusQueryMessage(BranchSyncStatusQueryData(commitMap.toList()))
        )

        val statusEntry = response.payload.objects
            .firstOrNull { (name, _, _) -> name == branchName }

        return when {
            response.payload.objects.isEmpty() -> Triple(BranchSyncStatus.ONLY_LOCAL, emptyList(), emptyList())
            statusEntry != null -> {
                val (_, status, commits) = statusEntry

                Triple(
                    status,
                    commits.first,
                    commits.second
                )
            }
            else -> Triple(BranchSyncStatus.ONLY_REMOTE, emptyList(), emptyList())
        }
    }
}