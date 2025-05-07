package dev.enric.remote

import dev.enric.logger.Logger
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.CoroutineExceptionHandler
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
    var payload: T

    val coroutineExceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, exception ->
            Logger.error("Caught exception in coroutine: ${exception.message}")
        }

    /**
     * Encodes the object to a ByteArray.
     * The ByteArray is the serialized object that is going to be sent using ObjectOutputStream.
     */
    fun encode(): ByteArray

    /**
     * Decodes the ByteArray to an object.
     * The ByteArray is the serialized object that is going to be received using ObjectInputStream.
     */
    fun decode(data: ByteArray): T

    /**
     * Executes the message.
     * This method is used to execute the message and perform the action that is going to be done with the data.
     */
    suspend fun execute(socket: Socket)
}