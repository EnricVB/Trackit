package dev.enric.core.management.roles

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.security.AuthUtil
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.BranchPermission
import dev.enric.domain.permission.RolePermission
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.exceptions.InvalidBranchPermissionException
import dev.enric.logger.Logger
import dev.enric.util.index.*
import javax.management.relation.RoleNotFoundException

class RoleModifyHandler(
    val name: String,
    val level: Int,
    val rolePermissions: String,
    val branchPermissions: MutableList<String>,
    val removeBranchPermissions: MutableList<String>,
    val overwrite: Boolean,
    val sudoArgs: Array<String>? = null
) {
    fun checkCanModifyRole(): Boolean {
        return when {
            !roleDoesntExists() -> false
            !isValidRolePermissions() -> false
            !isValidBranchPermissions() -> false
            !isValidSudoUser() -> false
            !canModifyRole() -> false
            else -> true
        }
    }

    private fun roleDoesntExists(): Boolean {
        if (!RoleIndex.roleAlreadyExists(name)) {
            Logger.error("Role does not exists")
            return false
        }

        return true
    }

    private fun isValidRolePermissions(): Boolean {
        if (!validateRolePermissionsString(rolePermissions)) {
            Logger.error("Invalid role permissions string")
            return false
        }

        return true
    }

    private fun isValidBranchPermissions(): Boolean {
        if (branchPermissions.size % 2 != 0) {
            Logger.error("Each --branch-permission must have exactly two arguments: <branch> <permission>")
            return false
        }

        return true
    }

    private fun isValidSudoUser(): Boolean {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()
        return if (sudo == null) {
            Logger.error("Sudo user not found. Please login first or use --sudo with proper credentials")
            false
        } else {
            Logger.log("Logged user: ${sudo.name}")
            true
        }
    }

    private fun canModifyRole(): Boolean {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()!!
        val hasModifyRolePermission = hasModifyRolePermission(sudo)
        val hasEnoughLevel = level >= getHighestRoleLevel(sudo)

        if(!hasModifyRolePermission) {
            Logger.error("User does not have permission to modify roles.")
        }

        if(!hasEnoughLevel) {
            Logger.error("User does not have enough level to modify roles. Required level: $level, user level: ${getHighestRoleLevel(sudo)}")
        }

        return hasModifyRolePermission && hasEnoughLevel
    }

    private fun hasModifyRolePermission(user: User): Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getRolePermissions().any { it.modifyRolePermission }
        }
    }

    private fun getHighestRoleLevel(user: User): Int {
        return user.roles.map { Role.newInstance(it) }
            .maxOfOrNull { it.permissionLevel } ?: 0
    }

    fun modifyRole() {
        val role = RoleIndex.getRoleByName(name) ?: throw RoleNotFoundException("Role $name does not exist")

        if (overwrite) {
            role.permissions.clear()
            Logger.log("Role permissions overwritten")
        }

        assignRolePermissions(role)
        assignBranchPermissions(role)
        removeBranchPermissions(role)

        Logger.log("Role modified successfully")

        role.encode(true)
    }

    fun assignRolePermissions(role: Role, rolePermissions: String = this.rolePermissions) {
        // Remove all role permissions to replace them with the new ones
        role.permissions.removeAll(role.getRolePermissions().map { it.encode().first })

        val rolePermission = RolePermissionIndex.getRolePermission(rolePermissions)?.encode(true)?.first
            ?: RolePermission(rolePermissions).encode(true).first

        role.permissions.add(rolePermission)
    }

    fun assignBranchPermissions(role: Role, branchPermissions: MutableList<String> = this.branchPermissions) {
        val branchPermissionsMap = branchPermissions.chunked(2).associate { it[0] to it[1] }

        branchPermissionsMap.forEach { (branchName, branchPermissionString) ->
            val branch = BranchIndex.getBranch(branchName)
                ?: throw BranchNotFoundException("Branch $branchName does not exist")
            val updatedBranchPermissionString = branchPermissionString.replace("'", "")

            if (!validateBranchPermissionsString(updatedBranchPermissionString)) {
                throw InvalidBranchPermissionException("Invalid branch permissions string for branch $branchName")
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

    fun validateRolePermissionsString(permissions: String): Boolean {
        return permissions.length == 4 && permissions.toList().allIndexed { index, char ->
            val validChars = listOf('m', 'u', 's', 'a')
            val expectedChar = validChars.getOrElse(index) { return false }
            char == expectedChar || char == '-'
        }
    }

    fun validateBranchPermissionsString(permissions: String): Boolean {
        return permissions.length == 2 && permissions.toList().allIndexed { index, char ->
            val validChars = listOf('r', 'w')
            val expectedChar = validChars.getOrElse(index) { return false }
            char == expectedChar || char == '-'
        }
    }
}

private inline fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
    return this.withIndex().all { (index, item) -> predicate(index, item) }
}