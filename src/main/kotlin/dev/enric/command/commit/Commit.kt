package dev.enric.command.commit

import dev.enric.command.TrackitCommand
import dev.enric.command.staging.Stage
import dev.enric.core.handler.commit.CommitHandler
import dev.enric.core.handler.staging.StagingHandler
import dev.enric.core.objects.Commit
import dev.enric.logger.Logger
import picocli.CommandLine.*
import java.sql.Timestamp
import java.time.Instant

@Command(
    name = "commit",
    description = ["Commits the staged files"],
    mixinStandardHelpOptions = true,
    )
class Commit : TrackitCommand() {

    /**
     * Title of the commit to be created, it should be a short description of the changes
     */
    @Parameters(index = "0", paramLabel = "TITLE", description = ["The commit title"])
    lateinit var title: String

    /**
     * Message of the commit to be created, it should be a detailed description of the changes
     */
    @Parameters(index = "1", paramLabel = "MESSAGE", description = ["The commit message"])
    lateinit var message: String

    /**
     * Stage all modified and untracked files before committing, it will add all the files to the staging area
     */
    @Option(names = ["--all", "-A"], description = ["Stage all modified and untracked files before committing"])
    var stageAllFiles: Boolean = false

    override fun call(): Int {
        super.call()

        if(stageAllFiles) {
            stageAllFiles()
        }

        createCommit(title, message)
        StagingHandler.clearStagingArea()

        return 0
    }

    fun stageAllFiles() {
        Logger.log("Staging all files before committing")

        val stageCommand = Stage()

        stageCommand.path = "."
        stageCommand.force = false

        stageCommand.call()
    }

    fun createCommit(title: String, message: String) {
        val commit = Commit(title = title, message = message, date = Timestamp.from(Instant.now()))
        CommitHandler().processCommit(commit)
    }
}