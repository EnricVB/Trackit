package dev.enric.remote.packet.message

import dev.enric.exceptions.RemoteConnectionException
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

class ErrorMessage(
    override var payload: String = "Something went wrong"
) : ITrackitMessage<String> {

    override val id: MessageFactory.MessageType
        get() = MessageFactory.MessageType.ERROR

    override fun encode(): ByteArray {
        return payload.encodeToByteArray()
    }

    override fun decode(data: ByteArray): String {
        return data.decodeToString().also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        withContext(Dispatchers.IO) {
            socket.close()
        }

        throw RemoteConnectionException(payload)
    }
}