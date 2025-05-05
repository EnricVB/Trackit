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

    @Option(
        names = ["--port"],
        required = true
    )
    var port: Int = 8088

    override fun call(): Int {
        super.call()
        RemoteReceiver(port = port).start()

        return 0
    }
}