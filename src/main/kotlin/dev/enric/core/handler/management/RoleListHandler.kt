package dev.enric.core.handler.management

import dev.enric.domain.objects.Role
import dev.enric.exceptions.RoleNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.index.RoleIndex

/**
 * RoleListHandler is a class responsible for handling operations related to listing roles in the system.
 *
 * It retrieves all the roles stored in the system and prints their information.
 * If no roles are found, an exception is thrown.
 */
class RoleListHandler {

    /**
     * Lists all the roles in the system.
     *
     * It retrieves the list of roles from the RoleIndex. If no roles are found, a `RoleNotFoundException`
     * is thrown. Otherwise, it prints the information of each role.
     *
     * @throws RoleNotFoundException if no roles are found in the system.
     */
    fun listRoles() {
        // Retrieve all roles from the RoleIndex
        val roles = RoleIndex.getAllRoles()

        // If no roles are found, throw an exception
        if (roles.isEmpty()) {
            throw RoleNotFoundException("No roles found.")
        }

        // Print the information of each role
        roles.forEach { Logger.info(Role.newInstance(it).printInfo()) }
    }
}