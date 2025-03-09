package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.Hash.HashType.ROLE_PERMISSION
import dev.enric.domain.Branch
import dev.enric.domain.permission.BranchPermission
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * This object is responsible for managing the branch permissions index.
 * It allows to get all the branch permissions, check if a branch permission already exists and get a branch permission by its name.
 */
object BranchPermissionIndex {

    /**
     * Retrieves a branch permission by its name.
     *
     * @param branchName The name of the branch to retrieve.
     * @return A [BranchPermission] object if found, or `null` if no branch permission with the given name exists.
     */
    fun getBranchPermission(permissions: String, branchName: String): BranchPermission? {
        getBranchPermissionsByBranch(branchName).forEach {
            val branchPermissions = BranchPermission.newInstance(it)

            if (permissions.length != 2 || permissions.any { ch -> ch !in listOf('r', 'w', '-') }) {
                throw IllegalArgumentException("Invalid permission format. Use 'r', 'w' or '-' in a 2-character string.")
            }

            if (branchPermissions.toString().take(2) == permissions) {
                return branchPermissions
            }
        }

        return null
    }

    /**
     * Retrieves all the branch permissions in the repository.
     *
     * @return A list of [Hash] objects representing the branch permissions.
     */
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

    /**
     * Retrieves all the branch permissions for a specific branch by its name.
     *
     * @param branchName The name of the branch to retrieve permissions for.
     * @return A list of [Hash] objects representing the branch permissions for the given branch.
     */
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
