package dev.enric.remote

import dev.enric.logger.Logger
import dev.enric.remote.message.ITrackitMessage
import dev.enric.remote.remoteObject.MessageFactory.MessageType.ERROR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class SSHConnection(
    private val socket: Socket,
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val clientPublicKey: String?
) {

    fun isAuthenticated(): Boolean {
        return clientPublicKey != null // TODO("Implement actual authentication logic")
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

    fun getPublicKey(): String {
        return clientPublicKey ?: "Unknown"
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