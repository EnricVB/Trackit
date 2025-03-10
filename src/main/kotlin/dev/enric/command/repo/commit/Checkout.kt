package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.command.repo.staging.Stage
import dev.enric.core.Hash
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.repo.commit.CheckoutHandler
import dev.enric.core.repo.commit.CommitHandler
import dev.enric.core.repo.staging.StagingHandler
import dev.enric.domain.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.logger.Logger
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*
import java.sql.Timestamp
import java.time.Instant

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
        checkoutHandler.checkout()

        return 0
    }

    private fun getCommitByHash() : Commit {
        val hashes = if (CommitIndex.isAbbreviatedHash(commitHash)) {
            CommitIndex.getAbbreviatedCommit(commitHash)
        } else {
            listOf(Hash(commitHash))
        }

        if (hashes.size > 1) {
            throw IllegalArgumentValueException("There are many Commits starting with $commitHash")
        } else if(hashes.isEmpty()) {
            throw IllegalArgumentValueException("There are no Commits starting with $commitHash")
        }

        return Commit.newInstance(hashes.first())
    }
}