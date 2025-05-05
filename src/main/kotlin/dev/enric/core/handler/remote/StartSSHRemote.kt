package dev.enric.core.handler.remote

import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.MalformedDataException
import dev.enric.exceptions.RemoteConnectionException
import java.io.PrintWriter
import java.net.Socket

class StartSSHRemote {

    // TODO: Dont use the clean password, use TLS
    fun connection(username: String, password: String, host: String, port: Int, path: String): Socket {
        val protocol = DataProtocol.newTrackitInstance(
            user = username,
            password = password,
            host = host,
            port = port,
            path = path
        )

        try {
            val socket = Socket(host, 8088) // TODO: Make this configurable
            val responseStr = sendConnectionPetition(socket, protocol)
            val (_, _, _, _, redirectPort, _) = DataProtocol.validateRequest(responseStr)?.destructured
                ?: throw MalformedDataException("Invalid response format")


            return retryConnection(host, redirectPort.toInt(), 10, 100)
        } catch (ex: Exception) {
            throw MalformedDataException("Error starting SSH connection: ${ex.message}")
        }
    }

    private fun sendConnectionPetition(
        socket: Socket,
        protocol: DataProtocol
    ): String {
        val writer = PrintWriter(socket.getOutputStream(), true)
        val reader = socket.getInputStream().bufferedReader()

        writer.println(protocol.toString())
        return reader.readLine()
    }

    fun retryConnection(host: String, port: Int, retries: Int = 10, delayMillis: Long = 100): Socket {
        repeat(retries) { attempt ->
            try {
                return Socket(host, port)
            } catch (e: Exception) {
                if (attempt == retries - 1) throw e
                Thread.sleep(delayMillis)
            }
        }

        throw RemoteConnectionException("Failed to connect to $host:$port after $retries attempts")
    }
}