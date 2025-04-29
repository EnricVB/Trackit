package dev.enric.cli.branch

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.branch.BranchHandler
import picocli.CommandLine.Command

/**
 * The BranchListCommand class is responsible for listing all the available branches in the Trackit version control system.
 * It retrieves and displays a list of branches that exist in the current repository.
 *
 * Usage example:
 * - List all branches:
 *   trackit branch list-branch
 */
@Command(
    name = "list-branch",
    mixinStandardHelpOptions = true,
    description = ["Lists all available branches in the repository."],
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit branch list-branch",
        "    Lists all the branches available in the current repository."
    ]
)
class BranchListCommand : TrackitCommand() {

    /**
     * Executes the command to list all available branches.
     * It retrieves the list of branches and displays them to the user.
     *
     * @return an exit code indicating the success or failure of the operation (0 for success, 1 for failure).
     */
    override fun call(): Int {
        super.call()

        // Call the BranchHandler to list the branches
        BranchHandler().listBranches()

        return 0
    }
}
