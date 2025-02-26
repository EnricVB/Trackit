package dev.enric.command.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.users.UserCreationHandler
import dev.enric.logger.Logger
import dev.enric.util.AuthUtil
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "user-create",
    description = ["Creates a new user"]
)
class UserCreation : TrackitCommand() {
    @Option(names = ["--name", "-n"], description = ["User name."])
    lateinit var name: String

    @Option(names = ["--password", "-p"], description = ["User password."])
    lateinit var password: String

    @Option(names = ["--mail", "-m"], description = ["User mail contact"])
    lateinit var mail: String

    @Option(names = ["--phone", "-P"], description = ["User phone contact"])
    lateinit var phone: String

    @Option(
        names = ["--role", "-r"],
        description = ["Assign roles"],
        split = " "
    )
    var roles: Array<String> = emptyArray()

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
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

        if(!handler.checkCanCreateUser()) {
            return 1
        }

        handler.createUser()

        return 0
    }
}