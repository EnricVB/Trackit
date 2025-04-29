package dev.enric.cli.management.grantPermission

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.grantPermission.RolePermissionGrantHandler
import picocli.CommandLine.*

/**
 * The RolePermissionGrantCommand class is responsible for granting permissions over a role for a specific role.
 *
 * Usage examples:
 * - Grant roles to a user:
 *   trackit grant-rolePermission <role> <permission>
 * 
 * - Display help for the command:
 *   trackit grant-rolePermission --help
 */
@Command(
    name = "grant-rolePermission",
    description = ["Grant role permissions to a specific role"],
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit grant-rolePermission <role> <permission>",
        "    Grants the specified permission to the given role over the specified roles.",
        "",
        "  trackit grant-role --help",
        "    Displays help information for this command."
    ],
    mixinStandardHelpOptions = true,
)
class RolePermissionGrantCommand : TrackitCommand() {

    /**
     * The name of the role to whose permission(s) will be granted.
     * This option is required for the command to execute.
     */
    @Parameters(
        index = "0",
        description = ["Name of the role to whom permissions will be granted"],
        arity = "1",
    )
    var role: String = ""

    /**
     * The roles to assign the permissions.
     * This parameter accepts one or more roles names.
     */
    @Parameters(
        index = "1",
        description = ["Role permissions"],
        arity = "1",
    )
    var permission: String = "--"

    /**
     * Executes the command to grant permissions to the specified role.
     * The command checks if the current user has permission to modify the roles of others,
     *
     * @return an exit code indicating the success (0) or failure (1) of the operation.
     */
    override fun call(): Int {
        super.call()

        // Create an instance of RoleGrantHandler with the necessary arguments
        val handler = RolePermissionGrantHandler(
            role = role,
            newPermission = permission.replace("'", ""),
            sudoArgs = sudoArgs,
        )

        // Check if the user has permission to modify user roles
        if (!handler.checkCanModifyRole()) {
            return 1
        }

        // Assign the roles to the user
        handler.changeBranchPermissions()

        return 0
    }
}
