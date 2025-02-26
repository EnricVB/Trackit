package dev.enric.command.repository

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.init.InitHandler
import dev.enric.logger.Logger
import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command

@Command(
    name = "init",
    description = ["Initialize a new repository"],
    mixinStandardHelpOptions = true,
)
class Init : TrackitCommand() {

    /**
     * Create a new repository folder in the current directory.
     * @see RepositoryFolderManager.createRepositoryFolder
     */
    override fun call(): Int {
        super.call()

        InitHandler.init()
        Logger.log("Repository initialized")

        return 0
    }
}