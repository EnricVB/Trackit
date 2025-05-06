package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.remote.PushHandler
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

        val handler = PushHandler()
        val socket = handler.startRemoteConnection()

        // Check if Remote has no pull requests

        // Send all objects to the remote server
        handler.sendObjectsToRemote(socket, BranchIndex.getCurrentBranch())

        return@runBlocking 0
    }
}