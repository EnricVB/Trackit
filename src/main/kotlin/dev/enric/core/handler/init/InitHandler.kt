package dev.enric.core.handler.init

import dev.enric.core.Hash
import dev.enric.core.objects.Role
import dev.enric.core.objects.User
import dev.enric.core.objects.permission.RolePermission
import dev.enric.util.RepositoryFolderManager
import java.util.*

object InitHandler {

    fun init() {
        RepositoryFolderManager().createRepositoryFolder()

        val owner = createDefaultRoles()
        createUser(owner)
    }

    /**
     * Create the default roles for the repository.
     * @see Role
     * @return The owner role so it can be used in the creation of the first user.
     */
    private fun createDefaultRoles(): Role {
        val ownerPermissions = RolePermission("musa").encode(true).first
        val projectManagerPermissions = RolePermission("musa").encode(true).first
        val undefinedPermissions = RolePermission("----").encode(true).first

        val owner = Role("owner", 1, mutableListOf(ownerPermissions)).encode(true).first
        Role("projectManager", 2, mutableListOf(projectManagerPermissions)).encode(true).first
        Role("undefined", Int.MAX_VALUE, mutableListOf(undefinedPermissions)).encode(true).first

        return Role.newInstance(owner)
    }

    private fun createUser(owner: Role): Hash {
        val console = System.console()

        val username: String
        val mail: String
        val phone: String
        val password: String

        if (console != null) { // This is running in a terminal
            username = console.readLine("Enter username: ")
            mail = console.readLine("Enter mail: ")
            phone = console.readLine("Enter phone: ")
            password = String(console.readPassword("Enter password: "))
        } else { // This is running in an IDE
            val scanner = Scanner(System.`in`)
            println("Enter username: ")
            username = scanner.nextLine()
            println("Enter mail: ")
            mail = scanner.nextLine()
            println("Enter phone: ")
            phone = scanner.nextLine()
            println("Enter password: ")
            password = scanner.nextLine()
        }

        return User(username, Hash.parseText(password), mail, phone, owner.encode().first).encode(true).first
    }
}