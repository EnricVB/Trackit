package dev.enric.command.branch

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.branch.MergeHandler
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.util.index.BranchIndex
import picocli.CommandLine.*

@Command(
    name = "merge",
    mixinStandardHelpOptions = true,
)
class Merge : TrackitCommand() {

    @Parameters(index = "0", paramLabel = "Merge Branch", description = ["The branch name to marge into actual branch."])
    var mergeBranch: String = ""

    @Option(names = ["-f", "--force"], description = ["Force merge even if files are not up to date."])
    var force: Boolean = false

    override fun call(): Int {
        super.call()

        val currentBranch = BranchIndex.getCurrentBranch()
        val mergeBranch = BranchIndex.getBranch(mergeBranch)
            ?: throw BranchNotFoundException("Branch '$mergeBranch' not found.")

        val mergeHandler = MergeHandler(currentBranch, mergeBranch, sudoArgs, force)

        // Will never enter this block, as in case of no permission, it will throw an exception
        if (!mergeHandler.canMerge()) {
            return 1
        }

        mergeHandler.doMerge()

        return 0
    }
}