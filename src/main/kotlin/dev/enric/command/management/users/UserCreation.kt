package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.management.users.UserCreationHandler
import dev.enric.logger.Logger
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.Console

@Command(
    name = "user-create",
    description = ["Creates a new user"],
    mixinStandardHelpOptions = true,
)
class UserCreation : TrackitCommand() {

    @Option(names = ["--name", "-n"], description = ["User name."], required = true)
    var name: String = ""

    @Option(names = ["--password", "-p"], description = ["User password."], interactive = true)
    var password: String? = null

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

    override fun call(): Int {
        super.call()

        password = assignInteractivePassword() ?: return 1

        val handler = UserCreationHandler(
            name,
            password!!,
            mail,
            phone,
            roles,
            sudoArgs
        )

        // Will return 1 as it will throw an exception if the user cannot be created
        if (!handler.checkCanCreateUser()) {
            return 1
        }

        handler.createUser()

        return 0
    }

    fun assignInteractivePassword() : String? {
        if (password.isNullOrBlank()) {
            val console: Console? = System.console()

            if (console != null) {
                return String(console.readPassword("Enter password: "))
            } else {
                Logger.error("Password is required but cannot be read in this environment")
                return null
            }
        }

        return null
    }
}