package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH_PERMISSION
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
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

            require(permissions.length == 2 && permissions.all { ch -> ch in listOf('r', 'w', '-') }) {
                "Invalid permission format. Use 'r', 'w' or '-' in a 2-character string."
            }

            return branchPermissions
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
            Hash(it.toString().substringAfterLast(File.separator + BRANCH_PERMISSION.hash + File.separator))
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
            Hash(it.toString().substringAfterLast(File.separator + BRANCH_PERMISSION.hash + File.separator))
        }.filter {
            val branchHash = BranchPermission.newInstance(it).branch
            val branch = Branch.newInstance(branchHash)

            branch.name == branchName
        }.toList()
    }

    /**
     * Retrieves all the branch permissions that are not being used by any user.
     *
     * @return A list of [Hash] objects representing the unused branch permissions.
     */
    fun getUnusedPermissions(): List<Hash> {
        val usedPermissions = UserIndex.getAllUsers().asSequence().map { User.newInstance(it) }
            .flatMap { user -> user.roles.asSequence() }.map { Role.newInstance(it) }
            .flatMap { role -> role.getBranchPermissions().asSequence() }.toSet()

        return getAllBranchPermissions().filterNot { usedPermissions.contains(BranchPermission.newInstance(it)) }
    }
}
