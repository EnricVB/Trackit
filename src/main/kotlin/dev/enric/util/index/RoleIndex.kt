package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.ROLE
import dev.enric.domain.Role
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object RoleIndex {
    val UNDEFINED_ROLE = getRoleByName("undefined")!!
    val PROJECT_MANAGER_ROLE = getRoleByName("projectManager")!!
    val OWNER_ROLE = getRoleByName("owner")!!

    fun roleAlreadyExists(name: String): Boolean {
        return getRoleByName(name) != null
    }

    fun getRoleByName(name: String): Role? {
        getAllRoles().forEach {
            val role = Role.newInstance(it)

            if(role.name == name) {
                return role
            }
        }

        return null
    }

    @OptIn(ExperimentalPathApi::class)
    fun getAllRoles(): List<Hash> {
        val roleFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(ROLE.hash.toString())

        return roleFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + ROLE.hash + "\\"))
        }.toList()
    }
}