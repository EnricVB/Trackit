package dev.enric.remote.packet.query

import dev.enric.domain.Hash
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.remote.network.serialize.DeserializerHandler
import dev.enric.remote.network.serialize.MessageFactory.MessageType
import dev.enric.remote.packet.message.data.MissingObjectCheckData
import dev.enric.remote.packet.response.MissingObjectCheckResponseMessage
import dev.enric.util.repository.RepositoryFolderManager
import java.net.Socket
import kotlin.io.path.exists

class MissingObjectCheckQueryMessage(
    override var payload: MissingObjectCheckData = MissingObjectCheckData()
) : ITrackitMessage<MissingObjectCheckData> {

    override val id: MessageType
        get() = MessageType.MISSING_BRANCH_DATA_QUERY

    override fun encode(): ByteArray {
        return payload.encode()
    }

    override fun decode(data: ByteArray): MissingObjectCheckData {
        return MissingObjectCheckData.decode(data).also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        val missingObjects = payload.objects.map { (hashStr, byteArray) ->
            val hashType = Hash.HashType.fromHash(Hash(hashStr))

            DeserializerHandler.deserialize(hashType, byteArray)
        }.filterNot { isObjectStoredLocally(it.generateKey()) }
        val messageData = MissingObjectCheckData(
            objects = missingObjects.map { it.generateKey().string to it.encode().second }
        )

        RemoteChannel(socket).send(MissingObjectCheckResponseMessage(messageData))
    }

    private fun isObjectStoredLocally(hash: Hash): Boolean {
        val repositoryFolderManager = RepositoryFolderManager()
        val hashType = Hash.HashType.fromHash(hash)
        val objectFolder = repositoryFolderManager.getObjectsFolderPath().resolve(hashType.hash.string)

        return objectFolder.resolve(hash.string).exists()
    }
}