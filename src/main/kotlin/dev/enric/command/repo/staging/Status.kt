package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.staging.StatusHandler
import picocli.CommandLine.Command

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