package dev.enric.core.repo.init

import dev.enric.core.Hash
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.RolePermission
import dev.enric.util.repository.RepositoryFolderManager

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

        val owner = Role("owner", 1, ownerPermissions).encode(true).first
        Role("projectManager", 2, projectManagerPermissions).encode(true).first
        Role("undefined", Int.MAX_VALUE, undefinedPermissions).encode(true).first

        return Role.newInstance(owner)
    }

    private fun createUser(owner: Role): Hash {
        return User.createUser(mutableListOf(owner)).encode(true).first
    }
}