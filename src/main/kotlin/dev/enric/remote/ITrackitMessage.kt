package dev.enric.remote

import dev.enric.remote.network.serialize.MessageFactory
import java.net.Socket


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
interface ITrackitMessage<T> {
    val id: MessageFactory.MessageType
    val payload: T

    /**
     * Encodes the object into a pair of a Hash and a ByteArray.
     * The Hash is the key that is going to be used to store the object in the repository.
     *
     * The ByteArray is the compressed object.
     * The data is serialized using ObjectOutputStream and written to a ByteArrayOutputStream.
     *
     * @return ByteArray with the serialized data.
     */
    fun encode(): ByteArray

    /**
     * Decodes the object from a ByteArray.
     * The ByteArray is the serialized object that is going to be deserialized using ObjectInputStream.
     *
     * The data is read from the ByteArrayInputStream and the size of the data is read first.
     * Then, for each element in the data, the HashType and the ByteArray are read and added to the result list.
     *
     * @param data ByteArray that is going to be deserialized.
     */
    fun decode(data: ByteArray): T

    /**
     * Executes the message.
     *
     * This method is used to execute the message and perform the action that is going to be done with the data.
     */
    suspend fun execute(socket: Socket)
}