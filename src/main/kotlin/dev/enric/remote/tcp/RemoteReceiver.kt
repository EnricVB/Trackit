package dev.enric.remote.tcp

import dev.enric.logger.Logger
import dev.enric.remote.tcp.message.ITrackitMessage
import dev.enric.remote.tcp.remoteObject.MessageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class RemoteReceiver(
    private val port: Int,
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val messageQueue: BlockingQueue<ITrackitMessage<*>> = LinkedBlockingQueue()

    fun start() {
        var serverSocket: ServerSocket? = null

        try {
            serverSocket = ServerSocket(port)
            serverSocket.soTimeout = 5000

            while (true) {
                val socket = serverSocket.accept()
                val connection = RemoteConnection(
                    socket,
                    socket.getInputStream(),
                    socket.getOutputStream()
                )

                this.startConnection(connection)
            }

        } catch (e: Exception) {
            Logger.error("Could not start SSHReceiver: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    /**
     * Starts the SSH server and listens for incoming connections.
     *
     * This method is called to initiate the SSH server and start accepting connections.
     * It runs in a separate thread to avoid blocking the main thread.
     */
    private fun startConnection(connection: RemoteConnection) {
        // Start the SSH server and listen for incoming connections
        launch {
            try {
                if (!connection.isAuthenticated()) {
                    connection.sendError("Authentication failed")
                    return@launch
                }

                val receivingJob = launch { startReceivingMessages(connection) }
                val processingJob = launch { startProcessingMessages(connection) }

                receivingJob.join()
                processingJob.join()
            } catch (e: Exception) {
                Logger.error("Unexpected error in SSHReceiver: ${e.message}")
            }
        }.start()
    }

    /**
     * Continuously receives messages from the SSH connection and puts them into the queue.
     */
    private suspend fun startReceivingMessages(connection: RemoteConnection) {
        while (connection.isOpen()) {
            try {
                val rawMessage: ByteArray? = connection.receiveMessage()
                if (rawMessage != null) {
                    val message = MessageFactory.decode(rawMessage)
                    messageQueue.offer(message)
                }
            } catch (e: Exception) {
                Logger.error("Error receiving or decoding message: ${e.message}")
                connection.sendError("Failed to receive message: ${e.message}")
            }
        }
    }

    /**
     * Continuously processes messages from the queue to execute if valid.
     */
    private suspend fun startProcessingMessages(connection: RemoteConnection) {
        while (connection.isOpen()) {
            try {
                val message = messageQueue.poll()
                if (message.validateMessage()) {
                    message.execute()
                } else {
                    Logger.error("Invalid message received: ${message.id}")
                    connection.sendError("Invalid message format")
                }
            } catch (e: Exception) {
                Logger.error("Error processing message: ${e.message}")
                connection.sendError("Error processing message: ${e.message}")
            }
        }
    }
}