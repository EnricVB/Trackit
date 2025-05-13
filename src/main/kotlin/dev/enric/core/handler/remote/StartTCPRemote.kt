package dev.enric.core.handler.remote

import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.MalformedDataException
import dev.enric.exceptions.RemoteConnectionException
import dev.enric.logger.Logger
import java.io.PrintWriter
import java.net.Socket

class StartTCPRemote {

    // TODO: Dont use the clean password, use TLS
    fun connection(username: String, password: String, host: String, port: Int, path: String): Socket {
        val protocol = DataProtocol.newTrackitInstance(
            user = username, password = password, host = host, port = port, path = path
        )

        val socket = Socket(host, 8088) // TODO: Make this configurable
        val responseStr = sendConnectionPetition(socket, protocol)
        val (_, _, _, _, redirectPort, _) = DataProtocol.validateRequest(responseStr)?.destructured
            ?: throw MalformedDataException("Invalid response format")

        return retryConnection(host, redirectPort.toInt(), 40, 100)
    }

    private fun sendConnectionPetition(
        socket: Socket, protocol: DataProtocol
    ): String {
        val writer = PrintWriter(socket.getOutputStream(), true)
        val reader = socket.getInputStream().bufferedReader()

        writer.println(protocol.toString())
        return reader.readLine()
    }

    fun retryConnection(host: String, port: Int, retries: Int = 10, delayMillis: Long = 200): Socket {
        repeat(retries) { attempt ->
            try {
                val socket = Socket(host, port)
                socket.soTimeout = 10000

                return socket
            } catch (e: Exception) {
                Logger.debug("Connection attempt $attempt to $host:$port failed: ${e.message}")
                if (attempt == retries - 1) throw RemoteConnectionException("Failed to connect to $host:$port after $retries attempts. Check if the host, port and path are correct.")
                Thread.sleep(delayMillis)
            }
        }

        throw RemoteConnectionException("Failed to connect to $host:$port after $retries attempts")
    }
}