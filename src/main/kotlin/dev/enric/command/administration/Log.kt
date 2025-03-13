package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.administration.LogHandler
import dev.enric.core.security.config.KeepSession
import dev.enric.logger.Logger
import dev.enric.core.security.AuthUtil
import dev.enric.util.index.UserIndex
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.Console

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