package dev.enric.util

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.ROLE
import dev.enric.core.objects.Role
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object RoleUtil {
    val UNDEFINED_ROLE = getRoleByName("undefined")!!
    val PROJECT_MANAGER_ROLE = getRoleByName("projectManager")!!
    val OWNER_ROLE = getRoleByName("owner")!!

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