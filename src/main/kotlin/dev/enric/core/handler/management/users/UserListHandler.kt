package dev.enric.core.handler.management.users

import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.util.index.UserIndex

/**
 * UserListHandler is responsible for retrieving and displaying a list of all users in the system.
 * It checks if there are any users available and prints their information.
 */
class UserListHandler {

    /**
     * Lists all users in the system. If no users are found, logs an error message.
     * Otherwise, it prints the information of each user to the console.
     */
    fun listUsers() {
        // Retrieve all users from the UserIndex
        val users = UserIndex.getAllUsers()

        // If no users are found, log an error message
        if (users.isEmpty()) {
            Logger.error("No users found")
            return
        }

        // Iterate over each user, creating an instance and printing their information
        users.forEach { Logger.info(User.newInstance(it).printInfo()) }
    }
}