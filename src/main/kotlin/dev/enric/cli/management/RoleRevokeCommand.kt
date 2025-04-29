package dev.enric.cli.management

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.RoleGrantHandler
import picocli.CommandLine.*

/**
 * The RoleRevokeCommand class is responsible for revoking one or more roles from a specific user.
 * It allows an administrator to remove roles from a user within the Trackit system.
 *
 * Usage examples:
 * - Revoke roles from a user:
 *   trackit revoke-role <user> <roles...>
 *
 * - Display help for the command:
 *   trackit revoke-role --help
 */
@Command(
    name = "revoke-role",
    description = ["Revoke role to a user"],
    footer = [
        "",
        "Examples:",
        "  trackit revoke-role <user> <roles...>",
        "    Revokes the specified roles from the given user.",
        "",
        "  trackit revoke-role --help",
        "    Displays help information for this command."
    ],
    mixinStandardHelpOptions = true,
)
class RoleRevokeCommand : TrackitCommand() {

    /**
     * The username of the user to whom the role(s) will be revoked.
     * This option is required for the command to execute.
     */
    @Option(
        names = ["--user", "-u"],
        description = ["Name of the user to whom the role will be revoked"],
        required = true,
    )
    var username: String = ""

    /**
     * The roles to revoke from the user.
     * This parameter accepts one or more role names.
     */
    @Parameters(
        index = "0",
        description = ["Roles to revoke from the user"],
        split = " ",
        arity = "1..*",
    )
    var roles: Array<String> = emptyArray()

    /**
     * Executes the command to revoke roles from the specified user.
     * The command checks if the current user has permission to modify the roles of others,
     * and if the user exists, then revokes the specified roles.
     *
     * @return an exit code indicating the success (0) or failure (1) of the operation.
     */
    override fun call(): Int {
        super.call()

        // Create an instance of RoleGrantHandler with the necessary arguments
        val handler = RoleGrantHandler(
            name = username,
            roleNames = roles,
            sudoArgs = sudoArgs,
        )

        // Check if the user has permission to modify user roles
        if (!handler.checkCanModifyUser()) {
            return 1 // Permission denied
        }

        // Revoke the roles from the user
        handler.removeRoles()

        return 0
    }
}
