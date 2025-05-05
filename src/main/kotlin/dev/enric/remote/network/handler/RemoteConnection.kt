package dev.enric.remote.network.handler

import dev.enric.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class RemoteConnection(val socket: Socket) {

    fun isAuthenticated(): Boolean {
        return true
    }

    suspend fun receiveMessage(): ByteArray? {
        return withContext(Dispatchers.IO) {
            val inputStream = socket.getInputStream()

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

    fun isOpen(): Boolean {
        return !socket.isClosed && socket.isConnected
    }

    fun close() {
        try {
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            inputStream.close()
            outputStream.close()
            socket.close()
        } catch (e: IOException) {
            Logger.error("Failed to close SSH connection: ${e.message}")
        }
    }
}