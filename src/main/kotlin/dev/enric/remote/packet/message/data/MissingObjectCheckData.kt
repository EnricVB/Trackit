package dev.enric.remote.packet.message.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * This class represents the data structure that will be sent to Remote Server to ask if the sent objects are missing.
 * Contains a list of objects with:
 * - String -> Object Hash.
 * - ByteArray -> Object Content.
 */
data class MissingObjectCheckData(
    val objects: List<Pair<String, ByteArray>> = emptyList(),
) {

    fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the size of the payload
        objectOutputStream.writeInt(objects.size)

        // Write each object to the output stream
        for ((objectHash, objectContent) in objects) {
            objectOutputStream.writeUTF(objectHash)
            objectOutputStream.writeInt(objectContent.size)
            objectOutputStream.write(objectContent)
        }

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        fun decode(data: ByteArray): MissingObjectCheckData {
            val byteArrayInputStream = ByteArrayInputStream(data)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            // Read the size of the payload
            val size = objectInputStream.readInt()
            val result = mutableListOf<Pair<String, ByteArray>>()

            // Read each object from the input stream
            for (i in 0 until size) {
                val objectHash = objectInputStream.readUTF()
                val objectContentLength = objectInputStream.readInt()
                val objectContent = ByteArray(objectContentLength)
                objectInputStream.readFully(objectContent)

                result.add(Pair(objectHash, objectContent))
            }

            // Return the decoded object
            return MissingObjectCheckData(result)
        }
    }
}
