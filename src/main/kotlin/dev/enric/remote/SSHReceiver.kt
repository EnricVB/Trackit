package dev.enric.remote

import dev.enric.logger.Logger
import dev.enric.remote.message.ITrackitMessage
import dev.enric.remote.remoteObject.MessageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SSHReceiver(
    private val port: Int,
    private val authorizedKeys: List<String>
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val messageQueue: BlockingQueue<ITrackitMessage<*>> = LinkedBlockingQueue()

    /**
     * Starts the SSH server and listens for incoming connections.
     *
     * This method is called to initiate the SSH server and start accepting connections.
     * It runs in a separate thread to avoid blocking the main thread.
     */
    fun startConnection(connection: SSHConnection) {
        // Start the SSH server and listen for incoming connections
        launch {
            try {
                if (!connection.isAuthenticated()) {
                    Logger.error("Unauthorized SSH public key: ${connection.getPublicKey()}")
                    connection.sendError("Authentication failed")
                    return@launch
                }

                Logger.debug("Connection authenticated: ${connection.getPublicKey()}")

                startReceivingMessages(connection)
                startProcessingMessages(connection)

            } catch (e: Exception) {
                Logger.error("Unexpected error in SSHReceiver: ${e.message}")
            }
        }.start()
    }

    /**
     * Continuously receives messages from the SSH connection and puts them into the queue.
     */
    private suspend fun startReceivingMessages(connection: SSHConnection) {
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
    private suspend fun startProcessingMessages(connection: SSHConnection) {
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