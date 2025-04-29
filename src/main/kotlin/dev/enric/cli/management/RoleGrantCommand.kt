package dev.enric.cli.management

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.RoleGrantHandler
import picocli.CommandLine.*

/**
 * The RoleGrantCommand class is responsible for granting one or more roles to a specific user.
 * It allows an administrator to assign roles to a user within the Trackit system.
 *
 * Usage examples:
 * - Grant roles to a user:
 *   trackit grant-role <user> <roles...>
 *
 * - Display help for the command:
 *   trackit grant-role --help
 */
@Command(
    name = "grant-role",
    description = ["Grant role to a user"],
    footer = [
        "",
        "Examples:",
        "  trackit grant-role <user> <roles...>",
        "    Grants the specified roles to the given user.",
        "",
        "  trackit grant-role --help",
        "    Displays help information for this command."
    ],
    mixinStandardHelpOptions = true,
)
class RoleGrantCommand : TrackitCommand() {

    /**
     * The username of the user to whom the role(s) will be granted.
     * This option is required for the command to execute.
     */
    @Option(
        names = ["--user", "-u"],
        description = ["Name of the user to whom the role will be granted"],
        required = true,
    )
    var username: String = ""

    /**
     * The roles to assign to the user.
     * This parameter accepts one or more role names.
     */
    @Parameters(
        index = "0",
        description = ["Roles to assign to the user"],
        split = " ",
        arity = "1..*",
    )
    var roles: Array<String> = emptyArray()

    /**
     * Executes the command to grant roles to the specified user.
     * The command checks if the current user has permission to modify the roles of others,
     * and if the user exists, then assigns the specified roles.
     *
     * @return an exit code indicating the success (0) or failure (1) of the operation.
     */
    override fun call(): Int {
        super.call()

        // Create an instance of RoleGrantHandler with the necessary arguments
        val handler = RoleGrantHandler(
            username = username,
            roleNames = roles,
            sudoArgs = sudoArgs,
        )

        // Check if the user has permission to modify user roles
        if (!handler.checkCanModifyUser()) {
            return 1
        }

        // Assign the roles to the user
        handler.addRoles()

        return 0
    }
}
