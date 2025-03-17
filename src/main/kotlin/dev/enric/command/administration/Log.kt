package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.LogHandler
import picocli.CommandLine.Command

@Command(
    name = "log",
    description = ["Shows the commit history of the repository"],
    mixinStandardHelpOptions = true,
    )
class Log : TrackitCommand() {

    override fun call(): Int {
        super.call()

        LogHandler().showLog()

        return 0
    }
}