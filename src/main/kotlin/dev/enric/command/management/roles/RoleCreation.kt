package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.roles.RoleCreationHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option


/**
 * Command to create a new role in the repository or system.
 *
 * This command allows administrators to define custom roles with specific permission levels and access rights
 * to control operations such as user management, role management, and branch-specific actions.
 *
 * Usage examples:
 *   trackit role-create --name Developer --permission-level 1 --role-permission mus- --branch-permission main rw
 *   trackit role-create -n Reviewer -l 2 -r ---- -b main r- -b dev '--'
 *
 * Available permissions:
 * - Role Permissions:
 *      m → Modify other roles (only those with lower permission levels)
 *      u → Create or modify users
 *      s → Assign roles to users
 *      a → Create new roles
 *      '-' → No permission in that position
 *
 * - Branch Permissions:
 *      Format: <branch> <permission>
 *      Use '--' (between quotes) to denote no permissions
 */
@Command(
    name = "role-create",
    description = ["Create a new role with specific permission and access levels."],
    footer = [
        "",
        "Examples:",
        "  trackit role-create --name Developer --permission-level 1 --role-permission mus- --branch-permission main rw",
        "  trackit role-create -n Reviewer -l 2 -r ---- -b main r- -b dev '--'",
        "",
        "Role Permissions (4-character string):",
        "  m → Modify roles (below current level)",
        "  u → Create/modify users",
        "  s → Assign roles",
        "  a → Create new roles",
        "  - → No permission",
        "",
        "Branch Permissions (format: <branch> <permission>):",
        "  Example: --branch-permission feature rw",
        "  Use '--' (in quotes) to denote no permissions.",
        "",
        "Notes:",
        "  - Role permission level must be lower than your current level.",
        "  - Multiple --branch-permission flags can be used."
    ],
    mixinStandardHelpOptions = true,
)
class RoleCreation : TrackitCommand() {

    /**
     * Name of the role to be created.
     */
    @Option(names = ["--name", "-n"], description = ["Role name."], required = true)
    var name: String = ""

    /**
     * Numeric level of permissions for this role.
     * Cannot be equal or higher than the current user's permission level.
     * Defaults to 0 if not specified.
     */
    @Option(
        names = ["--permission-level", "-l"],
        description = ["Level permissions. Can't be equal or greater than user permissions level"],
        required = false
    )
    var level: Int = 0

    /**
     * Role-level permissions represented as a 4-character string (e.g., "mus-").
     * Each character defines whether the role has the specific permission.
     * Defaults to "----" (no permissions).
     */
    @Option(
        names = ["--role-permission", "-r"],
        description = ["Assign role permissions. The permissions are: \n" +
                "  - m: Modify role permissions, except a equal or higher roles\n" +
                "  - u: User operations, as create or modify users\n" +
                "  - s: Assign new roles to specified user\n" +
                "  - a: Create new roles\n" +
                "  - '-': To specify role has not the permission"]
    )
    var rolePermissions: String = "----"


    /**
     * Branch-level permissions.
     * Format: <branch> <permission>. Example: --branch-permission feature rw
     * Use '--' (in quotes) to specify no permissions.
     * Can be specified multiple times.
     */
    @Option(
        names = ["--branch-permission", "-b"],
        description = ["Assign branch permissions. Format: --branch-permission <branch> <permission>. \n" +
                "  In case you want to asssign a empty permission, use '--' between ' '."],
        split = " ",
        arity = "2",
    )
    var branchPermissions: MutableList<String> = mutableListOf()

    override fun call(): Int {
        super.call()

        val handler = RoleCreationHandler(
            name,
            level,
            rolePermissions,
            branchPermissions,
            sudoArgs
        )

        // Will never return 1 because the checkCanCreateRole method will throw an exception if the role can't be created
        if (!handler.checkCanCreateRole()) {
            return 1
        }

        handler.createRole()

        return 0
    }
}