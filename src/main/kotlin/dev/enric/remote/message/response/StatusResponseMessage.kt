package dev.enric.remote.message.response

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

/**
 * A response message for the status query.
 * It contains the commit hash of the remote repository for the specified branch.
 *
 * @param payload The payload of the message, which is the commit hash of the branch.
 */
class StatusResponseMessage(
    override val payload: String = ""
) : ITrackitMessage<String> {

    override val id: MessageType
        get() = MessageType.STATUS_RESPONSE

    override fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the payload to the output stream
        objectOutputStream.writeUTF(payload)

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    override fun decode(data: ByteArray): String {
        val byteArrayInputStream = data.inputStream()
        val objectInputStream = ObjectInputStream(byteArrayInputStream)

        // Read the payload from the input stream
        return objectInputStream.readUTF()
    }

    override suspend fun execute(socket: Socket) {
    }
}