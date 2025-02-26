package dev.enric.command.repository

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.ignore.IgnoreHandler
import dev.enric.logger.Logger
import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters


@Command(
    name = "ignore",
    description = ["Ignores a file from the repository from being tracked"]
)
class Ignore : TrackitCommand() {
    @Parameters(index = "0", paramLabel = "path", description = ["The path of the file/directory to be ignored"])
    lateinit var path: String

    private val repositoryFolder = RepositoryFolderManager().initFolder

    override fun call(): Int {
        super.call()

        IgnoreHandler().ignore(repositoryFolder.resolve(path))
        Logger.log("File ignored")

        return 0
    }
}