package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.management.users.UserModifyHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "user-modify",
    description = ["Modifies an existing user"],
    mixinStandardHelpOptions = true,
)
class UserModify : TrackitCommand() {
    @Option(names = ["--name", "-n"], description = ["Name of the user to modify"], required = true)
    var name: String = ""

    @Option(
        names = ["--password", "-p"],
        description = ["Password of the user to modify"],
        interactive = true,
        required = true
    )
    var password: String = ""

    @Option(
        names = ["--new-password", "-N"],
        description = ["New password for the user"],
        interactive = true,
        required = false
    )
    var newPassword: String? = null

    @Option(names = ["--new-mail", "-M"], description = ["New mail for the user"], required = false)
    var newMail: String? = null

    @Option(names = ["--new-phone", "-P"], description = ["New phone for the user"], required = false)
    var newPhone: String? = null

    @Option(
        names = ["--role", "-r"],
        description = ["Roles to assign to the user"],
        split = " ",
        required = false
    )
    var roles: Array<String> = emptyArray()

    @Option(
        names = ["--delete-previous-roles", "-d"],
        description = ["Delete previous roles"],
        required = false
    )
    var deletePreviousRoles: Boolean = false

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        arity = "2",
        parameterConsumer = SudoArgsParameterConsumer::class,
        required = false
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        val handler = UserModifyHandler(
            name,
            password,
            newPassword,
            newMail,
            newPhone,
            roles,
            deletePreviousRoles,
            sudoArgs
        )

        // Will return 1 as it will throw an exception if the user cannot be modified
        if (!handler.checkCanModifyUser()) {
            return 1
        }

        handler.modifyUser()

        return 0
    }
}