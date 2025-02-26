package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.config.KeepSession
import dev.enric.logger.Logger
import dev.enric.util.AuthUtil
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "config",
    description = ["Configure common settings for the repository or whole system"],
)
class Config : TrackitCommand() {

    @Option(names = ["--username", "-u"], description = ["Define the username to configure"])
    var username: String? = null

    @Option(names = ["--password", "-p"], description = ["Define the password of the user"])
    var password: String? = null

    @Option(names = ["--keep-session", "-ks"], description = ["Keep session open after closing the terminal"])
    var keepSession: Boolean = false

    @Option(names = ["--global"], description = ["Apply changes at system level"])
    var global: Boolean = false

    @Option(names = ["--local"], description = ["Apply changes at repository level (default)"])
    var local: Boolean = true

    override fun call(): Int {
        super.call()

        if (keepSession) {
            keepSession()
        }

        return 0
    }

    private fun keepSession() {
        val keepSession = KeepSession(username!!, password)
        if (!AuthUtil.authenticate(username!!, password!!)) {
            Logger.error("Invalid credentials")
            return
        }

        if (global) {
            Logger.log("Saving session at system level")
            keepSession.globalSave()
        } else {
            Logger.log("Saving session at repository level")
            keepSession.localSave()
        }

        Logger.log("Session saved")
    }
}