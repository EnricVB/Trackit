package dev.enric.core.management.roles

import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex
import dev.enric.util.index.UserIndex

class RoleListHandler {

    fun listRoles() {
        val roles = RoleIndex.getAllRoles()
        if (roles.isEmpty()) {
            Logger.error("No roles found")
            return
        }

        roles.forEach {
            val role = Role.newInstance(it)

            println(role.printInfo())
        }
    }
}