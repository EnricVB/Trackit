package dev.enric

import dev.enric.command.repository.Ignore
import dev.enric.command.repository.Init
import dev.enric.command.staging.Stage
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "trackit",
    mixinStandardHelpOptions = true,
    version = ["trackit 1.0"],
    description = ["Track your files"],
    subcommands = [Init::class, Stage::class, Ignore::class]
)
class Main : Callable<Int> {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Main()).execute(*args)
            exitProcess(exitCode)
        }
    }

    override fun call(): Int {
        CommandLine.usage(this, System.out)

        return 0
    }
}