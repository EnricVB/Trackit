package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.config.KeepSession
import dev.enric.logger.Logger
import dev.enric.core.security.AuthUtil
import dev.enric.util.index.UserIndex
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.Console

@Command(
    name = "config",
    description = ["Configure common settings for the repository or whole system"],
    mixinStandardHelpOptions = true,
    )
class Config : TrackitCommand() {

    /**
     * Username to authenticate the user
     */
    @Option(names = ["--username", "-u"], description = ["Define the username to configure"], required = false)
    var username: String? = null

    /**
     * Password to authenticate the user
     *
     * If the password is not provided, the user will be prompted to enter it
     */
    @Option(names = ["--password", "-p"], description = ["Define the password of the user"], required = false)
    var password: String? = null

    /**
     * Keep the session open after closing the terminal
     */
    @Option(names = ["--keep-session", "-ks"], description = ["Keep session open after closing the terminal"], required = false)
    var keepSession: Boolean = false

    /**
     * Apply changes at system level saving it on the user's environment variables
     */
    @Option(names = ["--global"], description = ["Apply changes at system level"], required = false)
    var global: Boolean = false

    /**
     * Apply changes at repository level saving it on the repository's configuration
     */
    @Option(names = ["--local"], description = ["Apply changes at repository level (default)"], required = false)
    var local: Boolean = true

    override fun call(): Int {
        super.call()

        if (keepSession) {
            if (!validateCredentials()) {
                Logger.error("Invalid credentials")
                return 1
            }

            saveSession()
        }

        return 0
    }

    /**
     * Validate the credentials provided by the user
     */
    private fun validateCredentials(): Boolean {
        if (username.isNullOrBlank()) {
            val console: Console? = System.console()
            if (console != null) {
                username = console.readLine("Enter username: ")
            } else {
                Logger.error("Username is required but cannot be read in this environment")
                return false
            }
        }

        if (password.isNullOrBlank()) {
            val console: Console? = System.console()
            if (console != null) {
                password = String(console.readPassword("Enter password: "))
            } else {
                Logger.error("Password is required but cannot be read in this environment")
                return false
            }
        }

        return AuthUtil.authenticate(username!!, password!!)
    }

    /**
     * Save the session in the system or repository
     */
    private fun saveSession() {
        val salt = UserIndex.getUser(username ?: "")?.salt

        val session = KeepSession(username ?: "", password ?: "", salt)

        if (global) {
            Logger.log("Saving session at system level")
            session.globalSave()
        } else {
            Logger.log("Saving session at repository level")
            session.localSave()
        }
        Logger.log("Session saved")
    }
}