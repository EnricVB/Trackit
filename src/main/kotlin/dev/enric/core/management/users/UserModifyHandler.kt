package dev.enric.core.management.users

import dev.enric.core.security.AuthUtil
import dev.enric.core.security.PasswordHash
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.RolePermission
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

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

    fun modifyUser() {
        val user = UserIndex.getUser(name, password)!!

        if (!newPassword.isNullOrEmpty()) {
            if (newPassword != password) {
                user.password = PasswordHash.hash(newPassword, user.salt)
            } else {
                Logger.error("New password is empty or the same as the old one")
            }
        }

        if (!newMail.isNullOrEmpty()) {
            Logger.log("Changing mail to $newMail")
            user.mail = newMail
        }

        if (!newPhone.isNullOrEmpty()) {
            Logger.log("Changing phone number to $newPhone")
            user.phone = newPhone
        }

        if (newRoleNames.isNotEmpty()) {
            val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()!!
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

    fun checkCanModifyUser(): Boolean {
        val user = UserIndex.getUser(name, password)
        if (user == null) {
            Logger.error("User does not exists")
            return false
        }

        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()
        if (sudo == null) {
            Logger.error("Sudo user not found. Please login first or use --sudo with proper credentials")
            return false
        }

        Logger.log("Logged user: ${sudo.name}")

        val canModifyUser = canModifyUsers(sudo)
        if (!canModifyUser) {
            Logger.error("User does not have permission to create users")
        }

        return canModifyUser
    }

    fun canModifyUsers(user: User): Boolean {
        user.roles.map { Role.newInstance(it) }.forEach {
            return RolePermission.newInstance(it.permissions).userOperationPermission
        }

        return false
    }

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