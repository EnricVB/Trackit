package dev.enric.remote.network.handler

import dev.enric.exceptions.RemoteConnectionException
import dev.enric.remote.ITrackitMessage
import dev.enric.remote.network.serialize.MessageFactory
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue

class RemoteClientListener(private val connection: RemoteConnection) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val messageQueue = LinkedBlockingQueue<ITrackitMessage<*>>()
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            val receivingJob = launch { startReceiving() }
            val processingJob = launch { startProcessing() }

            try {
                joinAll(receivingJob, processingJob)
            } catch (e: Exception) {
                throw RemoteConnectionException("Error in Remote: ${e.message}")
            }
        }
    }

    private suspend fun startReceiving() {
        while (connection.isOpen() && isActive) {
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

        while (connection.isOpen() && isActive) {
            try {
                withContext(dispatcher) {
                    messageQueue.take()
                }?.execute(connection.socket)
            } catch (e: Exception) {
                remoteChannel.sendError("Error processing message: ${e.message}")
            }
        }
    }
}
