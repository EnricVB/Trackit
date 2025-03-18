package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.roles.RoleListHandler
import picocli.CommandLine.Command

/**
 * Command to list all existing roles in the system or repository.
 *
 * This command provides a summary of the roles, including their names, permission levels,
 * role permissions, and branch permissions, if available.
 *
 * Useful for administrators or users with sufficient privileges to audit the current
 * permission structure of the repository.
 *
 * Usage example:
 *   trackit role-list
 *
 * Output includes:
 *   - Role name
 *   - Permission level
 *   - Role permissions (e.g., mus-)
 *   - Branch permissions (e.g., main rw, feature r)
 */
@Command(
    name = "role-list",
    description = ["List all roles"]
)
class RoleList : TrackitCommand() {

    override fun call(): Int {
        super.call()

        RoleListHandler().listRoles()

        return 0
    }
}