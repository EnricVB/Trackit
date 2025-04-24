package dev.enric.command

import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import picocli.CommandLine.Option
import java.util.concurrent.Callable

/**
 * Base abstract class for all Trackit commands.
 *
 * Provides common options and behavior for:
 * - Logging level configuration
 * - Sudo-like user impersonation
 *
 * All commands in Trackit should extend this class to inherit logging
 * and sudo argument handling.
 */
abstract class TrackitCommand : Callable<Int> {

    /**
     * Suppress all logs except errors.
     * Useful for minimal output during automation.
     */
    @Option(names = ["--only-errors"], description = ["Only shows errors, skips logs"])
    var onlyErrors: Boolean = false

    /**
     * Completely suppress all logs (info and errors).
     * Useful when absolute silence is required.
     */
    @Option(names = ["--quiet", "-q"], description = ["Doesn't show logs"])
    var quiet: Boolean = false

    /**
     * Show debug logs.
     * Useful for troubleshooting and detailed output.
     */
    @Option(names = ["--verbose"], description = ["Shows debug logs"])
    var verbose: Boolean = false

    /**
     * Execute the command as another user (sudo-like behavior).
     *
     * Parameters:
     * 1. Username
     * 2. Password
     *
     * This is useful for administrative operations or when
     * acting on behalf of another user.
     *
     * Example:
     *   --sudo admin mypassword
     *
     * Custom parsing is handled by [SudoArgsParameterConsumer].
     */
    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    /**
     * Called automatically when the command is executed.
     *
     * Sets up the logging level based on the provided flags:
     * - `--only-errors` => show only errors
     * - `--quiet`       => suppress all logs
     * - default         => info level
     *
     * Commands should override this method and call `super.call()`
     * to ensure logger setup is performed.
     *
     * @return Exit code (default 0; can be overridden by subclasses)
     */
    override fun call(): Int {
        setupLogger()
        return 0
    }

    /**
     * Configures the logger based on the selected verbosity flags.
     *
     * The logger is tagged with the simple class name for contextual logs.
     */
    protected fun setupLogger() {
        Logger.setupLogger(
            when {
                onlyErrors -> Logger.LogLevel.ERRORS
                quiet -> Logger.LogLevel.QUIET
                verbose -> Logger.LogLevel.VERBOSE
                else -> Logger.LogLevel.INFO
            },
            this.javaClass.simpleName
        )
    }

    /**
     * Prompts the user for a username.
     *
     * @return The entered username
     */
    protected fun askForUsername(): String {
        val console = SystemConsoleInput.getInstance()

        return console.readLine("Enter username: ")
    }

    /**
     * Prompts the user for a password.
     * This method hides the password input.
     *
     * @return The entered password
     */
    protected fun askForPassword(): String {
        val console = SystemConsoleInput.getInstance()

        return String(console.readPassword("Enter password: "))
    }
}