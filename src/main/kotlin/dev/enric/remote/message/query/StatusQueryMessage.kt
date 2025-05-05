package dev.enric.remote.message.query

import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.message.response.StatusResponseMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.util.index.BranchIndex
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

/**
 * A query message that requests the status of a remote repository.
 * Asks for the commit hash of the remote repository for the specified Branch.
 *
 * @param payload The payload of the message, which is the branch name of the commit to be queried.
 */
class StatusQueryMessage(
    override val payload: String = ""
) : ITrackitMessage<String> {

    override val id: MessageType
        get() = MessageType.STATUS_QUERY

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
        val branch = BranchIndex.getBranch(payload)
        if (branch == null) {
            Logger.error("Branch not found: $payload")
            RemoteChannel(socket).sendError("Branch not found: $payload")
            return
        }

        val branchHead = BranchIndex.getBranchHead(branch.generateKey())

        RemoteChannel(socket).send(StatusResponseMessage(branchHead.generateKey().string))
    }
}