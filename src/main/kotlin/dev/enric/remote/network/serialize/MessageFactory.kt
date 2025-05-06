package dev.enric.remote.network.serialize

import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.RemoteConnectionException
import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.message.SendObjectsMessage
import dev.enric.remote.message.query.StatusQueryMessage
import dev.enric.remote.message.response.StatusResponseMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType.*
import java.io.DataInputStream

class MessageFactory {

    companion object {
        fun decode(data: ByteArray): ITrackitMessage<*> {
            Logger.debug("Received message raw message")

            val input = DataInputStream(data.inputStream())
            val type = MessageType.valueOf(input.readUTF())
            val payload = ByteArray(input.readInt())
            input.readFully(payload)

            Logger.debug("Decoding message type: $type, payload: ${payload.decodeToString()}")

            return when (type) {
                TRACKIT_OBJECT_SENDER -> SendObjectsMessage().apply { decode(payload) }
                STATUS_QUERY -> StatusQueryMessage().apply { decode(payload) }
                STATUS_RESPONSE -> StatusResponseMessage().apply { decode(payload) }
                ERROR -> throw RemoteConnectionException("Error message received: ${payload.decodeToString()}")
                else -> throw IllegalStateException("Unknown message type: $type")
            }
        }
    }

    enum class MessageType {
        TRACKIT_OBJECT_SENDER,
        STATUS_QUERY, STATUS_RESPONSE,
        ERROR,
        UKNOWN;

        companion object {
            fun fromOrdinal(readInt: Int): MessageType {
                return entries.firstOrNull { it.ordinal == readInt }
                    ?: UKNOWN
            }
        }
    }
}