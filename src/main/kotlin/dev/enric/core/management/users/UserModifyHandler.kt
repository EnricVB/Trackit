package dev.enric.core.management.users

import dev.enric.core.security.AuthUtil
import dev.enric.core.security.PasswordHash
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

/**
 * UserModifyHandler is responsible for modifying an existing user's information in the system.
 * It allows updating the user's password, email, phone number, and roles.
 * The handler also ensures that the user has the necessary permissions to modify other users.
 *
 * @property name The name of the user to be modified.
 * @property password The current password of the user.
 * @property newPassword The new password for the user (optional).
 * @property newMail The new email address for the user (optional).
 * @property newPhone The new phone number for the user (optional).
 * @property newRoleNames The new role names to be assigned to the user (optional).
 * @property deletePreviousRoles Flag to indicate if the previous roles should be deleted.
 * @property sudoArgs The credentials of the sudo user (optional).
 */
class UserModifyHandler(
    val name: String,
    val password: String,
    val newPassword: String?,
    val newMail: String?,
    val newPhone: String?,
    val newRoleNames: Array<String>,
    val deletePreviousRoles: Boolean,
    val sudoArgs: Array<String>? = null
) {

    fun checkCanModifyUser(): Boolean {
        val sudo = isValidSudoUser()
        userExists()
        hasModifyUserPermission(sudo)

        return true
    }

    /**
     * Verifies if the user already exists.
     *
     * @throws UserNotFoundException If the user does not exist.
     */
    fun userExists() {
        if (!UserIndex.userAlreadyExists(name)) {
            throw UserNotFoundException("User does not exist. Please create the user first.")
        }
    }

    /**
     * Verifies if the user is logged in or if the sudo user exists.
     *
     * @throws UserNotFoundException If the sudo user is not found.
     */
    private fun isValidSudoUser(): User {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()

        if (sudo == null) {
            throw UserNotFoundException("Sudo user not found. Please login first or use --sudo with proper credentials.")
        } else {
            Logger.log("Logged user: ${sudo.name}")
        }

        return sudo
    }

    /**
     * Checks if the user has the permission to modify users looking at the role permissions.
     *
     * @param user The user to check the permissions.
     * @return True if the user has the permission to modify users, false otherwise.
     */
    private fun hasModifyUserPermission(user: User): Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getRolePermissions().any { it.userOperationPermission }
        }
    }

    /**
     * Modifies the user's information based on the provided arguments.
     * - Updates password, email, phone number, and roles.
     * - Deletes previous roles if specified.
     */
    fun modifyUser() {
        val sudo = isValidSudoUser()
        val user = UserIndex.getUser(name, password)!!

        // Modify password if new password is provided and it is different from the old one
        if (!newPassword.isNullOrEmpty()) {
            if (newPassword != password) {
                user.password = PasswordHash.hash(newPassword, user.salt)
            } else {
                Logger.error("New password is empty or the same as the old one")
            }
        }

        // Modify email and phone number if new values are provided
        if (!newMail.isNullOrEmpty()) {
            Logger.log("Changing mail to $newMail")
            user.mail = newMail
        }

        if (!newPhone.isNullOrEmpty()) {
            Logger.log("Changing phone number to $newPhone")
            user.phone = newPhone
        }

        // Assign new roles if new role names are provided
        if (newRoleNames.isNotEmpty()) {
            val roles = assignRoles(sudo)

            if (deletePreviousRoles) {
                Logger.log("Deleting previous roles")
                user.roles.clear()
            }

            if (roles.isEmpty() && user.roles.isEmpty()) {
                Logger.error("No roles found, adding default role")
                roles.add(RoleIndex.UNDEFINED_ROLE)
            }

            user.roles.addAll(roles.map { it.encode().first })
        }

        user.encode(true)
    }

    /**
     * Checks if the user has the permission to modify users looking at the role permissions.
     *
     * @return True if the user has the permission to modify users, false otherwise.
     */
    fun assignRoles(sudo: User): MutableList<Role> {
        return newRoleNames.mapNotNull { RoleIndex.getRoleByName(it) }.filter {
            val canAddRole = it.permissionLevel > sudo.roles.map { sudoRoles -> Role.newInstance(sudoRoles) }
                .maxOf { sudoRole -> sudoRole.permissionLevel }

            if (!canAddRole) {
                Logger.error("User does not have permission to add role ${it.name}. The role has a higher permission level than the user")
                Logger.error("Skipping role ${it.name}")
            }

            return@filter canAddRole
        }.toMutableList()
    }
}