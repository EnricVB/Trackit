package dev.enric.remote.message

import dev.enric.domain.Hash.HashType
import dev.enric.logger.Logger
import dev.enric.remote.network.serialize.DeserializerHandler
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class SendObjectsMessage(
    override var payload: List<Pair<HashType, ByteArray>> = emptyList()
) : ITrackitMessage<List<Pair<HashType, ByteArray>>> {

    override val id: MessageFactory.MessageType
        get() = MessageFactory.MessageType.TRACKIT_OBJECT_SENDER

    override fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the size of the payload
        objectOutputStream.writeInt(payload.size)

        // Write each pair to the output stream
        for ((type, bytes) in payload) {
            // Write object type, size and ByteArray
            objectOutputStream.writeUTF(type.name)
            objectOutputStream.writeInt(bytes.size)
            objectOutputStream.write(bytes)
        }

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    override fun decode(data: ByteArray): List<Pair<HashType, ByteArray>> {
        val byteArrayInputStream = ByteArrayInputStream(data)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)

        // Read the size of the payload
        val result = mutableListOf<Pair<HashType, ByteArray>>()
        val size = objectInputStream.readInt()

        // Read each pair from the input stream
        for (i in 0 until size) {
            val hashType = HashType.valueOf(objectInputStream.readUTF())
            val byteArrayLength = objectInputStream.readInt()
            val byteArray = ByteArray(byteArrayLength)
            objectInputStream.readFully(byteArray)

            result.add(Pair(hashType, byteArray))
        }

        return result.also { payload = it }
    }

    override suspend fun execute(socket: Socket) {
        Logger.debug("Received SendObjectsMessage with ${payload.size} objects.")

        payload.forEach { (type, compressedBytes) ->
            val obj = DeserializerHandler.deserialize(type, compressedBytes)
            val (hash, _) = obj.encode(writeOnDisk = true)

            Logger.debug("Object with hash $hash and type $type has been sent to the repository.")
        }
    }
}