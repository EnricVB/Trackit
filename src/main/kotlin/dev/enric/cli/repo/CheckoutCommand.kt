package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.CheckoutHandler
import dev.enric.domain.Hash.HashType.BRANCH
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.BranchIndex
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
        "  - If checkout to a previous commit, the history will remain.",
        "    F.e. Checkout from v1.0.1 to v1.0.0, History will look like: ",
        "    'v1.0.0 -> v1.0.1 -> v1.0.0 -> ...",
        ""
    ]
)
class CheckoutCommand : TrackitCommand() {

    /**
     * The hash of the commit to checkout.
     * Supports full and abbreviated hashes.
     */
    @Parameters(index = "0", paramLabel = "Hash", description = ["The hash of the commit"])
    lateinit var checkoutDirection: String

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

        val commitToCheckout = getCommitByBranchName() ?: getCommitByHash()
        val checkoutHandler = CheckoutHandler(commitToCheckout, sudoArgs)

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
        val hashes = if (Hash.isAbbreviatedHash(checkoutDirection)) {
            CommitIndex.getAbbreviatedCommit(checkoutDirection)
        } else {
            listOf(Hash(checkoutDirection))
        }

        // Check if abbreviation matches more than 1 commit
        when {
            hashes.size > 1 -> throw IllegalArgumentValueException("There are many Commits starting with $checkoutDirection")
            hashes.isEmpty() -> throw IllegalArgumentValueException("There are no Commits starting with $checkoutDirection")
        }

        // If it is a branch hash, must get the BranchHead
        val isBranch = hashes.first().string.startsWith(BRANCH.hash.string)

        return if (isBranch) {
            BranchIndex.getBranchHead(hashes.first())
        } else {
            Commit.newInstance(hashes.first())
        }
    }

    /**
     * Resolves the user-provided branch name into his BranchHead Commit.
     *
     * @throws IllegalArgumentValueException if no matching commit is found or if multiple matches exist.
     * @return The resolved Commit instance.
     */
    private fun getCommitByBranchName(): Commit? {
        val branches = BranchIndex.getAllBranches().map { Branch.newInstance(it).name }

        if (checkoutDirection in branches) {
            val branchHash = BranchIndex.getBranch(checkoutDirection)!!.generateKey()

            return BranchIndex.getBranchHead(branchHash)
        }

        return null
    }
}