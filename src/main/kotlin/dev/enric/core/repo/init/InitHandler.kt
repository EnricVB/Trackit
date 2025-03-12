package dev.enric.core.repo.init

import dev.enric.core.Hash
import dev.enric.domain.Branch
import dev.enric.domain.Role
import dev.enric.domain.User
import dev.enric.domain.permission.BranchPermission
import dev.enric.domain.permission.RolePermission
import dev.enric.util.repository.RepositoryFolderManager

/**
 * InitHandler is an object that handles the initialization of a new repository.
 * It is responsible for creating the necessary repository folders, default roles, the first user, and the main branch.
 */
object InitHandler {


    /**
     * Initializes a new repository.
     * The following actions are performed:
     * - Creates the repository folder.
     * - Creates the default roles.
     * - Creates the first user with the owner role.
     * - Creates the main branch of the repository.
     */
    fun init() {
        RepositoryFolderManager().createRepositoryFolder()

        val owner = createDefaultRoles()
        createUser(owner)
    }

    /**
     * Creates the default roles needed for the repository.
     * This includes the 'owner', 'projectManager', and 'undefined' roles.
     * The permissions for each role are defined in the [RolePermission] class.
     *
     * @return The 'owner' role, which is used in the creation of the first user.
     */
    private fun createDefaultRoles(): Role {
        val ownerPermissions = RolePermission("musa").encode(true).first
        val projectManagerPermissions = RolePermission("musa").encode(true).first
        val undefinedPermissions = RolePermission("----").encode(true).first

        val mainBranch = createMainBranch()
        val mainBranchPermissions = BranchPermission("rw", mainBranch).encode(true).first

        val owner = Role("owner", 1, mutableListOf(ownerPermissions, mainBranchPermissions)).encode(true).first
        Role("projectManager", 2, mutableListOf(projectManagerPermissions, mainBranch)).encode(true).first
        Role("undefined", Int.MAX_VALUE, mutableListOf(undefinedPermissions)).encode(true).first

        return Role.newInstance(owner)
    }

    /**
     * Creates the first user with the provided role.
     *
     * @param owner The role to assign to the new user.
     * @return The hash of the created user.
     */
    private fun createUser(owner: Role): Hash {
        return User.createUser(mutableListOf(owner)).encode(true).first
    }

    /**
     * Creates the main branch 'main' in the repository.
     *
     * @return A [Hash] object representing the main branch.
     */
    private fun createMainBranch() : Hash {
        return Branch("main").encode(true).first
    }
}