package dev.enric.core.management.roles

import dev.enric.core.CommandHandler
import dev.enric.domain.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.allIndexed
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.exceptions.UserNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.*
import javax.management.relation.RoleNotFoundException

/**
 * RoleModifyHandler is a class that handles the modify of permissions of roles.
 *
 * It ensures that the role to be modified has valid permissions and branch permissions.
 * And that the user has the necessary permissions to modify the role.
 *
 * @property name The name of the role to be modified.
 * @property level The new level of the role. 1 being the highest level.
 * @property rolePermissions A 4-character string representing the permissions of the role. This will overwrite the current permissions.
 * Each character represents a permission:
 * m (modify roles), u (user operations), s (assign new roles), a (add new roles).
 *
 * @property branchPermissions A list of the new branch permissions to be added, where each element contains:
 * 1. branch name
 * 2. a 2-character string representing branch permissions:
 * r (read), w (write).
 *
 * @property removeBranchPermissions A list of the branch names of the permissions to be removed.
 *
 * @property overwrite If true, the role permissions will be overwritten with the new ones.
 *
 * @property sudoArgs The arguments of the sudo user.
 */

class RoleModifyHandler(
    val name: String,
    val level: Int,
    val rolePermissions: String,
    val branchPermissions: MutableList<String>,
    val removeBranchPermissions: MutableList<String>,
    val overwrite: Boolean,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the role can be modified by verifying several conditions:
     * - Whether the role already exists. Must exist.
     * - Whether the role permissions are valid.
     * - Whether the branch permissions are valid.
     * - Whether the user has the necessary permissions to modify a role.
     *
     * @return True if the role can be modified, false otherwise.
     * @throws InvalidPermissionException If the role already exists or if the permissions are invalid.
     * @throws BranchNotFoundException If a specified branch does not exist.
     * @throws UserNotFoundException If the sudo user is not found.
     */
    fun checkCanModifyRole(): Boolean {
        roleDoesntExists()
        isValidRolePermissions(rolePermissions)
        isValidBranchPermissions()
        canModifyRole(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Verifies if the role to be modified exists.
     *
     * @throws UserNotFoundException If the role does not exist.
     */
    private fun roleDoesntExists() {
        if (!RoleIndex.roleAlreadyExists(name)) {
            throw RoleNotFoundException("Role $name does not exist. Try creating it first.")
        }
    }

    /**
     * Validates the branch permissions list format.
     *
     * @throws IllegalArgumentValueException If the branch permissions list is invalid.
     */
    private fun isValidBranchPermissions() {
        if (branchPermissions.size % 2 != 0) {
            throw IllegalArgumentValueException("Invalid branch permissions list. " +
                    "Each branch permission must have a branch name and a 2-character string with only 'r', 'w' or '-' characters.")
        }
    }

    /**
     * Verifies if the user has the necessary permissions to modify a role.
     * The user must have the modify role permission and have a level equal or higher than the role to be modified.
     *
     * @return True if the user has the necessary permissions, false otherwise.
     * @throws InvalidPermissionException If the user does not have the necessary permissions.
     */
    private fun canModifyRole(sudo: User): Boolean {
        val hasModifyRolePermission = hasModifyRolePermission(sudo)
        val hasEnoughLevel = level >= getHighestRoleLevel(sudo)

        if(!hasModifyRolePermission) {
            throw InvalidPermissionException("User does not have permission to modify roles.")
        }

        if(!hasEnoughLevel) {
            throw InvalidPermissionException("User does not have enough level to modify roles. " +
                    "\nRequired level: $level, user level: ${getHighestRoleLevel(sudo)}")
        }

        return true
    }


    /**
     * Checks if the user has the permission to modify roles looking at the role permissions.
     *
     * @param user The user to check the permissions.
     * @return True if the user has the permission to modify roles, false otherwise.
     */
    private fun hasModifyRolePermission(user: User): Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getRolePermissions().any { it.modifyRolePermission }
        }
    }

    /**
     * Gets the highest level of the user roles.
     *
     * @param user The user to get the highest level.
     * @return The highest level of the user roles.
     */
    private fun getHighestRoleLevel(user: User): Int {
        return user.roles.map { Role.newInstance(it) }
            .maxOfOrNull { it.permissionLevel } ?: 0
    }

    /**
     * Modifies the role by assigning the new role permissions, branch permissions and removing branch permissions.
     * - Modifies the role permissions.
     * - Assigns the new branch permissions.
     * - Removes the branch permissions.
     * - Saves the role to the system.
     *
     * @throws RoleNotFoundException If the role does not exist.
     */
    fun modifyRole() {
        val role = RoleIndex.getRoleByName(name) ?: throw RoleNotFoundException("Role $name does not exist")

        // If the overwrite flag is true, remove all role permissions
        if (overwrite) {
            role.permissions.clear()
            Logger.log("Role permissions overwritten")
        }

        // Assign the new role permissions, branch permissions and remove branch permissions
        assignRolePermissions(role)
        assignBranchPermissions(role)
        removeBranchPermissions(role)

        // Save the role
        Logger.log("Role modified successfully")
        role.encode(true)
    }

    /**
     * Assigns the new role permissions to the role.
     * The role permissions are a 4-character string representing the permissions of the role.
     * Each character represents a permission:
     * m (modify roles), u (user operations), s (assign new roles), a (add new roles).
     *
     * @param role The role to assign the new permissions.
     * @param rolePermissions The new role permissions string.
     */
    fun assignRolePermissions(role: Role, rolePermissions: String = this.rolePermissions) {
        // Remove all role permissions to replace them with the new ones
        role.permissions.removeAll(role.getRolePermissions().map { it.encode().first })

        val rolePermission = RolePermissionIndex.getRolePermission(rolePermissions)?.encode(true)?.first
            ?: RolePermission(rolePermissions).encode(true).first

        role.permissions.add(rolePermission)
    }


    /**
     * Assigns the branch permissions to the given role.
     *
     * @param role The role to which the branch permissions are being assigned.
     * @param branchPermissions The branch permissions to be assigned.
     * @throws BranchNotFoundException If a specified branch does not exist.
     * @throws IllegalArgumentValueException If the branch permissions string is invalid.
     */
    fun assignBranchPermissions(role: Role, branchPermissions: MutableList<String> = this.branchPermissions) {
        val branchPermissionsMap = branchPermissions.chunked(2).associate { it[0] to it[1] }

        branchPermissionsMap.forEach { (branchName, branchPermissionString) ->
            val branch = BranchIndex.getBranch(branchName)
                ?: throw BranchNotFoundException("Branch $branchName does not exist")
            val updatedBranchPermissionString = branchPermissionString.replace("'", "")

            if (!validateBranchPermissionsString(updatedBranchPermissionString)) {
                throw IllegalArgumentValueException("Invalid branch permissions string for branch $branchName")
            }

            // Remove previous branch permission if it exists, to avoid duplicates
            removeBranchPermissions(role, listOf(branchName))

            val branchPermission = BranchPermissionIndex
                .getBranchPermission(updatedBranchPermissionString, branchName)
                ?.encode(true)?.first
                ?: BranchPermission(updatedBranchPermissionString, branch.encode().first).encode(true).first

            role.permissions.add(branchPermission)
        }
    }

    /**
     * Removes the branch permissions from the given role.
     *
     * @param role The role from which the branch permissions are being removed.
     * @param removeBranchPermissions The branch permissions to be removed.
     * @throws BranchNotFoundException If a specified branch does not exist.
     */
    fun removeBranchPermissions(role: Role, removeBranchPermissions: List<String> = this.removeBranchPermissions) {
        removeBranchPermissions.forEach { branchName ->
            val branch = BranchIndex.getBranch(branchName)
                ?: throw BranchNotFoundException("Branch $branchName does not exist")

            role.permissions.removeIf { permission ->
                val isBranchPermission = permission.string.startsWith(BRANCH_PERMISSION.hash.string)

                if (isBranchPermission) {
                    val branchPermission = BranchPermission.newInstance(permission)

                    return@removeIf branchPermission.branch == branch.encode().first
                }

                return@removeIf false
            }
        }
    }

    /**
     * Validates the branch permissions string format.
     *
     * @param permissions The branch permissions string to validate.
     * @return True if the branch permissions string is valid, false otherwise.
     */
    fun validateBranchPermissionsString(permissions: String): Boolean {
        return permissions.length == 2 && permissions.toList().allIndexed { index, char ->
            val validChars = listOf('r', 'w')
            val expectedChar = validChars.getOrElse(index) { return false }
            char == expectedChar || char == '-'
        }
    }
}