package dev.enric.command.repo.repository

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.ignore.IgnoreHandler
import dev.enric.logger.Logger
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Path

/**
 * Command to ignore a specific file or directory in the repository.
 *
 * This command prevents the specified path from being tracked by the repository's version control.
 * It updates the repository’s ignore configuration so that the provided file or directory (and its
 * contents if a directory) will be excluded from staging and committing.
 *
 * Usage examples:
 *   trackit ignore build/
 *   trackit ignore temp/data.txt
 */
@Command(
    name = "ignore",
    description = ["Ignores a file from the repository from being tracked"],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Examples:",
        "  trackit ignore build/",
        "    Ignores the 'build/' directory and all of its contents from being tracked in the repository.",
        "",
        "  trackit ignore temp/data.txt",
        "    Ignores the 'temp/data.txt' file from being tracked in the repository.",
        "",
        "Notes:",
        "  - The specified path should be relative to the root directory of the repository.",
        "  - If a directory is provided, all files and subdirectories inside it will be ignored.",
        "  - The ignored files or directories will not be staged or committed, and their changes will be excluded from version control.",
        "  - To undo the ignore action, manually remove the corresponding entry from the repository’s ignore configuration.",
        "  - This command only affects files tracked by the repository’s ignore settings, not existing commits.",
        "",
    ]

)
class Ignore : TrackitCommand() {

    /**
     * The path of the file or directory to be ignored, relative to the repository folder.
     *
     * Examples:
     * - To ignore a file in the root directory: `file.txt`
     * - To ignore a file in a subfolder: `subfolder/file.txt`
     * - To ignore a directory and all its contents: `folder/`
     */
    @Parameters(index = "0", paramLabel = "path", description = ["The path of the file/directory to be ignored"])
    lateinit var path: Path

    /**
     * Executes the ignore command logic.
     *
     * 1. Resolves the provided relative path against the repository root folder.
     * 2. Calls the IgnoreHandler to update the repository ignore configuration.
     * 3. Logs confirmation of the ignore action.
     *
     * @return Exit code 0 if successful.
     */
    override fun call(): Int {
        super.call()

        // Handle the ignore operation
        IgnoreHandler().ignore(path)

        // Log the action
        Logger.log("File ignored")

        return 0
    }
}