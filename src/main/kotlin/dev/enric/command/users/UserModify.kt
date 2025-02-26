package dev.enric.command.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.users.UserModifyHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "user-modify",
    description = ["Modifies an existing user"]
)
class UserModify : TrackitCommand() {
    @Option(names = ["--name", "-n"], description = ["Name of the user to modify"])
    lateinit var name: String

    @Option(names = ["--password", "-p"], description = ["Password of the user to modify"])
    lateinit var password: String

    @Option(names = ["--new-password", "-N"], description = ["New password for the user"])
    var newPassword: String? = null

    @Option(names = ["--new-mail", "-M"], description = ["New mail for the user"])
    var newMail: String? = null


    @Option(names = ["--new-phone", "-P"], description = ["New phone for the user"])
    var newPhone: String? = null

    @Option(
        names = ["--role", "-r"],
        description = ["Roles to assign to the user"],
        split = " "
    )
    var roles: Array<String> = emptyArray()

    @Option(
        names = ["--delete-previous-roles", "-d"],
        description = ["Delete previous roles"]
    )
    var deletePreviousRoles: Boolean = false

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        arity = "2"
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

        if (!handler.checkCanModifyUser()) {
            return 1
        }

        handler.modifyUser()

        return 0
    }
}