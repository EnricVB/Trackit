package dev.enric.remote.packet.message.data

import dev.enric.domain.Hash.HashType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class PushData(
    val objects: List<Pair<HashType, ByteArray>> = emptyList(),
    val branchHeadHash: String = "",
    val branchHash: String = "",
) {

    fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the branch head and branch name
        objectOutputStream.writeUTF(branchHeadHash)
        objectOutputStream.writeUTF(branchHash)

        // Write the size of the payload
        objectOutputStream.writeInt(objects.size)

        // Write each pair to the output stream
        for ((type, bytes) in objects) {
            // Write object type, size and ByteArray
            objectOutputStream.writeUTF(type.name)
            objectOutputStream.writeInt(bytes.size)
            objectOutputStream.write(bytes)
        }

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        fun decode(data: ByteArray): PushData {
            val byteArrayInputStream = ByteArrayInputStream(data)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            // Read the branch head and branch name
            val branchHeadHash = objectInputStream.readUTF()
            val branchHash = objectInputStream.readUTF()

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

            return PushData(
                objects = result,
                branchHeadHash = branchHeadHash,
                branchHash = branchHash
            )
        }
    }
}