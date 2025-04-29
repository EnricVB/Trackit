package dev.enric.command.branch

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.branch.BranchHandler
import picocli.CommandLine.Command

@Command(
    name = "list-branch",
    mixinStandardHelpOptions = true,
)
class BranchList : TrackitCommand() {

    override fun call(): Int {
        super.call()

        BranchHandler().listBranches()

        return 1
    }
}