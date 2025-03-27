package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.BlameHandler
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import picocli.CommandLine.*
import java.nio.file.Path

/**
 * The Blame command identifies the author responsible for changes in a given file.
 * It retrieves the latest commit for the current branch and determines who made the changes.
 */
@Command(
    name = "blame",
    description = ["Indicates who is responsible for the changes in a file."],
    footer = [
    ],
    mixinStandardHelpOptions = true,
)

class Blame : TrackitCommand() {

    /**
     * The file path to analyze and determine authorship.
     */
    @Parameters(index = "0", paramLabel = "File", description = ["The file path to the file to blame."])
    lateinit var filePath: Path

    /**
     * Executes the blame command, retrieving the author information for the specified file.
     *
     * @return Exit code 0 if successful, throws an exception if the file is not found.
     */
    override fun call(): Int {
        super.call()

        val branch = BranchIndex.getCurrentBranch()
        val commit = BranchIndex.getBranchHead(branch.generateKey())
        val file = filePath.toFile().takeIf { it.exists() }
            ?: throw IllegalStateException("No file found at $filePath")

        Logger.log(BlameHandler().blame(file, commit))

        return 0
    }
}
