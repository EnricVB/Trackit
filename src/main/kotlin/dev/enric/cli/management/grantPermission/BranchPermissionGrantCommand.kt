package dev.enric.cli.management.grantPermission

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.grantPermission.BranchPermissionGrantHandler
import picocli.CommandLine.*

/**
 * The BranchPermissionGrantCommand class is responsible for granting permissions over a Branch for a specific role.
 *
 * Usage examples:
 * - Grant roles to a user:
 *   trackit grant-branchPermission <role> <branches...> <permission>
 *
 * - Display help for the command:
 *   trackit grant-role --help
 */
@Command(
    name = "grant-branchPermission",
    description = ["Grant branch permissions to a specific role"],
    footer = [
        "",
        "Examples:",
        "  trackit grant-branchPermission <role> <branches...> <permission>",
        "    Grants the specified permission to the given role over the specified branches.",
        "",
        "  trackit grant-role --help",
        "    Displays help information for this command."
    ],
    mixinStandardHelpOptions = true,
)
class BranchPermissionGrantCommand : TrackitCommand() {

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
     * The branches to assign the permissions.
     * This parameter accepts one or more branch names.
     */
    @Parameters(
        index = "1",
        description = ["Branch names to assign permissions"],
        split = " ",
        arity = "1..*",
    )
    var branchNames: Array<String> = emptyArray()

    /**
     * The branches to assign the permissions.
     * This parameter accepts one or more branch names.
     */
    @Parameters(
        index = "2",
        description = ["Branch permissions"],
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
        val handler = BranchPermissionGrantHandler(
            role = role,
            branchNames = branchNames,
            newPermission = permission,
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
