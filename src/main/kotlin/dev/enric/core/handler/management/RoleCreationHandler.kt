package dev.enric.core.handler.management

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.allIndexed
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.exceptions.*
import dev.enric.logger.Logger
import dev.enric.util.index.*

/**
 * RoleCreationHandler is a class that handles the creation of roles.
 *
 * It ensures that the role to be created has valid permissions and branch permissions.
 * And that the user has the necessary permissions to create the role.
 *
 * @property name The name of the role to be created.
 * @property level The level of the role to be created. 1 being the highest level.
 * @property rolePermissions A 4-character string representing the permissions of the role.
 * Each character represents a permission:
 * m (modify roles), u (user operations), s (assign new roles), a (add new roles).
 *
 * @property branchPermissions A list of branch permissions where each element contains:
 * 1. branch name
 * 2. a 2-character string representing branch permissions:
 * r (read), w (write).
 *
 * @property sudoArgs The arguments of the sudo user.
 */
class RoleCreationHandler(
    val name: String,
    val level: Int,
    val rolePermissions: String,
    val branchPermissions: MutableList<String>,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the role can be created by verifying several conditions:
     * - Whether the role already exists.
     * - Whether the role permissions are valid.
     * - Whether the branch permissions are valid.
     * - Whether the user has the necessary permissions to create a role.
     *
     * @return True if the role can be created, false otherwise.
     * @throws InvalidPermissionException If the role already exists or if the permissions are invalid.
     * @throws BranchNotFoundException If a specified branch does not exist.
     * @throws UserNotFoundException If the sudo user is not found.
     */
    fun checkCanCreateRole(): Boolean {
        roleExists()
        isValidRolePermissions(rolePermissions)
        isValidBranchPermissions()
        canCreateRole(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Verifies if the role already exists.
     *
     * @throws InvalidPermissionException If the role already exists.
     */
    private fun roleExists() {
        if (RoleIndex.roleAlreadyExists(name)) {
            throw InvalidPermissionException("Role already exists. Try a different name.")
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
     * Verifies if the user has the necessary permissions to create a role.
     *
     * @return True if the user has the necessary permissions, false otherwise.
     * @throws InvalidPermissionException If the user does not have the necessary permissions.
     */
    private fun canCreateRole(sudo: User): Boolean {
        val hasCreateRolePermission = hasCreateRolePermission(sudo)
        val hasEnoughLevel = level >= getHighestRoleLevel(sudo)

        if(!hasCreateRolePermission) {
            throw InvalidPermissionException("User does not have permission to create roles.")
        }

        if(!hasEnoughLevel) {
            throw InvalidPermissionException("User does not have enough level to create roles. " +
                                             "\nRequired level: $level, user level: ${getHighestRoleLevel(sudo)}")
        }

        return true
    }

    /**
     * Checks if the user has the permission to create roles looking at the role permissions.
     *
     * @param user The user to check the permissions.
     * @return True if the user has the permission to create roles, false otherwise.
     */
    private fun hasCreateRolePermission(user: User): Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getRolePermissions().any { it.createRolePermission }
        }
    }

    /**
     * Gets the highest role level of the user between all his roles.
     *
     * @param user The user to get the highest role level.
     * @return The highest role level of the user.
     */
    private fun getHighestRoleLevel(user: User): Int {
        return user.roles.map { Role.newInstance(it) }
            .maxOfOrNull { it.permissionLevel } ?: 0
    }

    /**
     * Creates the role with the specified name, level, and permissions.
     * - Assigns role permissions.
     * - Assigns branch permissions.
     * - Saves the role to the system.
     *
     *  @throws BranchNotFoundException If a specified branch does not exist.
     */
    fun createRole() {
        val role = Role(name, level, mutableListOf())
        val branchPermissionsMap = branchPermissions.chunked(2).associate { it[0] to it[1] }

        // Assign role permissions
        assignRolePermissions(role, rolePermissions)
        assignBranchPermissions(role, branchPermissionsMap)

        // Save role
        Logger.info("Role created")
        role.encode(true)
    }

    /**
     * Assigns the role permissions to the given role.
     *
     * In case the role permissions already exist, it assigns the existing role permissions.
     *
     * Otherwise, it creates a new role permission and assigns it.
     *
     * @param role The role to which the permissions are being assigned.
     * @param rolePermissions The role permissions string to be assigned.
     */
    fun assignRolePermissions(role: Role, rolePermissions: String) {
        val rolePermission = RolePermissionIndex.getRolePermission(rolePermissions)?.encode(true)?.first
            ?: RolePermission(rolePermissions).encode(true).first

        role.permissions.add(rolePermission)
    }

    /**
     * Assigns the branch permissions to the given role.
     *
     * @param role The role to which the branch permissions are being assigned.
     * @param branchPermissionsMap The branch permissions map to be assigned.
     * @throws BranchNotFoundException If a specified branch does not exist.
     * @throws IllegalArgumentValueException If the branch permissions string is invalid.
     */
    fun assignBranchPermissions(role: Role, branchPermissionsMap: Map<String, String>) {
        branchPermissionsMap.forEach { (branchName, branchPermissionString) ->
            val branch = BranchIndex.getBranch(branchName)
                ?: throw BranchNotFoundException("Branch $branchName does not exist")
            val updatedBranchPermissionString = branchPermissionString.replace("'", "")

            if (!validateBranchPermissionsString(updatedBranchPermissionString)) {
                throw IllegalArgumentValueException("Invalid branch permissions string for branch $branchName")
            }

            val branchPermission = BranchPermissionIndex
                .getBranchPermission(updatedBranchPermissionString, branchName)
                ?.encode(true)?.first
                ?: BranchPermission(updatedBranchPermissionString, branch.generateKey()).encode(true).first

            role.permissions.add(branchPermission)
        }
    }

    /**
     * Validates the branch permissions string format.
     *
     * Must be a 2-character string with only 'r', 'w' or '-' characters.
     *
     * @param permissions The branch permissions string to be validated.
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