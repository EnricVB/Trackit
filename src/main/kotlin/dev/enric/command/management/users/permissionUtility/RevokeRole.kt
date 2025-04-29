package dev.enric.command.management.users.permissionUtility

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.permissionUtility.GrantRoleHandler
import picocli.CommandLine.*

@Command(
    name = "revoke-role",
    description = ["Revoke role to a user"],
    footer = [
        "",
        "Examples:",
        "  trackit revoke-role <user> <roles...>",
        "  trackit revoke-role --help",
        "",
    ],
    mixinStandardHelpOptions = true,
)
class RevokeRole : TrackitCommand() {

    /** The username of the user to whom the role will be revoked. */
    @Option(
        names = ["--user", "-u"],
        description = ["Name of the user to whom the role will be revoked"],
        required = true,
    )
    var username: String = ""

    /** Roles to revoke from the user. Can revoke multiple roles at once. */
    @Parameters(
        index = "0",
        description = ["Roles to revoke from the user"],
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
        handler.removeRoles()

        return 0
    }
}