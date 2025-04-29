package dev.enric.cli.branch

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.branch.MergeHandler
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.util.index.BranchIndex
import picocli.CommandLine.*

/**
 * The MergeCommand class handles the merging of one branch into the current branch.
 * It allows users to merge changes from a specified branch into the currently active branch.
 * Users can also force the merge even if files are not up to date.
 *
 * Usage examples:
 * - Merge a branch into the current branch:
 *   trackit branch merge <merge-branch>
 *
 * - Force a merge even if files are not up to date:
 *   trackit branch merge <merge-branch> --force
 */
@Command(
    name = "merge",
    mixinStandardHelpOptions = true,
    description = ["Merges the specified branch into the current branch."],
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit branch merge <merge-branch>",
        "    Merges the specified branch into the current branch.",
        "",
        "  trackit branch merge <merge-branch> --force",
        "    Forces the merge even if the files are not up to date."
    ]
)
class MergeCommand : TrackitCommand() {

    /**
     * The name of the branch to merge into the current branch.
     * This branch will be merged into the active branch.
     */
    @Parameters(index = "0", paramLabel = "Merge Branch", description = ["The branch name to merge into the current branch."])
    var mergeBranch: String = ""

    /**
     * A flag indicating whether to force the merge even if files are not up to date.
     * If true, the merge will proceed even if there are conflicts or outdated files.
     */
    @Option(names = ["-f", "--force"], description = ["Force merge even if files are not up to date."])
    var force: Boolean = false

    /**
     * Executes the merge operation by checking the specified branch and merging it into the current branch.
     * If the merge branch is not found, it throws a BranchNotFoundException.
     * If the force option is provided, the merge will proceed even if there are outdated files.
     *
     * @return an exit code indicating the success or failure of the operation (0 for success, 1 for failure).
     */
    override fun call(): Int {
        super.call()

        // Retrieve the current branch
        val currentBranch = BranchIndex.getCurrentBranch()

        // Retrieve the branch to merge
        val mergeBranch = BranchIndex.getBranch(mergeBranch)
            ?: throw BranchNotFoundException("Branch '$mergeBranch' not found.")

        // Initialize the merge handler
        val mergeHandler = MergeHandler(currentBranch, mergeBranch, sudoArgs, force)

        // Check if merge is possible (permissions, conflicts, etc.)
        if (!mergeHandler.canMerge()) {
            return 1
        }

        // Perform the merge
        mergeHandler.doMerge()

        return 0
    }
}
