package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.ROLE_PERMISSION
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * This object is responsible for managing the role permissions index.
 * It allows to get all the role permissions, check if a role permission already exists and get a role permission by its name.
 */
object RolePermissionIndex {

    /**
     * Retrieves a role permission by its permissions string.
     *
     * @param permissions A 4-character string indicating the role permissions.
     *                    Example: "musa" for modify, user, assign, and user operation permissions.
     * @return A [RolePermission] object if found, or `null` if no role permission with the given permissions exists.
     * @throws IllegalArgumentException If the permission string contains invalid characters.
     */
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


    /**
     * Retrieves all the role permissions in the repository.
     *
     * @return A list of [Hash] objects representing the role permissions.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getAllRolePermissions(): List<Hash> {
        val rolePermissionFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(ROLE_PERMISSION.hash.toString())

        return rolePermissionFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast(File.separator + ROLE_PERMISSION.hash + File.separator))
        }.toList()
    }
}