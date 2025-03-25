package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.roles.RoleModifyHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Command to modify an existing role's configuration, including permission levels,
 * role permissions, and branch-specific permissions.
 *
 * This command allows fine-grained control over role management, enabling the addition,
 * removal, or overwriting of permissions assigned to a role.
 *
 * Usage examples:
 *   - Modify role permission level:
 *     trackit modify-role -n Developer -l 2
 *
 *   - Add role permissions:
 *     trackit modify-role -n Maintainer -r mus-
 *
 *   - Add branch permissions:
 *     trackit modify-role -n QA -b feature rw
 *
 *   - Remove branch permissions:
 *     trackit modify-role -n QA -rb feature hotfix
 *
 *   - Overwrite all role permissions:
 *     trackit modify-role -n Admin -r mus- -o
 */
@Command(
    name = "modify-role",
    description = ["Modify an existing role’s permissions and configuration."],
    footer = [
        "",
        "Description:",
        "  Allows modification of a role’s permission level, role permissions, and branch access.",
        "",
        "Role Permissions:",
        "  m: Modify roles (only those with lower permission level)",
        "  u: Create/modify users",
        "  s: Assign roles to users",
        "  a: Create new roles",
        "  -: No permission in that position",
        "",
        "Branch Permissions:",
        "  Format: --branch-permission <branch> <permission>",
        "  Use '--' (in quotes) for no permission.",
        "",
        "Examples:",
        "  trackit modify-role -n Developer -l 2",
        "  trackit modify-role -n Maintainer -r mus-",
        "  trackit modify-role -n QA -b feature rw",
        "  trackit modify-role -n QA -rb feature hotfix",
        "  trackit modify-role -n Admin -r mus- -o",
        ""
    ],
    mixinStandardHelpOptions = true,
)

class RoleModify : TrackitCommand() {

    /**
     * The name of the role to be modified.
     */
    @Option(names = ["--name", "-n"], description = ["Role name to modify."], required = true)
    var name: String = ""

    /**
     * New permission level for the role.
     * Cannot be equal to or higher than the current user's permission level.
     */
    @Option(
        names = ["--permission-level", "-l"],
        description = ["Level permissions. Can't be equal or greater than user permissions level"],
        required = false
    )
    var level: Int = 0

    /**
     * Role permissions to add to the current role.
     * Format: 4-character string using 'm', 'u', 's', 'a', or '-' (no permission).
     *
     * Example:
     *   - "mus-" grants modify, user, assign permissions, but not create role permission.
     */
    @Option(
        names = ["--role-permission", "-r"],
        description = ["Adds new role permissions to the current one. The permissions are: \n" + "  - m: Modify role permissions, except a equal or higher roles\n" + "  - u: User operations, as create or modify users\n" + "  - s: Assign new roles to specified user\n" + "  - a: Create new roles\n" + "  - '-': To specify role has not the permission"]
    )
    var rolePermissions: String = "----"

    /**
     * Branch permissions to assign to the role.
     * Format: --branch-permission <branch> <permission>
     *
     * Example:
     *   -b main rw → Grants read/write on 'main'
     *   -b feature r → Grants read-only on feature branches
     *
     * Use '--' (in quotes) to assign empty permission.
     */
    @Option(
        names = ["--branch-permission", "-b"],
        description = ["Adds branch permissions. Format: --branch-permission <branch> <permission>. \n" + "  In case you want to assign an empty permission, use '--' between ' '."],
        split = " ",
        arity = "2",
    )
    var branchPermissions: MutableList<String> = mutableListOf()

    /**
     * Branch permissions to remove from the role.
     * Format: --remove-branch-permission <branch1> <branch2> ...
     */
    @Option(
        names = ["--remove-branch-permission", "-rb"],
        description = ["Removes branch permissions. Format: --remove-branch-permission <branch> <branch> ..."],
        split = " "
    )
    var removeBranchPermissions: MutableList<String> = mutableListOf()

    /**
     * Overwrite existing role permissions with the specified ones.
     * If false, new permissions are added without removing existing ones.
     */
    @Option(
        names = ["--overwrite", "-o"], description = ["Overwrite the current role permissions"]
    )
    var overwrite: Boolean = false

    /**
     * Executes the role modification.
     *
     * Delegates the logic to [RoleModifyHandler], which validates permissions
     * and applies the changes.
     *
     * @return 0 on success, 1 on failure (e.g., permission denied).
     */
    override fun call(): Int {
        super.call()

        val handler = RoleModifyHandler(
            name, level, rolePermissions, branchPermissions, removeBranchPermissions, overwrite, sudoArgs
        )

        // Validation will throw exception if modification is not permitted
        if (!handler.checkCanModifyRole()) {
            return 1
        }

        handler.modifyRole()

        return 0
    }
}
