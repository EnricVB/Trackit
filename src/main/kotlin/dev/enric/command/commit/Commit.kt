package dev.enric.command.commit

import dev.enric.core.handler.commit.CommitHandler
import dev.enric.core.handler.staging.StagingHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.sql.Timestamp
import java.time.Instant
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
        val commit = dev.enric.core.objects.Commit(title = title, message = message, date = Timestamp.from(Instant.now()))

        CommitHandler().processCommit(commit)
        //CommitHandler().postProcessCommit(commit)

        StagingHandler.clearStagingArea()

        return 0
    }
}