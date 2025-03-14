package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.repo.staging.StatusHandler
import dev.enric.util.repository.RepositoryFolderManager
import picocli.CommandLine.Command
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

@Command(
    name = "status",
    description = ["Show the status of the working directory"],
    mixinStandardHelpOptions = true,
)
class Status : TrackitCommand() {

    override fun call(): Int {
        super.call()

        StatusHandler.printStatus()

        return 0
    }
}