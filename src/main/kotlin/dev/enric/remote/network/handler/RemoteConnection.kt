package dev.enric.remote.network.handler

import dev.enric.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class RemoteConnection(val socket: Socket) {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun isAuthenticated(): Boolean {
        return true // TODO: Implement authentication check
    }

    suspend fun receiveMessage(): ByteArray? {
        return withContext(dispatcher) {
            val inputStream = socket.getInputStream()
            val input = DataInputStream(inputStream)

            try {
                val length = input.readInt()
                val data = ByteArray(length)
                input.readFully(data)

                return@withContext data
            } catch (e: IOException) {
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