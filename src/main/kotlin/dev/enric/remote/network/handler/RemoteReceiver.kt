package dev.enric.remote.network.handler

import dev.enric.logger.Logger
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class RemoteReceiver(
    private val port: Int,
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val messageQueue: BlockingQueue<ITrackitMessage<*>> = LinkedBlockingQueue()
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun startServerConnection() {
        var serverSocket: ServerSocket? = null

        try {
            serverSocket = ServerSocket(port)
            serverSocket.soTimeout = 10000

            while (true) {
                val socket = serverSocket.accept()
                val connection = RemoteConnection(socket)

                this.startConnection(connection)
            }

        } catch (e: Exception) {
            Logger.error("Could not start TCPReceiver: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    /**
     * Starts the TCP server and listens for incoming connections.
     *
     * This method is called to initiate the TCP server and start accepting connections.
     * It runs in a separate thread to avoid blocking the main thread.
     */
    fun startConnection(connection: RemoteConnection) {
        // Start the TCP server and listen for incoming connections
        val remoteChannel = RemoteChannel(connection.socket)

        launch {
            try {
                if (!connection.isAuthenticated()) {
                    remoteChannel.sendError("Authentication failed")
                    return@launch
                }

                val receivingJob = launch { startReceivingMessages(connection) }
                val processingJob = launch { startProcessingMessages(connection) }

                receivingJob.join()
                processingJob.join()
            } catch (e: Exception) {
                Logger.error("Unexpected error in TCPReceiver: ${e.message}")
            } finally {
                connection.close()
            }
        }.start()
    }

    /**
     * Continuously receives messages from the TCP connection and puts them into the queue.
     */
    private suspend fun startReceivingMessages(connection: RemoteConnection) {
        val remoteChannel = RemoteChannel(connection.socket)

        Logger.debug("Starting to receive messages on port $port")

        while (connection.isOpen()) {
            val rawMessage: ByteArray? = connection.receiveMessage()
            if (rawMessage != null) {
                try {
                    val message = MessageFactory.decode(rawMessage)
                    RemoteChannel.handleIncomingMessage(message)

                    Logger.debug("Received message: ${message.id}")
                    messageQueue.offer(message)
                } catch (e: Exception) {
                    Logger.error("Error decoding message: ${e.message}")
                    remoteChannel.sendError("Failed to decode message: ${e.message}")
                }
            }
        }

        Logger.debug("Stopped receiving messages on port $port")
    }

    /**
     * Continuously processes messages from the queue to execute if valid.
     */
    private suspend fun startProcessingMessages(connection: RemoteConnection) {
        val remoteChannel = RemoteChannel(connection.socket)

        while (connection.isOpen()) {
            try {
                withContext(dispatcher) { messageQueue.take() }?.execute(connection.socket)
            } catch (e: Exception) {
                Logger.error("Error processing message: ${e.message}")
                remoteChannel.sendError("Error processing message: ${e.message} \n${e.stackTraceToString()}")
            }
        }
    }
}