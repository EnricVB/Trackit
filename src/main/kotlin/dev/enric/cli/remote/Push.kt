package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.remote.PushHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.User
import dev.enric.exceptions.*
import dev.enric.logger.Logger
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus.*
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.StatusResponseMessage
import dev.enric.util.common.RemoteUtil
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import dev.enric.util.index.UserIndex
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.net.Socket

@Command(
    name = "push",
    description = ["Push the current repository to a remote server"],
    mixinStandardHelpOptions = true,
)
class Push : TrackitCommand() {
    @Option(
        names = ["--branch"],
        description = ["Sends the specified branch to the remote server."],
        required = false
    )
    var branchName: String = BranchIndex.getCurrentBranch().name

    @Option(
        names = ["--all-branches"],
        description = ["Push all branches to the remote server"],
        required = false
    )
    var allBranches: Boolean = false

    @Option(
        names = ["--user"],
        description = ["Sends the specified user to the remote server."],
        required = false
    )
    var userName: String? = null

    @Option(
        names = ["--all-users"],
        description = ["Push all users to the remote server"],
        required = false
    )
    var allUsers: Boolean = false

    @Option(
        names = ["--tag"],
        description = ["Sends the specified tag to the remote server."],
    )
    var tagName: String? = null

    @Option(
        names = ["--all-tags"],
        description = ["Push all tags to the remote server"],
        required = false
    )
    var allTags: Boolean = false

    @Option(
        names = ["--all"],
        description = ["Push all objects to the remote server"],
        required = false
    )
    var all: Boolean = false

    override fun call(): Int = runBlocking {
        super.call()
        val remotePushUrl = RemoteUtil.loadAndValidateRemotePushUrl()
        val handler = PushHandler(remotePushUrl)
        val socket = handler.connectToRemote()
        val currentBranch = BranchIndex.getBranch(branchName) ?:
            throw BranchNotFoundException("Branch $branchName not found.")

        val sendTags = tagName != null || allTags
        val sendUsers = userName != null || allUsers

        when {
            sendUsers && sendTags -> {sendTags(handler, socket); sendUsers(handler, socket)}
            sendTags -> sendTags(handler, socket)
            sendUsers -> sendUsers(handler, socket)
            allBranches -> sendAllBranches(handler, socket)
            all -> { sendTags(handler, socket); sendUsers(handler, socket); sendAllBranches(handler, socket) }
            else -> sendBranchData(handler, socket, currentBranch)
        }

        Logger.info("Push completed successfully.")

        return@runBlocking 0
    }

    private suspend fun sendBranchData(handler: PushHandler, socket: Socket, currentBranch: Branch) {
        checkRemoteBranchStatus(handler, socket, currentBranch)
        handler.pushBranchObjects(socket, currentBranch)
    }

    private suspend fun sendAllBranches(handler: PushHandler, socket: Socket) {
        val branches = BranchIndex.getAllBranches()

        branches.forEach { branchHash ->
            sendBranchData(handler, socket, Branch.newInstance(branchHash))
        }
    }

    private suspend fun sendUsers(handler: PushHandler, socket: Socket) {
        val usersHash = userName?.let { listOf(UserIndex.getUser(it)?.generateKey()) } ?: UserIndex.getAllUsers()

        if (usersHash.isEmpty()) {
            throw UserNotFoundException("User $userName not found.")
        }

        handler.pushUsers(
            socket,
            usersHash.mapNotNull { userHash ->
                userHash?.let { hash -> User.newInstance(hash) }
            }
        )
    }

    private suspend fun sendTags(handler: PushHandler, socket: Socket) {
        val tagsHash = tagName?.let { TagIndex.getTagsByName(it) } ?: TagIndex.getAllTags()

        if (tagsHash.isEmpty()) {
            throw TagNotFoundException("Tag $tagName not found.")
        }

        handler.pushTags(socket, tagsHash)
    }

    private suspend fun checkRemoteBranchStatus(handler: PushHandler, socket: Socket, currentBranch: Branch) {
        when (RemoteUtil.askForRemoteBranchStatus(currentBranch.name, socket).first) {
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
        ).payload.takeIf { it != "null" }
        val remoteHeadHash = remoteBranchHead?.let { Hash(it) }

        val missingData = handler.askForMissingData(socket, currentBranch, remoteHeadHash).map { (hash, byteContent) ->
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