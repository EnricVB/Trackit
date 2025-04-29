package dev.enric.core.handler.management.grantPermission

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash.HashType.BRANCH_PERMISSION
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.RoleIndex

/**
 *
 * @property sudoArgs The credentials of the sudo user (optional).
 */
class BranchPermissionGrantHandler(
    val role: String,
    val branchNames: Array<String>,
    val newPermission: String,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the user has the permission to modify roles.
     *
     * @return True if the user has the permission to modify roles, false otherwise.
     * @throws UserNotFoundException If the user does not exist.
     * @throws InvalidPermissionException If the user does not have permission to modify users.
     *
     */
    fun checkCanModifyRole(): Boolean {
        val sudo = isValidSudoUser(sudoArgs)
        checkHasModifyRolePermission(sudo)

        return true
    }

    /**
     * Modifies the role's permissions based on the provided arguments.
     */
    fun changeBranchPermissions() {
        val role = RoleIndex.getRoleByName(role)
        val branches = branchNames.mapNotNull { BranchIndex.getBranch(it) }

        if (branches.isEmpty()) {
            throw IllegalArgumentValueException("No branches found to assign permissions. Please provide at least one valid branch.")
        }

        if (role == null) {
            throw IllegalArgumentValueException("No role found to assign permissions. Please provide at least one valid role.")
        }

        role.permissions
            .filter { permissionHash ->
                if (!permissionHash.string.startsWith(BRANCH_PERMISSION.hash.string)) return@filter false
                return@filter BranchPermission.newInstance(permissionHash).branch in branches.map { it.generateKey() }
            }.forEach { permissionHash ->
                val branchPermission = BranchPermission.newInstance(permissionHash)

                val newBranchPermission = BranchPermission(
                    newPermission,
                    branchPermission.branch
                ).encode(true).first

                // Replace the old permission with the new one
                role.permissions.remove(permissionHash)
                role.permissions.add(newBranchPermission)

                Logger.debug("Updated permission for role ${role.name} on branch ${branchPermission.branch.string}: " +
                            "Read=${branchPermission.readPermission}, Write=${branchPermission.writePermission} " +
                            "to Read=$newPermission, Write=$newPermission")
            }

        // Save the updated role
        role.encode(true)
    }
}