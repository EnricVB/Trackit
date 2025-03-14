package dev.enric.command

import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.security.AuthUtil
import dev.enric.logger.Logger
import picocli.CommandLine.Option
import java.util.concurrent.Callable

abstract class TrackitCommand : Callable<Int> {
    @Option(names = ["--only-errors"], description = ["Only shows errors, skips logs"])
    var onlyErrors: Boolean = false

    @Option(names = ["--quiet", "-q"], description = ["Doesn't show logs"])
    var quiet: Boolean = false

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    /**
     * This method is called when the command is executed
     */
    override fun call(): Int {
        setupLogger()

        return 0
    }

    /**
     * Setup the logger with the desired log level
     */
    protected fun setupLogger() {
        Logger.setupLogger(
            when {
                onlyErrors -> Logger.LogLevel.ERRORS
                quiet -> Logger.LogLevel.QUIET
                else -> Logger.LogLevel.INFO
            },
            this.javaClass.simpleName
        )
    }
}