package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.commit.CheckoutHandler
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*

/**
 * Command to change the repository state to a specific commit.
 *
 * This command allows switching the working directory and index to match a given commit hash.
 * Supports abbreviated hashes and includes error handling for ambiguous or missing hashes.
 *
 * Usage example:
 *   trackit checkout a1b2c3d
 */
@Command(
    name = "checkout",
    description = ["Changes repository state to a specified commit or branch. Allows switching to a commit identified by hash."],
    mixinStandardHelpOptions = true,
    footer = [
        "Examples:",
        "  trackit checkout a1b2c3d           # Checkout to a commit using an abbreviated hash",
        "  trackit checkout 1234567890abcdef  # Checkout to a commit using a full hash",
        "  trackit checkout master             # Checkout to a branch named 'master'",
        "",
        "Notes:",
        "  - The commit hash can be abbreviated or full.",
        "  - If the hash is ambiguous, an error will be thrown.",
        ""
    ]
)
class Checkout : TrackitCommand() {

    /**
     * The hash of the commit to checkout.
     * Supports full and abbreviated hashes.
     */
    @Parameters(index = "0", paramLabel = "Hash", description = ["The hash of the commit"])
    lateinit var commitHash: String

    /**
     * Executes the checkout process.
     *
     * 1. Resolves the commit hash (abbreviated or full).
     * 2. Verifies commit uniqueness.
     * 3. Delegates pre-checkout validation and actual checkout to the handler.
     *
     * @return Exit code: 0 if successful, non-zero if an error occurs.
     */
    override fun call(): Int {
        super.call()

        val checkoutHandler = CheckoutHandler(getCommitByHash(), sudoArgs)

        // Will never return 1 because the checkCanCreateRole method will throw an exception if the role can't be created
        if (!checkoutHandler.canDoCheckout()) {
            return 1
        }

        checkoutHandler.preCheckout()
        checkoutHandler.checkout()

        return 0
    }

    /**
     * Resolves the user-provided hash into a unique Commit object.
     *
     * - If the hash is abbreviated, attempts to expand it via CommitIndex.
     * - Validates that the resolved hash is unique and corresponds to an existing commit.
     *
     * @throws IllegalArgumentValueException if no matching commit is found or if multiple matches exist.
     * @return The resolved Commit instance.
     */
    private fun getCommitByHash(): Commit {
        val hashes = if (Hash.isAbbreviatedHash(commitHash)) {
            CommitIndex.getAbbreviatedCommit(commitHash)
        } else {
            listOf(Hash(commitHash))
        }

        when {
            hashes.size > 1 -> throw IllegalArgumentValueException("There are many Commits starting with $commitHash")
            hashes.isEmpty() -> throw IllegalArgumentValueException("There are no Commits starting with $commitHash")
        }

        return Commit.newInstance(hashes.first())
    }
}