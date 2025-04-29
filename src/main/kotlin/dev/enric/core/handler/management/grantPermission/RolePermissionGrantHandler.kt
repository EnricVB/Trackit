package dev.enric.core.handler.management.grantPermission

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash.HashType.ROLE_PERMISSION
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex

/**
 * @property sudoArgs The credentials of the sudo user (optional).
 */
class RolePermissionGrantHandler(
    val role: String,
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
        val role = RoleIndex.getRoleByName(role) ?: throw UserNotFoundException("Role $role not found")

        role.permissions
            .filter { permissionHash ->
                return@filter permissionHash.string.startsWith(ROLE_PERMISSION.hash.string)
            }.forEach { permissionHash ->
                val rolePermission = RolePermission.newInstance(permissionHash)
                val newRolePermission = RolePermission(newPermission).encode(true).first

                // Replace the old permission with the new one
                role.permissions.remove(permissionHash)
                role.permissions.add(newRolePermission)


                Logger.debug("Updated permission for role ${role.name}: " +
                        "CreateRole=${rolePermission.createRolePermission}, " +
                        "AssignRole=${rolePermission.assignRolePermission}, " +
                        "ModifyRole=${rolePermission.modifyRolePermission}, " +
                        "UserOperation=${rolePermission.userOperationPermission}")
            }

        // Save the updated role
        role.encode(true)
    }
}