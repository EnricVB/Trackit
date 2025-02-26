package dev.enric.core.handler.users

import dev.enric.core.objects.User
import dev.enric.logger.Logger
import dev.enric.util.AuthUtil

class UserListHandler {

    fun listUsers() {
        val users = AuthUtil.getAllUsers()
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