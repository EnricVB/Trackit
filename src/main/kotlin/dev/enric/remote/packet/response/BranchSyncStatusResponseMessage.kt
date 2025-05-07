package dev.enric.remote.packet.response

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.remote.packet.message.data.BranchSyncStatusQueryData
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData
import dev.enric.remote.packet.query.BranchSyncStatusQueryMessage
import java.net.Socket

/**
 * This class represents a response message for the status of a branch synchronization.
 * It contains a payload of type [BranchSyncStatusQueryData].
 *
 * The message is used to respond to a [BranchSyncStatusQueryMessage].
 *
 * @property payload The payload of the message, which contains the status data.
 */
class BranchSyncStatusResponseMessage(
    override var payload: BranchSyncStatusResponseData = BranchSyncStatusResponseData()
) : ITrackitMessage<BranchSyncStatusResponseData> {

    override val id: MessageType
        get() = MessageType.BRANCH_SYNC_STATUS_RESPONSE

    override fun encode(): ByteArray {
        return payload.encode()
    }

    override fun decode(data: ByteArray): BranchSyncStatusResponseData {
        return BranchSyncStatusResponseData.decode(data).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        // No action needed for response message
    }
}