package dev.enric.core.commandconsumer

import picocli.CommandLine
import picocli.CommandLine.IParameterConsumer
import picocli.CommandLine.ParameterException
import java.io.Console
import java.util.*

class SudoArgsParameterConsumer : IParameterConsumer {
    override fun consumeParameters(
        args: Stack<String>?,
        option: CommandLine.Model.ArgSpec?,
        commandSpec: CommandLine.Model.CommandSpec?
    ) {
        if (args == null || option == null || commandSpec == null) {
            throw ParameterException(
                commandSpec?.commandLine(),
                "There was an error while reading the parameters, two parameters are required"
            )
        }

        val values = mutableListOf<String>()
        var username = if (args.isNotEmpty()) args.pop() else ""
        var password = if (args.isNotEmpty()) args.pop() else ""

        if (username.isNotBlank()) {
            values.add(args.pop())
        } else {
            val console: Console? = System.console()
            if (console != null) {
                username = console.readLine("Enter sudo name: ")
                values.add(username)
            } else {
                throw ParameterException(
                    commandSpec.commandLine(),
                    "Missing username parameter for --sudo (no console available)"
                )
            }
        }

        if (password.isNotBlank()) {
            values.add(args.pop())
        } else {
            val console: Console? = System.console()
            if (console != null) {
                password = String(console.readPassword("Enter sudo password: "))
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