package dev.enric

import dev.enric.command.TrackitCommand
import dev.enric.command.administration.Config
import dev.enric.command.commit.Commit
import dev.enric.command.repository.Ignore
import dev.enric.command.repository.Init
import dev.enric.command.staging.Stage
import dev.enric.command.staging.Unstage
import dev.enric.command.users.UserCreation
import dev.enric.command.users.UserList
import dev.enric.command.users.UserModify
import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

@Command(
    name = "trackit",
    mixinStandardHelpOptions = true,
    version = ["Trackit 1.0"],
    description = ["Track your files"],
    subcommands = [Init::class, Stage::class, Unstage::class, Ignore::class, Commit::class, Config::class, UserCreation::class, UserModify::class, UserList::class]
)
class Main : TrackitCommand() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val cmd = CommandLine(Main())

            cmd.isCaseInsensitiveEnumValuesAllowed = true
            cmd.isStopAtPositional = false

            cmd.setExecutionExceptionHandler { ex, _, _ ->
                System.err.println("Error: ${ex.message}")
                return@setExecutionExceptionHandler 1
            }

            val exitCode = cmd.execute(*args)

            exitProcess(exitCode)
        }
    }


    override fun call(): Int {
        CommandLine.usage(this, System.out)

        return 0
    }
}