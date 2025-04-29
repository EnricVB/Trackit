package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.StatusHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

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
    footer = [
        "",
        "Example:",
        "  trackit status",
        "    Displays the current status of modified, untracked, and staged files.",
        "",
        "Notes:",
        "  - '?' indicates untracked files. These are not added to the repository yet.",
        "  - '*' indicates unmodified files. These have no changes since the last commit.",
        "  - 'M' indicates modified files. These have changes that are not staged for commit.",
        "  - 'S' indicates files staged for commit. These changes will be included in the next commit.",
        "  - 'D' indicates deleted files. These files have been deleted and will be removed in the next commit.",
        "  - 'I' indicates ignored files. These files are not being tracked by the repository.",
        "",
    ]
)
class StatusCommand : TrackitCommand() {

    /**
     * Ignored option for the log command.
     *
     * This option is used to filter the files shown.
     * By default, the log doesn't show ignored files.
     */
    @Option(
        names = ["--show-ignored", "-i"], description = ["Shows ignored files"], required = false
    )
    var showIgnored: Boolean = false

    /**
     * Executes the status check on the current repository.
     *
     * Flow:
     * 1. Initializes the base command with `super.call()`.
     * 2. Calls `StatusHandler().printStatus()` to compute and display status.
     * 3. Returns exit code 0.
     *
     * @return Exit code 0 on success.
     */
    override fun call(): Int {
        // Ensures any setup from TrackitCommand is performed (e.g., sudo checks, env validation)
        super.call()

        // Delegates status computation and printing to the handler
        StatusHandler().printStatus(showIgnored)

        return 0
    }
}