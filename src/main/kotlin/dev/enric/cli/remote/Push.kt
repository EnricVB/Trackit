package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.remote.StartSSHRemote
import dev.enric.remote.message.query.StatusQueryMessage
import dev.enric.remote.message.response.StatusResponseMessage
import dev.enric.remote.network.handler.RemoteChannel
import dev.enric.util.index.BranchIndex
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command

@Command(
    name = "push",
    description = ["Push the current repository to a remote server"],
    mixinStandardHelpOptions = false,
)
class Push : TrackitCommand() {

    override fun call(): Int = runBlocking {
        super.call()

        val socket = StartSSHRemote().connection(
            username = "test",
            password = "test",
            host = "localhost",
            port = 8088,
            path = "C:\\tktFolder"
        )

        val response = RemoteChannel(socket).request<StatusResponseMessage>(
            message = StatusQueryMessage(BranchIndex.getCurrentBranch().name)
        )

        println("A ${response.payload}")

        return@runBlocking 0
    }
}