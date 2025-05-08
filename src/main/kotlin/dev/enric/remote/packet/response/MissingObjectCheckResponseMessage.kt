package dev.enric.remote.packet.response

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.remote.packet.message.data.MissingObjectCheckData
import java.net.Socket

class MissingObjectCheckResponseMessage(
    override var payload: MissingObjectCheckData = MissingObjectCheckData()
) : ITrackitMessage<MissingObjectCheckData> {

    override val id: MessageType
        get() = MessageType.MISSING_BRANCH_DATA_RESPONSE

    override fun encode(): ByteArray {
        return payload.encode()
    }

    override fun decode(data: ByteArray): MissingObjectCheckData {
        return MissingObjectCheckData.decode(data).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        // Do nothing. This is a response message.
    }
}