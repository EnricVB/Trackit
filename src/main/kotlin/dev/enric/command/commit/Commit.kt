package dev.enric.command.commit

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.commit.CommitHandler
import dev.enric.core.handler.staging.StagingHandler
import picocli.CommandLine.*
import java.sql.Timestamp
import java.time.Instant

@Command(
    name = "commit",
    description = ["Commits the staged files"]
)
class Commit : TrackitCommand() {
    @Parameters(index = "0", paramLabel = "TITLE", description = ["The commit title"])
    lateinit var title: String

    @Parameters(index = "1", paramLabel = "MESSAGE", description = ["The commit message"])
    lateinit var message: String

    @Option(names = ["--all", "-A"], description = ["Stage all modified and untracked files before committing"])
    var stageAllFiles: Boolean = false

    override fun call(): Int {
        super.call()

        var commit =
            dev.enric.core.objects.Commit(title = title, message = message, date = Timestamp.from(Instant.now()))

        commit = CommitHandler().processCommit(commit)
        //CommitHandler().postProcessCommit(commit)

        StagingHandler.clearStagingArea()

        return 0
    }
}