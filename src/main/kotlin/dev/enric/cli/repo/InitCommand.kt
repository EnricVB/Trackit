package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.InitHandler
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import picocli.CommandLine.Command

/**
 * Command to initialize a new Trackit repository in the current directory.
 *
 * This sets up the necessary internal folder structure and files for Trackit
 * to begin tracking files, similar to how `git init` works in Git.
 *
 * Example usage:
 *   trackit init
 */
@Command(
    name = "init",
    description = ["Initialize a new repository"],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Example:",
        "  trackit init",
        "    Initializes a new Trackit repository in the current directory.",
        "",
        "Notes:",
        "  - This command sets up the necessary internal folder structure and configuration files for Trackit.",
        "  - The repository will start tracking changes in the current directory, and you can begin adding files and commits.",
        "  - After running this command, you can use other Trackit commands such as 'trackit add' and 'trackit commit' to start working with the repository.",
        "  - It is recommended to run 'trackit conifg --keep-session' to keep the session active and not to use sudo on each command.",
        "  - This is similar to how 'git init' works in Git.",
        "",
    ]
)
class InitCommand : TrackitCommand() {

    /**
     * Executes the repository initialization.
     *
     * The command performs the following steps:
     * 1. Calls the InitHandler to create the internal folder structure.
     * 2. Logs a confirmation message upon successful initialization.
     *
     * @return Exit code 0 if successful.
     * @see RepositoryFolderManager.createRepositoryFolder for folder creation logic.
     */
    override fun call(): Int {
        // Ensures any required setup from TrackitCommand is done
        super.call()

        // Handles the initialization logic (folder creation, metadata setup, etc.)
        InitHandler().init()

        // Inform the user that initialization succeeded
        Logger.info("Repository initialized")

        return 0
    }
}