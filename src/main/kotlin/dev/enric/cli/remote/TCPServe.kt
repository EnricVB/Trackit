package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.remote.tcp.RemoteReceiver
import picocli.CommandLine.*

@Command(
    name = "tcp-serve",
    description = ["Starts a TCP Connection for granting access to comands like push or pull."],
    mixinStandardHelpOptions = false,
)
class TCPServe : TrackitCommand() {

    override fun call(): Int {
        super.call()

        RemoteReceiver(
            port = 8088
        ).start()

        return 0
    }
}