package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.command.repo.staging.Stage
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.repo.commit.CommitHandler
import dev.enric.core.repo.staging.StagingHandler
import dev.enric.domain.Commit
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

    /**
     * Execute the command as a different user
     */
    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    /**
     * Confirm the commit as a different user
     */
    @Option(
        names = ["--confirmer", "-c"],
        description = ["Confirm commit as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var confirmerArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        if(stageAllFiles) {
            stageAllFiles()
        }

        createCommit(title, message)
        StagingHandler.clearStagingArea()

        return 0
    }

    fun createCommit(title: String, message: String) {
        val commit = Commit(title = title, message = message, date = Timestamp.from(Instant.now()))
        val commitHandler = CommitHandler(commit)

        // TODO: Check if can do a commit in specified branch.
        // if (!commitHandler.canDoCommit(commit, branch) {
        //     return 1
        // }

        commitHandler.preCommit(sudoArgs, confirmerArgs)
        commitHandler.processCommit()
        commitHandler.postCommit()
    }

    fun stageAllFiles() {
        Logger.log("Staging all files before committing")

        val stageCommand = Stage()

        stageCommand.path = "."
        stageCommand.force = false

        stageCommand.call()
    }
}