package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.remote.StartSSHRemote
import picocli.CommandLine.*

@Command(
    name = "test",
    description = ["Starts a TCP Connection for granting access to comands like push or pull."],
    mixinStandardHelpOptions = false,
)
class TestRemote : TrackitCommand() {

    override fun call(): Int {
        super.call()

        StartSSHRemote().startConnection(
            username = "test",
            password = "test",
            host = "localhost",
            port = 8088,
            path = "C:\\Users\\enric.velasco\\Desktop\\trackit\\tktFolder"
        )

        return 0
    }
}