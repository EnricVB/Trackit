package dev.enric.command.branch

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.branch.BranchHandler
import dev.enric.core.handler.repo.commit.CheckoutHandler
import dev.enric.domain.objects.Branch
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "branch",
    mixinStandardHelpOptions = true,
)
class Branch : TrackitCommand() {

    @Option(
        names = ["-r", "--remove"], description = ["Will remove the named branch."]
    )
    var removeBranchName: String = ""

    @Option(
        names = ["-c", "--create"], description = ["Will create a new branch with the given name."]
    )
    var createBranchName: String = ""

    @Option(
        names = ["-C", "--checkout"], description = ["Checkout to new created branch."]
    )
    var checkoutBranch: Boolean = false

    @Option(
        names = ["--push"], description = ["Push direction.", "By default gets the current branch direction"]
    )
    var pushDirection: String = ""

    @Option(
        names = ["--fetch"], description = ["Fetch direction.", "By default gets the current branch direction"]
    )
    var fetchDirection: String = ""

    override fun call(): Int {
        super.call()
        val currentBranch = CommitIndex.getCurrentCommit()?.branch?.let { Branch.newInstance(it) }

        // In case removeBranch is true, will remove the branch from the index, not commits.
        if (removeBranchName.isNotBlank()) {
            val branch = BranchIndex.getBranch(removeBranchName)
            BranchHandler(branch, sudoArgs).removeBranch()

            return 0
        }

        // In case createBranch is true
        if (createBranchName.isNotBlank()) {
            BranchHandler(currentBranch, sudoArgs).createBranch(createBranchName)

            // In case checkout is true, will call CheckoutHandler to move to branch head.
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