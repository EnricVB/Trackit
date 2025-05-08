package dev.enric.remote.network.serialize

import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType.*
import dev.enric.remote.packet.message.ErrorMessage
import dev.enric.remote.packet.message.PushMessage
import dev.enric.remote.packet.query.BranchSyncStatusQueryMessage
import dev.enric.remote.packet.query.MissingObjectCheckQueryMessage
import dev.enric.remote.packet.query.StatusQueryMessage
import dev.enric.remote.packet.response.BranchSyncStatusResponseMessage
import dev.enric.remote.packet.response.MissingObjectCheckResponseMessage
import dev.enric.remote.packet.response.StatusResponseMessage
import java.io.DataInputStream

class MessageFactory {

    companion object {
        fun decode(data: ByteArray): ITrackitMessage<*> {
            Logger.debug("Received message raw message ${data.decodeToString()}")

            val input = DataInputStream(data.inputStream())
            val type = MessageType.valueOf(input.readUTF())
            val payload = ByteArray(input.readInt())
            input.readFully(payload)

            Logger.debug("Decoding message type: $type, payload: ${payload.decodeToString()}")

            return when (type) {
                PUSH_MESSAGE -> PushMessage().apply { decode(payload) }
                STATUS_QUERY -> StatusQueryMessage().apply { decode(payload) }
                STATUS_RESPONSE -> StatusResponseMessage().apply { decode(payload) }
                MISSING_BRANCH_DATA_QUERY -> MissingObjectCheckQueryMessage().apply { decode(payload) }
                MISSING_BRANCH_DATA_RESPONSE -> MissingObjectCheckResponseMessage().apply { decode(payload) }
                BRANCH_SYNC_STATUS_QUERY -> BranchSyncStatusQueryMessage().apply { decode(payload) }
                BRANCH_SYNC_STATUS_RESPONSE -> BranchSyncStatusResponseMessage().apply { decode(payload) }
                ERROR -> ErrorMessage().apply { decode(payload) }
                else -> throw IllegalStateException("Unknown message type: $type")
            }
        }
    }

    enum class MessageType {
        PUSH_MESSAGE,
        STATUS_QUERY, STATUS_RESPONSE,
        MISSING_BRANCH_DATA_QUERY, MISSING_BRANCH_DATA_RESPONSE,
        BRANCH_SYNC_STATUS_QUERY, BRANCH_SYNC_STATUS_RESPONSE,
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