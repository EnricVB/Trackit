package dev.enric.remote.network.serialize

import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.MalformedDataException
import dev.enric.exceptions.RemoteConnectionException
import dev.enric.logger.Logger
import dev.enric.remote.network.serialize.MessageFactory.MessageType.*
import dev.enric.remote.message.TrackitObjectSender
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.message.query.StatusQueryMessage
import dev.enric.remote.message.response.StatusResponseMessage

class MessageFactory {

    companion object {
        fun decode(data: ByteArray): ITrackitMessage<*> {
            Logger.debug("Received raw message: ${data.decodeToString()}")

            val input = data.decodeToString()
            val splitIndex = input.indexOf(":")
            if (splitIndex == -1) throw MalformedDataException("Invalid message format: $input")

            val typeOrdinal = input.substring(0, splitIndex).toIntOrNull()
                ?: throw MalformedDataException("Invalid message type: $input")

            val type = MessageType.fromOrdinal(typeOrdinal)
            val payload = input.substring(splitIndex + 1).toByteArray()

            Logger.debug("Decoded message type: $type, payload: ${payload.decodeToString()}")

            return when (type) {
                TRACKIT_OBJECT_SENDER -> TrackitObjectSender().apply { decode(payload) }
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