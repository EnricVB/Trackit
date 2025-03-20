package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.ROLE
import dev.enric.domain.objects.Role
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * This object is responsible for managing the roles index.
 * It allows to get all the roles, check if a role already exists and get a role by its name.
 */
object RoleIndex {
    val UNDEFINED_ROLE = getRoleByName("undefined")!!
    val PROJECT_MANAGER_ROLE = getRoleByName("projectManager")!!
    val OWNER_ROLE = getRoleByName("owner")!!

    /**
     * Checks if a role with the given name already exists.
     *
     * @param name The name of the role to check.
     * @return `true` if a role with the given name exists, `false` otherwise.
     */
    fun roleAlreadyExists(name: String): Boolean {
        return getRoleByName(name) != null
    }

    /**
     * Retrieves a role by its name.
     *
     * @param name The name of the role to retrieve.
     * @return A [Role] object if found, or `null` if no role with the given name exists.
     */
    fun getRoleByName(name: String): Role? {
        getAllRoles().forEach {
            val role = Role.newInstance(it)

            if(role.name == name) {
                return role
            }
        }

        return null
    }

    /**
     * Retrieves all the roles in the repository.
     *
     * @return A list of [Hash] objects representing the roles.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getAllRoles(): List<Hash> {
        val roleFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(ROLE.hash.toString())

        return roleFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast(File.separator + ROLE.hash + File.separator))
        }.toList()
    }
}