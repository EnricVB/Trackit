package dev.enric.core.commandconsumer

import picocli.CommandLine
import picocli.CommandLine.IParameterConsumer
import picocli.CommandLine.ParameterException
import java.io.Console
import java.util.*

/**
 * Custom parameter consumer for the --sudo option
 *
 * This consumer will read the username and password from the console if they are not provided as arguments
 *
 * @see IParameterConsumer
 */
class SudoArgsParameterConsumer : IParameterConsumer {

    /**
     * Consume the parameters for the --sudo option
     *
     * @param args the stack of arguments
     * @param option the option to consume
     * @param commandSpec the command spec
     */
    override fun consumeParameters(
        args: Stack<String>?,
        option: CommandLine.Model.ArgSpec?,
        commandSpec: CommandLine.Model.CommandSpec?
    ) {
        // In case of missing parameters, throw an exception
        if (args == null || option == null || commandSpec == null) {
            throw ParameterException(
                commandSpec?.commandLine(),
                "There was an error while reading the parameters, two parameters are required"
            )
        }

        val values = mutableListOf<String>()
        var username = if (args.isNotEmpty()) args.pop() else ""
        var password = if (args.isNotEmpty()) args.pop() else ""

        // If the username is not provided, read it from the console
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

        // If the password is not provided, read it from the console
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

        // Set the values for the option
        option.setValue(values.toTypedArray())
    }
}