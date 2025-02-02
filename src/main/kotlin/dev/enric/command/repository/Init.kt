package dev.enric.command.repository

import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(
    name = "init",
    description = ["Initialize a new repository"]
)
class Init : Callable<Int> {

    /**
     * Create a new repository folder in the current directory.
     * @see RepositoryFolderManager.createRepositoryFolder
     */
    override fun call(): Int {
        RepositoryFolderManager().createRepositoryFolder()

        return 0
    }
}