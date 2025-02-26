package dev.enric.core.commandconsumer

import picocli.CommandLine
import picocli.CommandLine.IParameterConsumer
import picocli.CommandLine.ParameterException
import java.io.Console
import java.util.Stack

class SudoArgsParameterConsumer : IParameterConsumer {
    override fun consumeParameters(
        args: Stack<String>?,
        option: CommandLine.Model.ArgSpec?,
        commandSpec: CommandLine.Model.CommandSpec?
    ) {
        if (args == null || option == null || commandSpec == null) {
            throw ParameterException(commandSpec?.commandLine(), "There was an error while reading the parameters, two parameters are required")
        }

        val values = mutableListOf<String>()

        if (args.isNotEmpty()) {
            values.add(args.pop())
        } else {
            throw ParameterException(
                commandSpec.commandLine(),
                "Missing user parameter for --sudo"
            )
        }

        if (args.isNotEmpty()) {
            values.add(args.pop())
        } else {
            val console: Console? = System.console()
            if (console != null) {
                val password = String(console.readPassword("Enter password: "))
                values.add(password)
            } else {
                throw ParameterException(
                    commandSpec.commandLine(),
                    "Missing password parameter for --sudo (no console available)"
                )
            }
        }

        option.setValue(values.toTypedArray())
    }
}