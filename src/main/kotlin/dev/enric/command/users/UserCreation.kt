package dev.enric.command.users

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.handler.users.UserCreationHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "user-create",
    description = ["Creates a new user"],
    mixinStandardHelpOptions = true,
)
class UserCreation : TrackitCommand() {

    @Option(names = ["--name", "-n"], description = ["User name."], required = true)
    var name: String = ""

    @Option(names = ["--password", "-p"], description = ["User password."], required = true)
    var password: String = ""

    @Option(names = ["--mail", "-m"], description = ["User mail contact"], required = false)
    var mail: String = ""

    @Option(names = ["--phone", "-P"], description = ["User phone contact"], required = false)
    var phone: String = ""

    @Option(
        names = ["--role", "-r"],
        description = ["Assign roles"],
        split = " "
    )
    var roles: Array<String> = emptyArray()

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        val handler = UserCreationHandler(
            name,
            password,
            mail,
            phone,
            roles,
            sudoArgs
        )

        if (!handler.checkCanCreateUser()) {
            return 1
        }

        handler.createUser()

        return 0
    }
}