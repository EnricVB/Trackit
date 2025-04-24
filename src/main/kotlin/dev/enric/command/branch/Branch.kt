package dev.enric.command.branch

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.branch.BranchHandler
import dev.enric.core.handler.repo.commit.CheckoutHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
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
    var removeBranch: String = ""

    @Option(
        names = ["-c", "--create"], description = ["Will create a new branch with the given name."]
    )
    var createBranch: String = ""

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
        if (removeBranch.isNotBlank()) {
            val branch = BranchIndex.getBranch(removeBranch)
            BranchHandler(branch, sudoArgs).removeBranch()

            return 0
        }


        // In case createBranch is true
        if (createBranch.isNotBlank()) {
            BranchHandler(currentBranch, sudoArgs).createBranch(createBranch)

            // In case checkout is true, will call CheckoutHandler to move to branch head.
            if (checkoutBranch) {
                val branch = BranchIndex.getBranch(createBranch)
                val head = branch?.let { BranchIndex.getBranchHead(it.generateKey()) }

                if (head == null) return 1

                // Checkout to new branch
                val checkoutHandler = CheckoutHandler(head, sudoArgs)
                checkoutHandler.preCheckout()
                checkoutHandler.checkout()
            }

            return 0
        }

        return 1
    }
}