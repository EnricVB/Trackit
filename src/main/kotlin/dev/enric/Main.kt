package dev.enric

import dev.enric.cli.TrackitCommand
import dev.enric.cli.admin.*
import dev.enric.cli.branch.BranchCommand
import dev.enric.cli.branch.BranchListCommand
import dev.enric.cli.branch.MergeCommand
import dev.enric.cli.management.RoleCreationCommand
import dev.enric.cli.management.RoleListCommand
import dev.enric.cli.management.RoleModifyCommand
import dev.enric.cli.management.UserCreationCommand
import dev.enric.cli.management.UserListCommand
import dev.enric.cli.management.UserModifyCommand
import dev.enric.cli.management.grantPermission.*
import dev.enric.cli.remote.TCPServe
import dev.enric.cli.remote.Push
import dev.enric.cli.repo.*
import dev.enric.exceptions.TrackitException
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import org.fusesource.jansi.AnsiConsole
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import kotlin.system.exitProcess
import picocli.CommandLine.Help.ColorScheme
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@Command(
    name = "trackit",
    mixinStandardHelpOptions = true,
    version = ["Trackit 1.2.14-BETA"],
    description = ["Track your files"],
    subcommands = [
        InitCommand::class, ConfigCommand::class, IgnoreCommand::class,

        StageCommand::class, UnstageCommand::class, StatusCommand::class,
        CommitCommand::class, LogCommand::class, DiffCommand::class, BlameCommand::class,
        CheckoutCommand::class, RestoreCommand::class, ResetCommand::class,
        MergeCommand::class,

        BranchCommand::class, BranchListCommand::class,
        BranchPermissionGrantCommand::class, BranchPermissionRevokeCommand::class,

        TagAssignCommand::class, TagCreationCommand::class, TagListCommand::class, TagRemoveCommand::class,

        UserCreationCommand::class, UserModifyCommand::class, UserListCommand::class,

        RoleCreationCommand::class, RoleModifyCommand::class, RoleListCommand::class,
        RoleGrantCommand::class, RoleRevokeCommand::class,
        RolePermissionGrantCommand::class, RolePermissionRevokeCommand::class,

        GarbageRecolectorCommand::class, CheckIntegrityCommand::class,

        TCPServe::class, Push::class
    ]
)
class Main : TrackitCommand() {



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val isHelp = args.any { it == "--help" || it == "-h" } && args.size == 1
            val isInit = args.any { it == "init" } && args.size == 1
            val isClone = args.any { it == "clone" }

            // Checks if the current folder is a trackit repository
            if (!RepositoryFolderManager().isRepositoryFolder()) {
                if(isInit || isClone || isHelp) {
                    CommandLine(Main()).execute(*args)
                    exitProcess(0)
                } else {
                    System.err.println("Error: This is not a trackit repository.")
                    exitProcess(1)
                }
            }

            // Installs the ANSI console
            System.setProperty("picocli.ansi", "true")
            AnsiConsole.systemInstall()

            Logger.trace("trackit ${args.joinToString()}")

            val exitCode = try {
                val cmd = CommandLine(Main()).apply {
                    colorScheme = ColorScheme.Builder()
                        .errors(Ansi.Style.bold, Ansi.Style.fg_red)
                        .commands(Ansi.Style.bold, Ansi.Style.fg_green)
                        .options(Ansi.Style.bold, Ansi.Style.fg_cyan)
                        .parameters(Ansi.Style.bold, Ansi.Style.fg_blue)
                        .optionParams(Ansi.Style.italic, Ansi.Style.fg_white)
                        .stackTraces(Ansi.Style.bold, Ansi.Style.fg_red)
                        .build()

                    isCaseInsensitiveEnumValuesAllowed = true
                    isStopAtPositional = false

                    setExecutionExceptionHandler { ex, _, _ ->
                        handleException(ex)
                        1
                    }
                }

                cmd.execute(*args)
            } catch (ex: Exception) {
                handleException(ex)
                1
            } finally {
                AnsiConsole.systemUninstall()
            }

            exitProcess(exitCode)
        }

        private fun handleException(ex: Throwable) {
            if (ex is TrackitException) {
                Logger.error(ex.message ?: "Trackit error")
            } else {
                Logger.error("An internal error occurred.")
                Logger.trace(ex.stackTraceToString())
            }
        }
    }


    override fun call(): Int {
        CommandLine.usage(this, System.out)

        return 0
    }
}