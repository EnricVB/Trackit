package dev.enric

import dev.enric.command.TrackitCommand
import dev.enric.command.administration.*
import dev.enric.command.branch.Branch
import dev.enric.command.branch.Merge
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
import dev.enric.command.repo.commit.Reset
import dev.enric.command.repo.commit.Restore
import dev.enric.command.repo.staging.Status
import dev.enric.command.repo.tag.TagAssign
import dev.enric.command.repo.tag.TagCreation
import dev.enric.command.repo.tag.TagList
import dev.enric.command.repo.tag.TagRemove
import dev.enric.exceptions.TrackitException
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import org.fusesource.jansi.AnsiConsole
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import kotlin.system.exitProcess
import picocli.CommandLine.Help.ColorScheme

/*
TODO:
    Comandos que debería agregar y no son muy dificiles:
    - BranchDelete          -> Elimina una rama
    - BranchList            -> Lista las ramas
    - PermissionGrant       -> Agrega permisos a un usuario
    - PermissionRevoke      -> Revoca permisos a un usuario
    - RolePermissionAssign  -> Agrega permisos a un rol
    - RolePermissionRemove  -> Revoca permisos a un rol
    - CheckIntegrity        -> Verifica la integridad de la base de datos
*/

@Command(
    name = "trackit",
    mixinStandardHelpOptions = true,
    version = ["Trackit 1.0.0-BETA"],
    description = ["Track your files"],
    subcommands = [
        Init::class, Stage::class, Unstage::class, Status::class,
        Ignore::class, Config::class, Log::class, Diff::class, Blame::class, GarbageRecolector::class,
        Commit::class, Checkout::class, Restore::class, Reset::class,
        UserCreation::class, UserModify::class, UserList::class,
        RoleCreation::class, RoleModify::class, RoleList::class,
        TagAssign::class, TagCreation::class, TagList::class, TagRemove::class,
        Branch::class, Merge::class,
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