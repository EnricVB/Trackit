package dev.enric.core.management.users

import dev.enric.core.Hash
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.RolePermission
import dev.enric.logger.Logger
import dev.enric.core.security.AuthUtil
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

class UserCreationHandler(
    val name: String,
    val password: String,
    val mail: String?,
    val phone: String?,
    val roleNames: Array<String>,
    val sudoArgs: Array<String>? = null
) {

    fun checkCanCreateUser(): Boolean {
        if (UserIndex.userAlreadyExists(name)) {
            Logger.error("User already exists")
            return false
        }

        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()
        if (sudo == null) {
            Logger.error("Sudo user not found. Please login first or use --sudo with proper credentials")
            return false
        }

        Logger.log("Logged user: ${sudo.name}")

        val canCreateUser = canCreateUser(sudo)
        if (!canCreateUser) {
            Logger.error("User does not have permission to create users")
        }

        return canCreateUser
    }

    fun canCreateUser(user: User): Boolean {
        user.roles.map { Role.newInstance(it) }.forEach {
            return RolePermission.newInstance(it.permissions).userOperationPermission
        }

        return false
    }

    fun createUser() {
        val sudo = UserIndex.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "") ?: AuthUtil.getLoggedUser()!!
        val roles = assignRoles(sudo)

        if (roles.isEmpty()) {
            Logger.error("No roles found, adding default role")
            roles.add(RoleIndex.UNDEFINED_ROLE)
        }

        User(
            name = name,
            password = Hash.parseText(password),
            mail = mail ?: "",
            phone = phone ?: "",
            roles = roles.map { it.encode().first }.toMutableList()
        ).encode(true)

        Logger.log("User $name created")
    }

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