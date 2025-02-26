package dev.enric.command

import dev.enric.logger.Logger
import picocli.CommandLine.Option
import java.util.concurrent.Callable

abstract class TrackitCommand : Callable<Int> {
    @Option(names = ["--only-errors"], description = ["Only shows errors, skips logs"])
    var onlyErrors: Boolean = false

    @Option(names = ["--quiet", "-q"], description = ["Doesn't show logs"])
    var quiet: Boolean = false

    override fun call(): Int {
        setupLogger()

        return 0
    }

    protected fun setupLogger() {
        Logger.setupLogger(
            when {
                onlyErrors -> Logger.LogLevel.ERRORS
                quiet -> Logger.LogLevel.QUIET
                else -> Logger.LogLevel.INFO
            }
        )
    }
}