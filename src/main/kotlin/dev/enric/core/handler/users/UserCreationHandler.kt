package dev.enric.core.handler.users

import dev.enric.core.Hash
import dev.enric.core.objects.Role
import dev.enric.core.objects.User
import dev.enric.logger.Logger
import dev.enric.util.AuthUtil
import dev.enric.util.RoleUtil

class UserCreationHandler(
    val name: String,
    val password: String,
    val mail: String?,
    val phone: String?,
    val roleNames: Array<String>,
    val sudoArgs: Array<String>? = null
) {

    fun checkCanCreateUser(): Boolean {
        if (AuthUtil.authenticate(name, password)) {
            Logger.error("User already exists")
            return false
        }

        val sudo = AuthUtil.getLoggedUser() ?: AuthUtil.getUser(sudoArgs?.get(0) ?: "", sudoArgs?.get(1) ?: "")
        if (sudo == null) {
            Logger.error("Sudo user not found. Please login first or use --sudo with proper credentials")
            return false
        }

        Logger.log("Logged user: ${sudo.name}")
        return true
    }

    fun createUser() {
        val roles : MutableList<Role> = roleNames.mapNotNull { RoleUtil.getRoleByName(it) }.toMutableList()
        if(roles.isEmpty()) {
            Logger.error("No roles found, adding default role")
            roles.add(RoleUtil.UNDEFINED_ROLE)

            return
        }

        User(
            name = name,
            password = Hash.parseText(password),
            mail = mail ?: "",
            phone = phone ?: "",
            roles = roles.map { it.encode().first }
        ).encode(true)

        Logger.log("User $name created")
    }
}