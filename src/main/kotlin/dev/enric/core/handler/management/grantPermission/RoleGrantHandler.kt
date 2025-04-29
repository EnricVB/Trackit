package dev.enric.core.handler.management.grantPermission

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

/**
 * GrantRoleHandler is responsible for modifying an existing user's information in the system.
 *
 * @property username The name of the user to be modified.
 * @property roleNames The new role names to be assigned to the user (optional).
 * @property sudoArgs The credentials of the sudo user (optional).
 */
class RoleGrantHandler(
    val username: String,
    val roleNames: Array<String>,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the user has the permission to modify other user's roles.
     *
     * @return True if the user has the permission to modify other users, false otherwise.
     * @throws UserNotFoundException If the user does not exist.
     * @throws InvalidPermissionException If the user does not have permission to modify users.
     *
     */
    fun checkCanModifyUser(): Boolean {
        val sudo = isValidSudoUser(sudoArgs)
        userExists(username)
        checkHasModifyUserPermission(sudo)

        return true
    }

    /**
     * Modifies the user's roles based on the provided arguments.
     */
    fun addRoles() {
        val sudo = isValidSudoUser(sudoArgs)
        val user = UserIndex.getUser(username)!!

        // Assign new roles if new role names are provided
        if (roleNames.isNotEmpty()) {
            val rolesToAdd = getRolesToAssign(sudo)

            if (rolesToAdd.isEmpty() && user.roles.isEmpty()) {
                throw IllegalArgumentValueException("No roles found to assign to the user. Please provide at least one valid role.")
            }

            user.roles.addAll(rolesToAdd.map { it.generateKey() })
        }

        user.encode(true)
    }

    /**
     * Removes the user's roles based on the provided arguments.
     */
    fun removeRoles() {
        val sudo = isValidSudoUser(sudoArgs)
        val user = UserIndex.getUser(username)!!

        // Remove roles if new role names are provided
        if (roleNames.isNotEmpty()) {
            val rolesToRemove = getRolesToRemove(sudo)

            if (rolesToRemove.isEmpty() && user.roles.isEmpty()) {
                throw IllegalArgumentValueException("No roles found to remove from the user. Please provide at least one valid role.")
            }

            user.roles.removeAll(rolesToRemove.map { it.generateKey() })
        }
    }

    /**
     * Assigns roles to the user based on the provided role names.
     *
     * @return A list of roles that can be assigned to the user.
     */
    private fun getRolesToAssign(sudo: User): MutableList<Role> {
        return roleNames.mapNotNull { RoleIndex.getRoleByName(it) }.filter {
            val canAddRole = it.permissionLevel > sudo.roles.map { sudoRoles -> Role.newInstance(sudoRoles) }
                .maxOf { sudoRole -> sudoRole.permissionLevel }

            if (!canAddRole) {
                Logger.error("User does not have permission to add role ${it.name}. The role has a higher permission level than the user")
                Logger.error("Skipping role ${it.name}")
            }

            return@filter canAddRole
        }.toMutableList()
    }

    /**
     * Remove roles to the user based on the provided role names.
     *
     * @return A list of roles that can be removed from the user.
     */
    private fun getRolesToRemove(sudo: User): MutableList<Role> {
        return roleNames.mapNotNull { RoleIndex.getRoleByName(it) }.filter {
            val canRemoveRole = it.permissionLevel > sudo.roles.map { sudoRoles -> Role.newInstance(sudoRoles) }
                .maxOf { sudoRole -> sudoRole.permissionLevel }

            if (!canRemoveRole) {
                Logger.error("User does not have permission to remove role ${it.name}. The role has a lower permission level than the user")
                Logger.error("Skipping role ${it.name}")
            }

            return@filter canRemoveRole
        }.toMutableList()
    }
}