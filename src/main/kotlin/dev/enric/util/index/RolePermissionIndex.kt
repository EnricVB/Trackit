package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.ROLE_PERMISSION
import dev.enric.domain.permission.RolePermission
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object RolePermissionIndex {

    fun getRolePermission(permissions: String): RolePermission? {
        getAllRolePermissions().forEach {
            val rolePermission = RolePermission.newInstance(it)

            val permissionString = permissions.take(4)
            val validChars = listOf('m', 'u', 's', 'a')

            val properties = listOf(
                rolePermission::createRolePermission,
                rolePermission::modifyRolePermission,
                rolePermission::assignRolePermission,
                rolePermission::userOperationPermission
            )

            permissionString.forEachIndexed { index, char ->
                when (char) {
                    validChars[index] -> properties[index].set(true)
                    '-' -> properties[index].set(false)
                    else -> throw IllegalArgumentException("Invalid permission at position ${index + 1}: $char")
                }
            }

            if (rolePermission.toString().take(4) == permissions) {
                return rolePermission
            }
        }

        return null
    }


    @OptIn(ExperimentalPathApi::class)
    fun getAllRolePermissions(): List<Hash> {
        val rolePermissionFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(ROLE_PERMISSION.hash.toString())

        return rolePermissionFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + ROLE_PERMISSION.hash + "\\"))
        }.toList()
    }
}