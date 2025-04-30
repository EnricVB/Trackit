package dev.enric.remote.message

import dev.enric.domain.Hash.HashType
import dev.enric.domain.TrackitObject
import dev.enric.logger.Logger
import dev.enric.remote.DeserializerHandler

class TrackitObjectSender(
    private val data: List<TrackitObject<*>> = emptyList(),
) : ITrackitMessage {
    override val id: Int = 0

    override fun getData(): List<Pair<HashType, ByteArray>> {
        return data.map { trackitObject ->
            val (hash, compressed) = trackitObject.encode()
            val type = HashType.fromHash(hash)
            type to compressed
        }
    }

    override fun execute(receivedData : ByteArray) {
        val payloads: List<Pair<HashType, ByteArray>> = decode(receivedData)

        payloads.forEach { (type, compressedBytes) ->
            val obj = DeserializerHandler.deserialize(type, compressedBytes)
            val (hash, _) = obj.encode(writeOnDisk = true)

            Logger.debug("Object with hash $hash and type $type has been sent to the repository.")
        }
    }
}