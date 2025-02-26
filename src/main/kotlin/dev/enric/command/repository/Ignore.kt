package dev.enric.command.repository

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.ignore.IgnoreHandler
import dev.enric.logger.Logger
import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters


@Command(
    name = "ignore",
    description = ["Ignores a file from the repository from being tracked"],
    mixinStandardHelpOptions = true,
    )
class Ignore : TrackitCommand() {

    /**
     * The path of the file/directory to be ignored, relative to the repository folder.
     *
     * In case you want to ignore a file named `file.txt` located in the root of the repository, you should pass `file.txt` as the argument.
     *
     * You can also ignore a file located in a subdirectory of the repository folder. In this case, you should pass the path relative to the repository folder.
     *
     * If you pass a directory, all the files and directories inside it will be ignored.
     */
    @Parameters(index = "0", paramLabel = "path", description = ["The path of the file/directory to be ignored"])
    lateinit var path: String

    private val repositoryFolder = RepositoryFolderManager().initFolder

    override fun call(): Int {
        super.call()

        IgnoreHandler.ignore(repositoryFolder.resolve(path))
        Logger.log("File ignored")

        return 0
    }
}