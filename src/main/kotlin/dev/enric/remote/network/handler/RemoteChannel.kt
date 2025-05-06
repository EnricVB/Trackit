package dev.enric.remote.network.handler

import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType.ERROR
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class RemoteChannel(private val socket: Socket) {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    companion object {
        val pendingResponse = mutableMapOf<Int, CompletableDeferred<ITrackitMessage<*>>>()
        val lock = Any()

        fun handleIncomingMessage(message: ITrackitMessage<*>) {
            synchronized(lock) {
                pendingResponse.remove(message.id.ordinal)?.complete(message)
            }
        }
    }

    suspend fun send(message: ITrackitMessage<*>) {
        withContext(dispatcher) {
            val header = message.id.name
            val payload = message.encode()
            sendRawMessage(header, payload)
        }
    }

    suspend fun sendRawMessage(header: String, payload: ByteArray) {
        withContext(dispatcher) {
            val outputStream: OutputStream = socket.getOutputStream()
            val output = DataOutputStream(outputStream)

            // Calculate the total size of the message
            val headerBytes = header.toByteArray(Charsets.UTF_8)
            val headerLength = 2 + headerBytes.size
            val totalSize = headerLength + 4 + payload.size

            output.writeInt(totalSize) // Message length for RemoteConnection

            output.writeUTF(header) // Message type
            output.writeInt(payload.size) // Payload length
            output.write(payload) // Payload data

            output.flush()

            Logger.debug("Sent message: $header, payload size: ${payload.size}")
        }
    }

    suspend inline fun <reified T : ITrackitMessage<*>> request(message: ITrackitMessage<*>): T {
        val expectedID = T::class.java.getDeclaredConstructor().newInstance().id

        val deferred = CompletableDeferred<ITrackitMessage<*>>()
        synchronized(lock) {
            pendingResponse[expectedID.ordinal] = deferred
        }

        send(message)
        return deferred.await() as T
    }

    fun sendError(s: String) {
        try {
            val outputStream: OutputStream = socket.getOutputStream()

            val output = DataOutputStream(outputStream)
            output.writeInt(ERROR.ordinal)
            output.writeUTF(s)
            output.flush()
        } catch (e: IOException) {
            Logger.error("Failed to send error message: ${e.message}")
        }
    }
}
