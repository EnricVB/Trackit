package dev.enric.core.handler.management

import dev.enric.core.handler.CommandHandler
import dev.enric.core.security.AuthUtil
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

/**
 * UserCreationHandler is responsible for handling the creation of users in the system.
 * It performs all necessary checks to ensure that the user can be created and assigns roles to the user.
 *
 * @property name The name of the user to be created.
 * @property password The password for the user.
 * @property mail The email address of the user (optional).
 * @property phone The phone number of the user (optional).
 * @property roleNames The list of role names to be assigned to the user.
 * @property sudoArgs The credentials of the sudo user (optional).
 */
class UserCreationHandler(
    val name: String,
    val password: String,
    val mail: String?,
    val phone: String?,
    val roleNames: Array<String>,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the user can be created. Performs necessary checks including:
     * - If the user already exists.
     * - If the sudo user is valid and has the necessary permissions.
     *
     * @return True if the user can be created, false otherwise.
     * @throws InvalidPermissionException If the user already exists or if the sudo user does not have permission to create users.
     * @throws UserNotFoundException If the sudo user is not found.
     */
    fun checkCanCreateUser(): Boolean {
        userExists()
        canCreateUser(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Verifies if the user already exists.
     *
     * @throws InvalidPermissionException If the user already exists.
     */
    fun userExists() {
        if (UserIndex.userAlreadyExists(name)) {
            throw InvalidPermissionException("User already exists")
        }
    }

    /**
     * Checks if the user has the permission to create users looking at the role permissions.
     *
     * @param user The user to check the permissions.
     * @return True if the user has the permission to create users, false otherwise.
     */
    private fun hasCreateUserPermission(user: User): Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getRolePermissions().any { it.userOperationPermission }
        }
    }

    /**
     * Checks if the user has the permission to create users looking at the role permissions.
     *
     * @return True if the user has the permission to create users, false otherwise.
     */
    private fun canCreateUser(user: User): Boolean {
        val canCreateUser = hasCreateUserPermission(user)

        if (!canCreateUser) {
            throw InvalidPermissionException("User does not have permission to create users.")
        }

        return true
    }

    /**
     * Creates a new user and assigns roles to them.
     * If no roles are provided or no valid roles are found, assigns a default role.
     */
    fun createUser() {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()!!
        val roles = assignRoles(sudo)

        // If no roles are provided, add the default role
        if (roles.isEmpty()) {
            Logger.error("No roles found, adding default role")
            roles.add(RoleIndex.UNDEFINED_ROLE)
        }

        // Create the user
        User.createUser(name, password, mail ?: "", phone ?: "", roles).encode(true)
        Logger.info("User $name created")
    }


    /**
     * Assigns roles to the new user. It filters out roles that the sudo user doesn't have permission to assign.
     * If a role has a higher permission level than the sudo user, it won't be assigned.
     *
     * @param sudo The sudo user who is creating the new user.
     * @return A list of roles assigned to the new user.
     */
    fun assignRoles(sudo: User): MutableList<Role> {
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
}