package dev.enric.core.management.roles

import dev.enric.core.security.AuthUtil
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.BranchPermission
import dev.enric.domain.permission.RolePermission
import dev.enric.logger.Logger
import dev.enric.util.index.*

class RoleCreationHandler(
    val name: String,
    val level: Int,
    val rolePermissions: String,
    val branchPermissions: MutableList<String>,
    val sudoArgs: Array<String>? = null
) {

    fun checkCanCreateRole(): Boolean {
        if (RoleIndex.roleAlreadyExists(name)) {
            Logger.error("Role already exists")
            return false
        }

        if (!validateRolePermissionsString(rolePermissions)) {
            Logger.error("Invalid role permissions string")
            return false
        }

        if (branchPermissions.size % 2 != 0) {
            throw IllegalArgumentException("Each --branch-permission must have exactly two arguments: <branch> <permission>")
        }

        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()
        if (sudo == null) {
            Logger.error("Sudo user not found. Please login first or use --sudo with proper credentials")
            return false
        }

        Logger.log("Logged user: ${sudo.name}")

        val canCreateRoles = canCreateRole(sudo)
        if (!canCreateRoles) {
            Logger.error("User does not have permission to create roles")
        }

        return canCreateRoles
    }

    fun validateRolePermissionsString(permissions: String): Boolean {
        if (permissions.length != 4) {
            Logger.error("Invalid permissions string: The string must be exactly 4 characters long.")
            return false
        }

        val positions =
            listOf(permissions[0] to 'm', permissions[1] to 'u', permissions[2] to 's', permissions[3] to 'a')

        for ((index, expected) in positions.withIndex()) {
            val (char, validChar) = expected
            if (char != validChar && char != '-') {
                Logger.error("Invalid permission at position ${index + 1}: '$char'. Expected '$validChar' or '-'.")
                return false
            }
        }

        return true
    }

    fun validateBranchPermissionsString(permissions: String): Boolean {
        if (permissions.length != 2) {
            Logger.error("Invalid permissions string: The string must be exactly 2 characters long.")
            return false
        }

        val positions = listOf(permissions[0] to 'r', permissions[1] to 'w')

        for ((index, expected) in positions.withIndex()) {
            val (char, validChar) = expected

            if (char != validChar && char != '-') {
                Logger.error("Invalid permission at position ${index + 1}: '$char'. Expected '$validChar' or '-'.")
                return false
            }
        }

        return true
    }

    fun canCreateRole(user: User): Boolean {
        var userHighestRoleLevel = 0
        var hasCreateRolePermission = false

        user.roles.map { Role.newInstance(it) }.forEach { role ->
            role.getRolePermissions().forEach { rolePermission ->
                if (rolePermission.createRolePermission) {
                    hasCreateRolePermission = true
                }
            }

            if (role.permissionLevel > userHighestRoleLevel) {
                userHighestRoleLevel = role.permissionLevel
            }
        }

        return hasCreateRolePermission && level <= userHighestRoleLevel
    }

    fun createRole() {
        val role = Role(name, level, mutableListOf())
        assignRolePermissions(role, rolePermissions)

        val branchPermissionsMap = branchPermissions.chunked(2).associate { it[0] to it[1] }
        assignBranchPermissions(role, branchPermissionsMap)

        Logger.log("Role created")

        role.encode(true)
    }

    /**
     * Assigns role permissions to the role.
     *
     * @param role The role to assign the role permissions
     * @param rolePermissions The role permissions to assign to the role
     * @see RolePermission
     */
    fun assignRolePermissions(role: Role, rolePermissions: String) {
        val rolePermission =
            RolePermissionIndex.getRolePermission(rolePermissions)?.encode(true)?.first ?:
            RolePermission(rolePermissions).encode(true).first

        role.permissions.add(rolePermission)
    }

    /**
     * Assigns branch permissions to the role.
     *
     * @param role The role to assign the branch permissions
     * @param branchPermissionsMap The map of branch permissions to assign to the role
     * @see BranchPermission
     */
    fun assignBranchPermissions(role: Role, branchPermissionsMap: Map<String, String>) {
        for((branchName, branchPermissionString) in branchPermissionsMap) {
            val branch = BranchIndex.getBranch(branchName) ?: throw IllegalArgumentException("Branch $branchName does not exist")
            val updatedBranchPermissionString = branchPermissionString.replace("'", "")

            if(!validateBranchPermissionsString(updatedBranchPermissionString)) {
                throw IllegalArgumentException("Invalid branch permissions string for branch $branchName")
            }

            val branchPermission =
                BranchPermissionIndex.getBranchPermission(updatedBranchPermissionString, branchName)?.encode(true)?.first ?:
                BranchPermission(updatedBranchPermissionString, branch.encode().first).encode(true).first

            role.permissions.add(branchPermission)
        }
    }
}