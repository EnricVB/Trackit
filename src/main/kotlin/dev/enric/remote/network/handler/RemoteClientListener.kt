package dev.enric.remote.network.handler

import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

class RemoteClientListener(private val connection: RemoteConnection) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val messageQueue = LinkedBlockingQueue<ITrackitMessage<*>>()

    fun start() {
        launch {
            val receivingJob = launch { startReceiving() }
            val processingJob = launch { startProcessing() }

            receivingJob.join()
            processingJob.join()
        }
    }

    private suspend fun startReceiving() {
        while (connection.isOpen()) {
            val raw = connection.receiveMessage()
            if (raw != null) {
                val message = MessageFactory.decode(raw)
                RemoteChannel.handleIncomingMessage(message)
                messageQueue.offer(message)
            }
        }
    }

    private suspend fun startProcessing() {
        val remoteChannel = RemoteChannel(connection.socket)

        while (connection.isOpen()) {
            try {
                withContext(Dispatchers.IO) {
                    messageQueue.take()
                }?.execute(connection.socket)
            } catch (e: Exception) {
                remoteChannel.sendError("Error procesando mensaje: ${e.message}")
            }
        }
    }
}
