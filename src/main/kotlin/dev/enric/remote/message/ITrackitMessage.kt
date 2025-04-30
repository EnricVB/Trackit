package dev.enric.remote.message

import dev.enric.domain.Hash.HashType
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


/*TODO:
    FUNCIONAMIENTO TCP
        Enviar mensaje tal cual con el id y el payload

    FUNCIONAMIENTO SSH
        Enviar petición 'ssh user@host trackit-server $id > payload.bin'
        Con el id y el payload

        El servidor ejecutará trackit-serve y obtendrá el ByteArray en el payload.bin
        Así que el servidor no tiene que hacer nada, solo ejecutar el comando y obtener el payload.bin

    FUNCIONAMIENTO HTTP
        Enviar petición 'http://host:port/trackit-server/$id'
        Con el id y el payload

        El servidor ejecutará trackit-serve y obtendrá el ByteArray en el payload.bin
        Así que el servidor no tiene que hacer nada, solo ejecutar el comando y obtener el payload.bin
 */
interface ITrackitMessage {
    val id: Int
    fun getData(): List<Pair<HashType, ByteArray>>

    /**
     * Encodes the object into a pair of a Hash and a ByteArray.
     * The Hash is the key that is going to be used to store the object in the repository.
     *
     * The ByteArray is the compressed object.
     * The data is serialized using ObjectOutputStream and written to a ByteArrayOutputStream.
     *
     * @return ByteArray with the serialized data.
     */
    fun encode(): ByteArray {
        val data = getData()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        objectOutputStream.writeInt(data.size)
        for (pair in data) {
            objectOutputStream.writeObject(pair.first)
            objectOutputStream.write(pair.second)
        }

        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Decodes the object from a ByteArray.
     * The ByteArray is the serialized object that is going to be deserialized using ObjectInputStream.
     *
     * The data is read from the ByteArrayInputStream and the size of the data is read first.
     * Then, for each element in the data, the HashType and the ByteArray are read and added to the result list.
     *
     * @param data ByteArray that is going to be deserialized.
     */
    fun decode(data: ByteArray): List<Pair<HashType, ByteArray>> {
        val byteArrayInputStream = data.inputStream()
        val objectInputStream = ObjectInputStream(byteArrayInputStream)

        val size = objectInputStream.readInt()
        val result = mutableListOf<Pair<HashType, ByteArray>>()

        for (i in 0 until size) {
            val hashType = objectInputStream.readObject() as HashType
            val byteArray = ByteArray(objectInputStream.available())
            objectInputStream.read(byteArray)

            result.add(Pair(hashType, byteArray))
        }

        return result
    }

    /**
     * Executes the message.
     * This method is used to execute the message and perform the action that is going to be done with the data.
     */
    fun execute(receivedData : ByteArray) {
        // Implementation of the execute method
    }
}