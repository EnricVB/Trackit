package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.RemotePathConfig
import dev.enric.core.handler.remote.PushHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.RemoteDirectionNotFoundException
import dev.enric.exceptions.RemotePullRequestException
import dev.enric.logger.Logger
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus.*
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.StatusResponseMessage
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
        val socket = handler.connectToRemote()
        val currentBranch = BranchIndex.getCurrentBranch()

        checkRemoteBranchStatus(handler, socket, currentBranch)
        handler.pushBranchObjects(socket, currentBranch)

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

    private suspend fun checkRemoteBranchStatus(handler: PushHandler, socket: Socket, currentBranch: Branch) {
        when (handler.askForRemoteBranchStatus(socket)) {
            BEHIND, DIVERGED, ONLY_REMOTE -> throw RemotePullRequestException(
                "Push aborted: The remote branch is not up to date. Please pull and resolve conflicts first."
            )
            SYNCED -> throw RemotePullRequestException(
                "Push aborted: The remote branch is already up to date. No changes to push."
            )
            AHEAD, ONLY_LOCAL -> {
                validateAndPushMissingData(handler, socket, currentBranch)
            }
        }
    }

    private suspend fun validateAndPushMissingData(handler: PushHandler, socket: Socket, currentBranch: Branch) {
        val remoteBranchHead = RemoteChannel(socket).request<StatusResponseMessage>(
            StatusQueryMessage(currentBranch.name)
        ).payload

        val missingData = handler.askForMissingData(socket, currentBranch, Hash(remoteBranchHead)).map { (hash, byteContent) ->
            val hashType = Hash.HashType.fromHash(hash)
            hashType to byteContent
        }

        missingData.forEach {
            Logger.debug("Missing data: ${it.first} with hash ${it.second}")
        }

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