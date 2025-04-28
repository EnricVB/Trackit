package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.commit.CheckoutHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*

@Command(
    name = "reset",
    description = ["Reset the current branch to a specific commit. Depending on the option, it can be a hard, mix or soft reset."],
    mixinStandardHelpOptions = true,
    footer = [
        "Hard reset: Discards all changes in the working directory and index. The branch is moved to the specified commit.",
        "Mixed reset: Discards all changes in the index but keeps the working directory intact. The branch is moved to the specified commit.",
        "Soft reset: Keeps all changes in the working directory and index. The branch is moved to the specified commit.",
        "Note: The reset command is a powerful operation that can result in data loss. Use with caution."
    ]
)
class Reset : TrackitCommand() {

    @Option(
        names = ["--soft"],
        description = ["Keep all changes in the working directory and index. The branch is moved to the specified commit."],
    )
    var soft: Boolean = false

    @Option(
        names = ["--mixed"],
        description = ["Discard all changes in the index but keep the working directory intact. The branch is moved to the specified commit."],
    )
    var mixed: Boolean = true

    @Option(
        names = ["--hard"],
        description = ["Discard all changes in the working directory and index. The branch is moved to the specified commit. Similar to checkout"],
    )
    var hard: Boolean = false

    @Option(
        names = ["--commit"],
        description = ["The commit to reset to."],
        required = true,
    )
    var commitString: String = ""

    override fun call(): Int {
        super.call()
        val commitHash = CommitIndex.getAbbreviatedCommit(commitString)[0]

        if (soft && mixed || soft && hard || mixed && hard) {
            throw IllegalArgumentValueException("You can only use one of --soft, --mixed or --hard at a time.")
        }

        // Reset the current branch to the specified commit depending on the options
        if (soft) {
            softReset(commitHash)
        } else if (mixed) {
            mixedReset(commitHash)
        } else if (hard) {
            hardReset(commitHash)
        }

        return 0
    }

    /**
     * Resets the current branch to the specified commit.
     */
    private fun softReset(commitHash : Hash) {
        CommitIndex.setCurrentCommit(commitHash)
    }

    /**
     * Resets the current branch to the specified commit and clears the staging area.
     */
    private fun mixedReset(commitHash : Hash) {
        CommitIndex.setCurrentCommit(commitHash)
        StagingHandler.clearStagingArea()
    }

    /**
     * Resets the current branch to the specified commit and discards all changes in the working directory and index.
     */
    private fun hardReset(commitHash : Hash) {
        val checkoutHandler = CheckoutHandler(Commit.newInstance(commitHash), sudoArgs)

        // Will never return 1 because the checkCanCreateRole method will throw an exception if the role can't be created
        if (!checkoutHandler.canDoCheckout()) {
            return
        }

        checkoutHandler.preCheckout()
        checkoutHandler.checkout()
    }
}