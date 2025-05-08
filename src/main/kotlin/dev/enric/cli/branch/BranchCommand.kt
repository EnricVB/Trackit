package dev.enric.cli.branch

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.branch.BranchHandler
import dev.enric.core.handler.repo.CheckoutHandler
import dev.enric.domain.objects.Branch
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * The BranchCommand class provides functionality for managing branches in the Trackit version control system.
 * It allows users to create, remove, and checkout branches, as well as manage push and fetch directions.
 * The command provides options for creating a new branch, removing an existing branch,
 * and switching to the head of a branch after creation.
 *
 * Usage examples:
 * - Remove a branch:
 *   trackit branch --remove <branch-name>
 *
 * - Create a new branch:
 *   trackit branch --create <branch-name>
 *
 * - Create and checkout to a new branch:
 *   trackit branch --create <branch-name> --checkout
 *
 * - Specify push direction:
 *   trackit branch --push <direction>
 *
 * - Specify fetch direction:
 *   trackit branch --fetch <direction>
 */
@Command(
    name = "branch",
    mixinStandardHelpOptions = true,
    description = ["Manages branches in the Trackit version control system."],
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit branch --remove <branch-name>",
        "    Removes the specified branch from the index (does not remove commits).",
        "",
        "  trackit branch --create <branch-name>",
        "    Creates a new branch with the given name.",
        "",
        "  trackit branch --create <branch-name> --checkout",
        "    Creates a new branch and checks out to it.",
        "",
        "  trackit branch --push <direction>",
        "    Specifies the push direction for the branch (defaults to the current branch's direction).",
        "",
        "  trackit branch --fetch <direction>",
        "    Specifies the fetch direction for the branch (defaults to the current branch's direction)."
    ]
)
class BranchCommand : TrackitCommand() {

    /**
     * The name of the branch to remove. If specified, the branch will be removed from the index.
     * Note: This will not delete commits, only the branch reference.
     */
    @Option(
        names = ["-r", "--remove"], description = ["Will remove the named branch."]
    )
    var removeBranchName: String = ""

    /**
     * The name of the branch to create. If specified, a new branch with this name will be created.
     */
    @Option(
        names = ["-c", "--create"], description = ["Will create a new branch with the given name."]
    )
    var createBranchName: String = ""

    /**
     * Flag indicating whether to checkout to the newly created branch after creation.
     * If true, the command will automatically switch to the newly created branch.
     */
    @Option(
        names = ["-C", "--checkout"], description = ["Checkout to new created branch."]
    )
    var checkoutBranch: Boolean = false

    /**
     * Executes the branch operations based on the provided options.
     * Depending on the options set, it will either remove an existing branch, create a new one,
     * or switch to the newly created branch (if specified).
     *
     * @return an exit code indicating the success or failure of the operation (0 for success, 1 for failure).
     */
    override fun call(): Int {
        super.call()
        val currentBranch = CommitIndex.getCurrentCommit()?.branch?.let { Branch.newInstance(it) }

        // If the removeBranchName option is provided, remove the branch from the index (not the commits).
        if (removeBranchName.isNotBlank()) {
            val branch = BranchIndex.getBranch(removeBranchName)
            BranchHandler(branch, sudoArgs).removeBranch()
            return 0
        }

        // If the createBranchName option is provided, create a new branch.
        if (createBranchName.isNotBlank()) {
            BranchHandler(currentBranch, sudoArgs).createBranch(createBranchName)

            // If the checkoutBranch flag is true, checkout to the newly created branch.
            if (checkoutBranch) {
                checkoutToBranch(createBranchName)
            }

            return 0
        }

        return 1
    }

    /**
     * Checkout to the branch with the given name.
     *
     * @param branchName The name of the branch to checkout to.
     * @throws BranchNotFoundException if the branch with the given name does not exist.
     */
    private fun checkoutToBranch(branchName: String) {
        val branch = BranchIndex.getBranch(createBranchName)
            ?: throw BranchNotFoundException("No branch found with name: $branchName")

        val head = BranchIndex.getBranchHead(branch.generateKey())

        val checkoutHandler = CheckoutHandler(head, sudoArgs)
        checkoutHandler.preCheckout()
        checkoutHandler.checkout()
    }
}