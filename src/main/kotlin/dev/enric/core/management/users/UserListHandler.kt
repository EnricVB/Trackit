package dev.enric.core.management.users

import dev.enric.domain.User
import dev.enric.logger.Logger
import dev.enric.util.index.UserIndex

class UserListHandler {

    fun listUsers() {
        val users = UserIndex.getAllUsers()
        if (users.isEmpty()) {
            Logger.error("No users found")
            return
        }

        users.forEach {
            val user = User.newInstance(it)

            println(user.printInfo())
        }
    }
}