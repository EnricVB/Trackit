package dev.enric

import dev.enric.command.TrackitCommand
import dev.enric.command.administration.Config
import dev.enric.command.administration.Log
import dev.enric.command.management.roles.RoleCreation
import dev.enric.command.management.roles.RoleList
import dev.enric.command.management.roles.RoleModify
import dev.enric.command.repo.commit.Commit
import dev.enric.command.repo.repository.Ignore
import dev.enric.command.repo.repository.Init
import dev.enric.command.repo.staging.Stage
import dev.enric.command.repo.staging.Unstage
import dev.enric.command.management.users.UserCreation
import dev.enric.command.management.users.UserList
import dev.enric.command.management.users.UserModify
import dev.enric.command.repo.commit.Checkout
import dev.enric.command.repo.staging.Status
import dev.enric.exceptions.TrackitException
import dev.enric.logger.Logger
import org.fusesource.jansi.AnsiConsole
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import kotlin.system.exitProcess
import picocli.CommandLine.Help.ColorScheme



@Command(
    name = "trackit",
    mixinStandardHelpOptions = true,
    version = ["Trackit 1.0"],
    description = ["Track your files"],
    subcommands = [Init::class,
        Stage::class, Unstage::class, Status::class,
        Ignore::class, Config::class, Log::class,
        Commit::class, Checkout::class,
        UserCreation::class, UserModify::class, UserList::class,
        RoleCreation::class, RoleModify::class, RoleList::class]
)
class Main : TrackitCommand() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            AnsiConsole.systemInstall()

            // Saves the command line arguments to a log file
            Logger.trace("trackit ${args.joinToString()}")

            val cmd = CommandLine(Main())

            cmd.setColorScheme(ColorScheme.Builder()
                .errors(Ansi.Style.bold, Ansi.Style.fg_red)
                .commands(Ansi.Style.bold, Ansi.Style.fg_green)
                .options(Ansi.Style.bold, Ansi.Style.fg_cyan)
                .parameters(Ansi.Style.bold, Ansi.Style.fg_blue)
                .optionParams(Ansi.Style.italic, Ansi.Style.fg_white)
                .stackTraces(Ansi.Style.bold, Ansi.Style.fg_red)
                .build())

            cmd.isCaseInsensitiveEnumValuesAllowed = true
            cmd.isStopAtPositional = false

            cmd.setExecutionExceptionHandler { ex, _, _ ->
                if (ex !is TrackitException) {
                    Logger.error("An internal error occurred.")
                    Logger.trace(ex.stackTrace.contentToString())
                }

                return@setExecutionExceptionHandler 1
            }

            val exitCode = cmd.execute(*args)

            AnsiConsole.systemUninstall()
            exitProcess(exitCode)
        }
    }


    override fun call(): Int {
        CommandLine.usage(this, System.out)

        return 0
    }
}