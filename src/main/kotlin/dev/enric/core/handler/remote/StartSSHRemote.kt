package dev.enric.core.handler.remote

import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.MalformedDataException
import dev.enric.logger.Logger
import java.io.PrintWriter
import java.net.Socket

class StartSSHRemote {

    // TODO: Dont use the clean password, use TLS
    fun startConnection(username: String, password: String, host: String, port: Int, path: String) {
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

            val redirectPortInt = redirectPort.toInt()
            println(redirectPortInt)
        } catch (ex: Exception) {
            Logger.error("Error starting SSH connection: ${ex.message}")
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
}