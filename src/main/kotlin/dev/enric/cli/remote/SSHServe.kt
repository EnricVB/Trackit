package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.CheckoutHandler
import dev.enric.domain.Hash.HashType.BRANCH
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.remote.tcp.RemoteReceiver
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*

@Command(
    name = "ssh-serve",
    description = ["Starts a TCP Connection for granting access to comands like push or pull."],
    mixinStandardHelpOptions = false,
)
class SSHServe : TrackitCommand() {

    override fun call(): Int {
        super.call()

        RemoteReceiver(
            port = 8088,
            authorizedKeys = listOf(), //TODO: Add authorized keys
        ).start()

        return 0
    }
}