package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.repo.commit.CommitHandler
import dev.enric.domain.Commit
import picocli.CommandLine.*

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
    @Option(names = ["--all", "-a"], description = ["Stage all modified and untracked files before committing"])
    var stageAllFiles: Boolean = false

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

        val commit = Commit(title = title, message = message)
        val commitHandler = CommitHandler(commit)

        // Sets some commit properties before the commit
        commitHandler.initializeCommitProperties(sudoArgs, confirmerArgs)

        // Stages all files if the flag is set
        commitHandler.preCommit(stageAllFiles)

        // Will never return 1 as it will throw an exception if the commit cannot be done
        if (!commitHandler.canDoCommit()) {
            return 1
        }

        // Processes the commit
        commitHandler.processCommit()
        commitHandler.postCommit()

        return 0
    }
}