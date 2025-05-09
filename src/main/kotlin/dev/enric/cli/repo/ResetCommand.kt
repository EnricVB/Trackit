package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.CheckoutHandler
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*
import kotlin.io.path.ExperimentalPathApi

/**
 * The ResetCommand class is responsible for resetting the current branch to a specific commit.
 * Depending on the chosen option, it can perform a hard, mixed, or soft reset.
 *
 * A reset operation is a powerful action that can discard or preserve changes in the working directory and index.
 * - Hard reset: Discards all changes and moves the branch to the specified commit.
 * - Mixed reset: Discards changes in the index but keeps the working directory intact.
 * - Soft reset: Keeps all changes in the working directory and index but moves the branch to the specified commit.
 *
 * Use this command with caution, as it may result in data loss.
 */
@Command(
    name = "reset",
    description = ["Reset the current branch to a specific commit."],
    mixinStandardHelpOptions = true,
    usageHelpWidth = 500,
    footer = [
        "Hard reset: Discards all changes in the working directory and index. The branch is moved to the specified commit.",
        "Mixed reset: Discards all changes in the index but keeps the working directory intact. The branch is moved to the specified commit.",
        "Soft reset: Keeps all changes in the working directory and index. The branch is moved to the specified commit.",
        "Note: The reset command is a powerful operation that can result in data loss. Use with caution."
    ]
)
class ResetCommand : TrackitCommand() {

    /**
     * Flag to perform a soft reset, which keeps all changes in the working directory and index.
     */
    @Option(
        names = ["--soft"],
        description = ["Keep all changes in the working directory and index. The branch is moved to the specified commit."],
    )
    var soft: Boolean = false

    /**
     * Flag to perform a mixed reset, which discards all changes in the index but keeps the working directory intact.
     */
    @Option(
        names = ["--mixed"],
        description = ["Discard all changes in the index but keep the working directory intact. The branch is moved to the specified commit."],
    )
    var mixed: Boolean = true

    /**
     * Flag to perform a hard reset, which discards all changes in the working directory and index.
     * The branch is moved to the specified commit, similar to a checkout.
     */
    @Option(
        names = ["--hard"],
        description = ["Discard all changes in the working directory and index. The branch is moved to the specified commit. Similar to checkout"],
    )
    var hard: Boolean = false

    /**
     * The commit hash to which the branch will be reset. This is a required parameter.
     */
    @Option(
        names = ["--commit"],
        description = ["The commit to reset to."],
        required = false,
    )
    var commitString: String = CommitIndex.getCurrentCommit()?.generateKey()?.string ?: ""

    /**
     * Executes the reset command. Depending on the options selected, it will perform a soft, mixed, or hard reset.
     * The command validates that only one reset type is selected at a time, and if so, it performs the appropriate reset.
     *
     * @return an exit code indicating the success (0) or failure (1) of the operation.
     */
    override fun call(): Int {
        super.call()
        val commitHashes = CommitIndex.getAbbreviatedCommit(commitString)
        if (commitHashes.isEmpty()) {
            throw IllegalArgumentValueException("You must specify a valid commit hash.")
        }

        val commitHash = commitHashes.first()

        // Validate that only one reset type is used at a time
        if (soft && mixed || soft && hard || mixed && hard) {
            throw IllegalArgumentValueException("You can only use one of --soft, --mixed or --hard at a time.")
        }

        // Reset the current branch to the specified commit depending on the options
        when {
            soft -> softReset(commitHash)
            mixed -> mixedReset(commitHash)
            hard -> hardReset(commitHash)
            else -> throw IllegalArgumentValueException("You must specify one of --soft, --mixed or --hard.")
        }

        return 0
    }

    /**
     * Performs a soft reset, moving the branch to the specified commit while preserving changes in the working directory and index.
     *
     * @param commitHash The hash of the commit to reset to.
     */
    private fun softReset(commitHash : Hash) {
        CommitIndex.setCurrentCommit(commitHash)
    }

    /**
     * Performs a mixed reset, moving the branch to the specified commit and clearing the staging area.
     *
     * @param commitHash The hash of the commit to reset to.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun mixedReset(commitHash : Hash) {
        CommitIndex.setCurrentCommit(commitHash)
        StagingHandler.clearStagingArea()
    }

    /**
     * Performs a hard reset, moving the branch to the specified commit and discarding all changes in the working directory and index.
     *
     * @param commitHash The hash of the commit to reset to.
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
