package dev.enric.core.handler

import dev.enric.command.branch.Branch
import dev.enric.core.security.AuthUtil
import dev.enric.domain.Hash
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.UserIndex

open class CommandHandler {
    /**
     * Verifies if the user is logged in or if the sudo user exists.
     *
     * @throws UserNotFoundException If the sudo user is not found.
     * @return The user that is logged in or the sudo user.
     */
    protected fun isValidSudoUser(sudoArgs: Array<String>?): User {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "")
        val loggedUser = AuthUtil.getLoggedUser()

        val user: User?

        if (sudo != null) {
            user = sudo
        } else if (loggedUser != null) {
            if (!sudoArgs.isNullOrEmpty()) {
                Logger.warning("Couldn't log in with sudo credentials. Login with logged user")
            }

            user = loggedUser
        } else {
            throw UserNotFoundException("User ${sudoArgs?.first()} not found. Try keeping session with 'trackit config --keep-session' or use '--sudo' option.")
        }

        Logger.log("Logged in with ${user.name}")
        return user
    }

    /**
     * Validates the role permissions string format.
     *
     * @throws InvalidPermissionException If the role permissions string is invalid.
     */
    protected fun isValidRolePermissions(rolePermissions: String) {
        if (!validateRolePermissionsString(rolePermissions)) {
            throw InvalidPermissionException(
                "Invalid role permissions string. " +
                        "Must be a 4-character string with only 'm', 'u', 's', 'a' or '-' characters."
            )
        }
    }

    /**
     * Validates the role permissions string format.
     *
     * @param permissions The role permissions string to validate.
     * @return True if the role permissions string is valid, false otherwise.
     */
    fun validateRolePermissionsString(permissions: String): Boolean {
        return permissions.length == 4 && permissions.toList().allIndexed { index, char ->
            val validChars = listOf('m', 'u', 's', 'a')
            val expectedChar = validChars.getOrElse(index) { return false }
            char == expectedChar || char == '-'
        }
    }

    /**
     * Checks if the user has write permission on the current branch.
     * @param user The user to check.
     * @return True if the user has write permission on the branch, false otherwise.
     */
    fun hasWritePermissionOnBranch(user: User, branch: Hash) : Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getBranchPermissions().any { it.branch == branch && it.writePermission }
        }
    }

    /**
     * Checks if the user has read permission on the commit branch.
     * @param user The user to check.
     * @return True if the user has read permission on the branch, false otherwise.
     */
    fun hasReadPermissionOnBranch(user: User, branch: Hash) : Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getBranchPermissions().any { it.branch == branch && it.readPermission }
        }
    }
}

/**
 * Checks if all elements in the iterable match the predicate.
 *
 * @param predicate The predicate to match.
 * @return True if all elements match the predicate, false otherwise.
 */
inline fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
    return this.withIndex().all { (index, item) -> predicate(index, item) }
}
