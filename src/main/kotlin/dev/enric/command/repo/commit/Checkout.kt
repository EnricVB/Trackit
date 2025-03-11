package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.core.Hash
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.repo.commit.CheckoutHandler
import dev.enric.domain.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import picocli.CommandLine.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

@Command(
    name = "checkout",
    description = ["Changes repository state to a specified commit or branch"],
    mixinStandardHelpOptions = true,
)
class Checkout : TrackitCommand() {

    /**
     * Title of the commit to be created, it should be a short description of the changes
     */
    @Parameters(index = "0", paramLabel = "Hash", description = ["The hash of the commit"])
    lateinit var commitHash: String

    /**
     * Sudo args in case user did not kept session logged in.
     */
    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        // TODO: Check if has permissions to read this branch
        val checkoutHandler = CheckoutHandler(getCommitByHash(), sudoArgs)

        checkoutHandler.preCheckout()
        checkoutHandler.checkout()

        return 0
    }

    private fun getCommitByHash(): Commit {
        val hashes = if (CommitIndex.isAbbreviatedHash(commitHash)) {
            CommitIndex.getAbbreviatedCommit(commitHash)
        } else {
            listOf(Hash(commitHash))
        }

        if (hashes.size > 1) {
            throw IllegalArgumentValueException("There are many Commits starting with $commitHash")
        } else if (hashes.isEmpty()) {
            throw IllegalArgumentValueException("There are no Commits starting with $commitHash")
        }

        return Commit.newInstance(hashes.first())
    }
}