package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.staging.StatusHandler
import picocli.CommandLine.Command

/**
 * Command to display the status of the working directory and staging area.
 *
 * This command informs the user about:
 *  - Modified files
 *  - Untracked files
 *  - Files staged for commit
 *
 * Equivalent to `git status` in Git.
 *
 * Usage example:
 *   trackit status
 */
@Command(
    name = "status",
    description = ["Show the status of the working directory"],
    mixinStandardHelpOptions = true,
)
class Status : TrackitCommand() {

    /**
     * Executes the status check on the current repository.
     *
     * Flow:
     * 1. Initializes the base command with `super.call()`.
     * 2. Calls `StatusHandler.printStatus()` to compute and display status.
     * 3. Returns exit code 0.
     *
     * @return Exit code 0 on success.
     */
    override fun call(): Int {
        // Ensures any setup from TrackitCommand is performed (e.g., sudo checks, env validation)
        super.call()

        // Delegates status computation and printing to the handler
        StatusHandler.printStatus()

        return 0
    }
}