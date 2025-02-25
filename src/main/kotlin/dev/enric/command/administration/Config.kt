package dev.enric.command.administration

import dev.enric.core.handler.config.KeepSession
import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "config",
    description = ["Configure common settings for the repository or whole system"],
)
class Config : Callable<Int> {

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

    private val localConfigPath = File(RepositoryFolderManager.CONFIG_FILE)
    private val globalConfigPath = File(System.getProperty("user.home"), RepositoryFolderManager.CONFIG_FILE)


    override fun call(): Int {
        if(keepSession) {
            keepSession()
        }

        return 0
    }

    private fun keepSession() {
        val keepSession = KeepSession(username!!, password)
        //TODO Check if user is correctly authenticated

        if(global) {
            keepSession.globalSave()
        } else {
            keepSession.localSave()
        }

        println(keepSession.getToken())
    }
}