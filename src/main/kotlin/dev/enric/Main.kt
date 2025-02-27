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
    subcommands = [Init::class, Stage::class, Unstage::class, Ignore::class, Commit::class, Config::class, UserCreation::class, UserModify::class, UserList::class]
)
class Main : TrackitCommand() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            AnsiConsole.systemInstall()

            val cmd = CommandLine(Main())

            cmd.setColorScheme(ColorScheme.Builder()
                .errors(Ansi.Style.bold, Ansi.Style.fg_red)
                .commands(Ansi.Style.bold, Ansi.Style.fg_green)
                .options(Ansi.Style.bold, Ansi.Style.fg_cyan)
                .parameters(Ansi.Style.bold, Ansi.Style.fg_blue)
                .stackTraces(Ansi.Style.bold, Ansi.Style.fg_red)
                .build())

            cmd.isCaseInsensitiveEnumValuesAllowed = true
            cmd.isStopAtPositional = false

            cmd.setExecutionExceptionHandler { ex, _, _ ->
                System.err.println("Error: ${ex.message}")
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