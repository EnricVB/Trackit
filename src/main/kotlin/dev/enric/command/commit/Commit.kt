package dev.enric.command.commit

import dev.enric.util.commit.CommitHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable

@Command(
    name = "commit",
    description = ["Commits the staged files"]
)
class Commit : Callable<Int> {
    @Parameters(paramLabel = "title", description = ["The commit title"])
    var title: String = ""

    @Parameters(paramLabel = "message", description = ["The commit message"])
    var message: String = ""

    @Option(names = ["--all", "-A"], description = ["Add all files to the staging area"])
    var stageAllFiles = false

    override fun call(): Int {
        CommitHandler().createCommitTree()

        return 0
    }
}