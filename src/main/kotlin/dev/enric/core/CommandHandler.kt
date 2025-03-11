package dev.enric.core

import dev.enric.core.security.AuthUtil
import dev.enric.domain.User
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
    protected fun isValidSudoUser(sudoArgs : Array<String>?): User {
        var sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "")

        if (sudo == null) {
            Logger.error("Sudo user not found. Trying to login with logged user.")
            sudo = AuthUtil.getLoggedUser()
        }

        if (sudo == null) {
            throw UserNotFoundException("User not found. Please login first or use --sudo with proper credentials.")
        } else {
            Logger.log("Logged in with user: ${sudo.name}")
        }

        return sudo
    }

    /**
     * Validates the role permissions string format.
     *
     * @throws InvalidPermissionException If the role permissions string is invalid.
     */
    protected fun isValidRolePermissions(rolePermissions: String) {
        if (!validateRolePermissionsString(rolePermissions)) {
            throw InvalidPermissionException("Invalid role permissions string. " +
                    "Must be a 4-character string with only 'm', 'u', 's', 'a' or '-' characters.")
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
