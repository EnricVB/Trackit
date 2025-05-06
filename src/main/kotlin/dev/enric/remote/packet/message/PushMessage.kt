package dev.enric.remote.packet.message

import dev.enric.core.handler.repo.CheckoutHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.DeserializerHandler
import dev.enric.remote.network.serialize.MessageFactory
import dev.enric.remote.packet.message.data.PushData
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import java.net.Socket

class PushMessage(
    override var payload: PushData = PushData()
) : ITrackitMessage<PushData> {

    override val id: MessageFactory.MessageType
        get() = MessageFactory.MessageType.PUSH_MESSAGE

    override fun encode(): ByteArray {
        return payload.encode()
    }

    override fun decode(data: ByteArray): PushData {
        return PushData.decode(data).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        Logger.debug("Received PushMessage with ${payload.objects.size} objects.")
        val branchHash = Hash(payload.branchHash)
        val branchHeadHash = Hash(payload.branchHeadHash)

        payload.objects.forEach { (type, compressedBytes) ->
            val obj = DeserializerHandler.deserialize(type, compressedBytes)
            val (hash, _) = obj.encode(writeOnDisk = true)

            Logger.debug("Object with hash $hash and type $type has been sent to the repository.")
        }

        BranchIndex.setBranchHead(branchHash, branchHeadHash)
        Logger.debug("Branch head updated to $branchHeadHash for branch $branchHash.")

        CommitIndex.setCurrentCommit(branchHeadHash)
        Logger.debug("Current commit updated to $branchHeadHash.")

        // Reset hard to keep the remote and local branches in sync
        resetHard(branchHeadHash)
    }

    private fun resetHard(commitHash: Hash) {
        val checkoutHandler = CheckoutHandler(Commit.newInstance(commitHash))

        checkoutHandler.preCheckout()
        checkoutHandler.checkout()
    }
}