package dev.enric.remote.message.response

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import java.net.Socket
import java.nio.charset.Charset

/**
 * A response message for the status query.
 * It contains the commit hash of the remote repository for the specified branch.
 *
 * @param payload The payload of the message, which is the commit hash of the branch.
 */
class StatusResponseMessage(
    override var payload: String = ""
) : ITrackitMessage<String> {

    override val id: MessageType
        get() = MessageType.STATUS_RESPONSE

    override fun encode(): ByteArray {
        return payload.toByteArray(Charset.defaultCharset())
    }

    override fun decode(data: ByteArray): String {
        return String(data, Charset.defaultCharset()).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        // No action needed for response message
    }
}