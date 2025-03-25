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
 *   trackit list-role
 *
 * Output includes:
 *   - Role name
 *   - Permission level
 *   - Role permissions (e.g., mus-)
 *   - Branch permissions (e.g., main rw, feature r)
 */
@Command(
    name = "list-role",
    description = ["List all roles in the repository or system."],
    footer = [
        "",
        "Description:",
        "  Displays all roles with their permission levels, role permissions, and branch-specific access.",
        "",
        "Output includes:",
        "  - Role name",
        "  - Permission level",
        "  - Role permissions (e.g., mus-)",
        "  - Branch permissions (e.g., main rw, feature r-)",
        "",
        "Example:",
        "  trackit list-role"
    ],
    mixinStandardHelpOptions = true,
)
class RoleList : TrackitCommand() {

    override fun call(): Int {
        super.call()

        RoleListHandler().listRoles()

        return 0
    }
}