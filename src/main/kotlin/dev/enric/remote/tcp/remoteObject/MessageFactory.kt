package dev.enric.remote.tcp.remoteObject

import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.MalformedDataException
import dev.enric.exceptions.RemoteConnectionException
import dev.enric.remote.tcp.remoteObject.MessageFactory.MessageType.*
import dev.enric.remote.tcp.message.ITrackitMessage
import dev.enric.remote.tcp.message.TrackitObjectSender

class MessageFactory {

    companion object {
        fun decode(data: ByteArray): ITrackitMessage<*> {
            val input = data.decodeToString()
            val splitIndex = input.indexOf(":")
            if (splitIndex == -1) throw MalformedDataException("Invalid message format: $input")

            val typeOrdinal = input.substring(0, splitIndex).toIntOrNull()
                ?: throw MalformedDataException("Invalid message type: $input")

            val type = MessageType.fromOrdinal(typeOrdinal)
            val payload = input.substring(splitIndex + 1).toByteArray()

            return when (type) {
                TRACKIT_OBJECT_SENDER -> TrackitObjectSender().apply { decode(payload) }
                ERROR -> throw RemoteConnectionException("Error message received: ${payload.decodeToString()}")
                else -> throw IllegalStateException("Unknown message type: $type")
            }
        }
    }

    enum class MessageType {
        TRACKIT_OBJECT_SENDER,
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