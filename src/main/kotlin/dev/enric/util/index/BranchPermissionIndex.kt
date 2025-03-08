package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.Hash.HashType.ROLE_PERMISSION
import dev.enric.domain.Branch
import dev.enric.domain.permission.BranchPermission
import dev.enric.domain.permission.RolePermission
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object BranchPermissionIndex {

    fun getRolePermission(permissions: String, branchName: String): RolePermission? {
        getBranchPermissionsByBranch(branchName).forEach {
            val rolePermission = RolePermission.newInstance(it)

            val permissionString = permissions.take(4)
            val validChars = listOf('r', 'w')

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
    fun getAllBranchPermissions(): List<Hash> {
        val branchPermissionFolder =
            RepositoryFolderManager().getObjectsFolderPath().resolve(BRANCH_PERMISSION.hash.toString())

        return branchPermissionFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + ROLE_PERMISSION.hash + "\\"))
        }.toList()
    }

    @OptIn(ExperimentalPathApi::class)
    fun getBranchPermissionsByBranch(branchName: String): List<Hash> {
        val branchPermissionFolder =
            RepositoryFolderManager().getObjectsFolderPath().resolve(BRANCH_PERMISSION.hash.toString())

        return branchPermissionFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + ROLE_PERMISSION.hash + "\\"))
        }.filter {
            val branchHash = BranchPermission.newInstance(it).branch
            val branch = Branch.newInstance(branchHash)

            branch.name == branchName
        }.toList()
    }
}