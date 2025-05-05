package dev.enric.remote.tcp

import dev.enric.logger.Logger
import dev.enric.remote.tcp.message.ITrackitMessage
import dev.enric.remote.tcp.remoteObject.MessageFactory.MessageType.ERROR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class RemoteConnection(
    private val socket: Socket,
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
) {

    fun isAuthenticated(): Boolean {
        return true
    }

    suspend fun receiveMessage(): ByteArray? {
        return withContext(Dispatchers.IO) {

            val input = DataInputStream(inputStream)

            try {
                val length = input.readInt()
                val data = ByteArray(length)
                input.readFully(data)

                return@withContext data
            } catch (e: IOException) {
                Logger.error("Error receiving message: ${e.message}")
                return@withContext null
            }
        }
    }

    suspend fun sendMessage(message: ITrackitMessage<*>) {
        withContext(Dispatchers.IO) {
            val payload = message.encode()
            val header = "${message.id.ordinal}:".toByteArray()
            val fullMessage = header + payload
            sendRawMessage(fullMessage)
        }
    }

    private suspend fun sendRawMessage(data: ByteArray) {
        withContext(Dispatchers.IO) {
            val output = DataOutputStream(outputStream)
            output.writeInt(data.size)
            output.write(data)
            output.flush()
        }
    }

    fun sendError(error: String) {
        try {
            val output = DataOutputStream(outputStream)
            output.writeInt(ERROR.ordinal)
            output.writeUTF(error)
            output.flush()
        } catch (e: IOException) {
            Logger.error("Failed to send error message: ${e.message}")
        }
    }

    fun isOpen(): Boolean {
        return !socket.isClosed && socket.isConnected
    }

    fun close() {
        try {
            inputStream.close()
            outputStream.close()
            socket.close()
        } catch (e: IOException) {
            Logger.error("Failed to close SSH connection: ${e.message}")
        }
    }
}