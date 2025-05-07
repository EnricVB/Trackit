package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.RemotePathConfig
import dev.enric.core.handler.remote.PushHandler
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.RemoteDirectionNotFoundException
import dev.enric.exceptions.RemotePullRequestException
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus.BEHIND
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus.DIVERGED
import dev.enric.util.index.BranchIndex
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import java.net.Socket

@Command(
    name = "push",
    description = ["Push the current repository to a remote server"],
    mixinStandardHelpOptions = false,
)
class Push : TrackitCommand() {

    override fun call(): Int = runBlocking {
        super.call()
        val remotePushUrl = loadAndValidateRemotePushUrl()
        val handler = PushHandler(remotePushUrl)
        val socket = handler.startRemoteConnection()

        checkRemoteBranchStatus(handler, socket)
        validateAndPushMissingData(handler, socket)

        val currentBranch = BranchIndex.getCurrentBranch()
        handler.sendObjectsToRemote(socket, currentBranch)

        return@runBlocking 0
    }

    private fun loadAndValidateRemotePushUrl(): DataProtocol {
        val (remotePush, _) = RemotePathConfig().load()

        return remotePush?.let { DataProtocol.validateRequest(it) }
            ?.let { DataProtocol.toDataProtocol(it) }
            ?: throw RemoteDirectionNotFoundException(
                "Remote push URL is missing or invalid. Set it using the 'trackit config' command."
            )
    }

    private suspend fun checkRemoteBranchStatus(handler: PushHandler, socket: Socket) {
        when (handler.askForRemoteBranchStatus(socket)) {
            BEHIND, DIVERGED -> throw RemotePullRequestException(
                "Push aborted: The remote branch is not up to date. Please pull and resolve conflicts first."
            )
            else -> Unit
        }
    }

    private suspend fun validateAndPushMissingData(handler: PushHandler, socket: Socket) {
        val missingData = handler.askForMissingData(socket)
        if (missingData.isNotEmpty()) {
            if (!handler.isAutoPushEnabled()) {
                throw RemotePullRequestException(
                    "Push aborted: Missing data on remote. Activate autopush directive or use '-all'."
                )
            }

            handler.sendMissingData(socket, missingData)
        }
    }
}