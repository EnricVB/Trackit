package dev.enric.cli.admin

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.BlameHandler
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
    description = ["Indicates who made specified changes."],
    mixinStandardHelpOptions = true,
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit blame src/Main.kt          Identify the author of changes in the 'src/Main.kt' file.",
        "Notes:",
        "  - Ensure the file path is correct and exists in the repository.",
        "  - You can use this command to check the author for any file in the current branch."
    ],
)

class BlameCommand : TrackitCommand() {

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
        val file = filePath.toFile().takeIf { it.exists() } ?: throw IllegalStateException("No file found at $filePath")

        Logger.info(BlameHandler().blame(file, commit))

        return 0
    }
}
