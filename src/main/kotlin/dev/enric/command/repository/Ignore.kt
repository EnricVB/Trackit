package dev.enric.command.repository

import dev.enric.util.RepositoryFolderManager
import dev.enric.core.handler.ignore.IgnoreHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable


@Command(
    name = "ignore",
    description = ["Ignores a file from the repository from being tracked"]
)
class Ignore : Callable<Int> {
    @Parameters(index = "0", paramLabel = "path", description = ["The path of the file/directory to be ignored"])
    lateinit var path: String

    private val repositoryFolder = RepositoryFolderManager().initFolder

    override fun call(): Int {
        IgnoreHandler().ignore(repositoryFolder.resolve(path))

        return 0
    }
}