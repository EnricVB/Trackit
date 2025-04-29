package dev.enric.cli.management.grantPermission

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.grantPermission.BranchPermissionGrantHandler
import picocli.CommandLine.*

/**
 * The BranchPermissionRevokeCommand class is responsible for revoking permissions over a Branch for a specific role.
 *
 * Usage examples:
 * - Revoke roles to a user:
 *   trackit revoke-branchPermission <role> <branches...>
 *
 * - Display help for the command:
 *   trackit revoke-role --help
 */
@Command(
    name = "revoke-branchPermission",
    description = ["Revoke branch permissions to a specific role."],
    footer = [
        "",
        "Examples:",
        "  trackit revoke-branchPermission <role> <branches...>",
        "    Revokes the specified permission to the given role over the specified branches.",
        "",
        "  trackit revoke-role --help",
        "    Displays help information for this command."
    ],
    mixinStandardHelpOptions = true,
)
class BranchPermissionRevokeCommand : TrackitCommand() {

    /**
     * The name of the role to whose permission(s) will be revoked.
     * This option is required for the command to execute.
     */
    @Parameters(
        index = "0",
        description = ["Name of the role to whom permissions will be revoked"],
        arity = "1",
    )
    var role: String = ""

    /**
     * The branches to revoke the permissions.
     * This parameter accepts one or more branch names.
     */
    @Parameters(
        index = "1",
        description = ["Branch names to revoke permissions"],
        split = " ",
        arity = "1..*",
    )
    var branchNames: Array<String> = emptyArray()

    /**
     * Executes the command to revoke permissions to the specified role.
     * The command checks if the current user has permission to modify the roles of others,
     *
     * @return an exit code indicating the success (0) or failure (1) of the operation.
     */
    override fun call(): Int {
        super.call()

        // Create an instance of RoleGrantHandler with the necessary arguments
        val handler = BranchPermissionGrantHandler(
            role = role,
            branchNames = branchNames,
            newPermission = "--",
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
