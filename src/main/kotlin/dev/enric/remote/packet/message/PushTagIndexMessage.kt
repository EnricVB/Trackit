package dev.enric.remote.packet.message

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.util.repository.RepositoryFolderManager
import java.net.Socket
import java.nio.charset.Charset
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

/**
 * A query message that requests the status of a remote repository.
 * Asks for the commit head hash of the remote repository for the specified Branch.
 *
 * @param payload The payload of the message, which is the branch name of the commit to be queried.
 */
class PushTagIndexMessage(
    override var payload: String = ""
) : ITrackitMessage<String> {

    override val id: MessageType
        get() = MessageType.PUSH_INDEX_MESSAGE

    override fun encode(): ByteArray {
        return payload.toByteArray(Charset.defaultCharset())
    }

    override fun decode(data: ByteArray): String {
        return data.toString(Charset.defaultCharset()).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        RepositoryFolderManager().getTagIndexPath().writeText(
            payload,
            Charset.defaultCharset(),
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE
        )
    }
}