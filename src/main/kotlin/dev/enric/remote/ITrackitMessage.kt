package dev.enric.remote

import dev.enric.logger.Logger
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import java.net.Socket

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