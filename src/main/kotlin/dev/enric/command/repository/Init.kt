package dev.enric.command.repository

import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(
    name = "--init",
    description = ["Initialize a new repository"]
)
class Init : Callable<Int> {

    override fun call(): Int {
        RepositoryFolderManager().createRepositoryFolder()

        return 0
    }
}