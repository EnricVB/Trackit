package dev.enric.remote.network.handler

import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory.MessageType.ERROR
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class RemoteChannel(private val socket: Socket) {

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
        withContext(Dispatchers.IO) {
            val payload = message.encode()
            val header = "${message.id.ordinal}:".toByteArray()
            val fullMessage = header + payload
            sendRawMessage(fullMessage)
        }
    }

    suspend fun sendRawMessage(data: ByteArray) {
        withContext(Dispatchers.IO) {
            val outputStream: OutputStream = socket.getOutputStream()

            val output = DataOutputStream(outputStream)
            output.writeInt(data.size)
            output.write(data)
            output.flush()

            Logger.debug("Sent message: ${data.decodeToString()}")
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
