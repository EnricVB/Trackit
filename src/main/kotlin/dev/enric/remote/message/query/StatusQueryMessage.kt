package dev.enric.remote.message.query

import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.message.response.StatusResponseMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.util.index.BranchIndex
import java.net.Socket
import java.nio.charset.Charset

/**
 * A query message that requests the status of a remote repository.
 * Asks for the commit hash of the remote repository for the specified Branch.
 *
 * @param payload The payload of the message, which is the branch name of the commit to be queried.
 */
class StatusQueryMessage(
    override var payload: String = ""
) : ITrackitMessage<String> {

    override val id: MessageType
        get() = MessageType.STATUS_QUERY

    override fun encode(): ByteArray {
        return payload.toByteArray(Charset.defaultCharset())
    }

    override fun decode(data: ByteArray): String {
        return data.toString(Charset.defaultCharset()).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        Logger.debug("Executing StatusQueryMessage with payload: $payload")

        val branch = BranchIndex.getBranch(payload)
        if (branch == null) {
            Logger.error("Branch not found: $payload")
            RemoteChannel(socket).sendError("Branch not found: $payload")
            return
        }

        RemoteChannel(socket).send(StatusResponseMessage("null"))

        /*
        try {
            val branchHead = BranchIndex.getBranchHead(branch.generateKey())

            RemoteChannel(socket).send(StatusResponseMessage(branchHead.generateKey().string))
        } catch (ex: CommitNotFoundException) {
            RemoteChannel(socket).send(StatusResponseMessage("null"))
            Logger.error("Branch head not found: ${ex.message}")
        }*/
    }
}