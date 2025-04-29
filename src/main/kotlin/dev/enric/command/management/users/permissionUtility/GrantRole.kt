package dev.enric.command.management.users.permissionUtility

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.permissionUtility.GrantRoleHandler
import picocli.CommandLine.*

@Command(
    name = "grant-role",
    description = ["Grant role to a user"],
    footer = [
        "",
        "Examples:",
        "  trackit grant-role <user> <roles...>",
        "  trackit grant-role --help",
        "",
    ],
    mixinStandardHelpOptions = true,
)
class GrantRole : TrackitCommand() {

    /** The username of the user to whom the role will be granted. */
    @Option(
        names = ["--user", "-u"],
        description = ["Name of the user to whom the role will be granted"],
        required = true,
    )
    var username: String = ""

    /** Roles to assign to the user. Can assign multiple roles at once. */
    @Parameters(
        index = "0",
        description = ["Roles to assign to the user"],
        split = " ",
        arity = "1..*",
    )
    var roles: Array<String> = emptyArray()

    override fun call(): Int {
        super.call()

        val handler = GrantRoleHandler(
            name = username,
            roleNames = roles,
            sudoArgs = sudoArgs,
        )

        // Will throw an exception if the user does not have permission to modify users
        if (!handler.checkCanModifyUser()) {
            return 1
        }

        // Will throw an exception if the user does not exist
        handler.addRoles()

        return 0
    }
}