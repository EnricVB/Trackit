package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.objects.Commit
import dev.enric.logger.Logger
import dev.enric.util.index.*
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path

/**
 * Handler responsible for collecting and removing unused or orphaned items from the repository.
 * This includes tags, commits, permissions, and roles that are not in use or not associated
 * with the current branch.
 */
class GarbageRecolectorHandler : CommandHandler() {
    val repositoryPath: Path = RepositoryFolderManager().getObjectsFolderPath()

    /**
     * Removes all tags that are not associated with any commit.
     * This operation ensures that orphaned tags are removed from both the index and the filesystem.
     */
    fun recolectTags() {
        val complexTagFolder = repositoryPath.resolve(COMPLEX_TAG.hash.string)
        val simpleTagFolder = repositoryPath.resolve(SIMPLE_TAG.hash.string)  // Corrected to SIMPLE_TAG

        // Iterate over unused tags and remove them from the index and filesystem
        TagIndex.getUnusedTags().forEach { tag ->
            Logger.debug("Removing tag $tag from index")

            // Remove tag from the index
            TagIndex.removeTag(tag)

            // Remove tag from the file system
            val tagFolder = if (tag.string.startsWith(COMPLEX_TAG.hash.string)) complexTagFolder else simpleTagFolder
            val tagFile = tagFolder.resolve(tag.string).toFile()

            if (tagFile.exists()) {
                tagFile.deleteRecursively()
                Logger.info("Tag $tag removed successfully from the filesystem.")
            } else {
                Logger.warning("Tag $tag does not exist in the filesystem.")
            }
        }
    }

    /**
     * Removes all commits that are not associated with the current branch.
     * This operation helps maintain a clean repository by deleting orphaned commits.
     */
    fun recolectCommits() {
        val currentBranch = BranchIndex.getCurrentBranch()
        val commitFolder = repositoryPath.resolve(COMMIT.hash.string)

        // Iterate over all commits and remove those not associated with the current branch
        CommitIndex.getAllCommit().forEach { commitHash ->
            val commit = Commit.newInstance(commitHash)

            // Only remove commits not on the current branch
            if (commit.branch != currentBranch.generateKey()) {
                Logger.debug("Removing commit $commitHash from index")

                val commitFile = commitFolder.resolve(commitHash.string).toFile()

                if (commitFile.exists()) {
                    commitFile.deleteRecursively()
                    Logger.info("Commit $commitHash removed successfully.")
                } else {
                    Logger.warning("Commit $commitHash does not exist in the filesystem.")
                }
            }
        }
    }

    /**
     * Removes all unused permissions (role and branch) from the repository.
     * This operation clears any permissions that are no longer in use.
     */
    fun recolectPermissions() {
        val rolePermissionFolder = repositoryPath.resolve(ROLE_PERMISSION.hash.string)
        val branchPermissionFolder = repositoryPath.resolve(BRANCH_PERMISSION.hash.string)

        // Iterate over unused role permissions and remove them
        RolePermissionIndex.getUnusedPermissions().forEach { permission ->
            Logger.debug("Removing role permission $permission from index")

            val permissionFile = rolePermissionFolder.resolve(permission.string).toFile()

            if (permissionFile.exists()) {
                permissionFile.deleteRecursively()
                Logger.info("Role permission $permission removed successfully.")
            } else {
                Logger.warning("Role permission $permission does not exist in the filesystem.")
            }
        }

        // Iterate over unused branch permissions and remove them
        BranchPermissionIndex.getUnusedPermissions().forEach { permission ->
            Logger.debug("Removing branch permission $permission from index")

            val permissionFile = branchPermissionFolder.resolve(permission.string).toFile()

            if (permissionFile.exists()) {
                permissionFile.deleteRecursively()
                Logger.info("Branch permission $permission removed successfully.")
            } else {
                Logger.warning("Branch permission $permission does not exist in the filesystem.")
            }
        }
    }

    /**
     * Removes all unused roles from the repository.
     * This operation helps clean up orphaned roles that are no longer needed.
     */
    fun recolectRoles() {
        val roleFolder = repositoryPath.resolve(ROLE.hash.string)

        // Iterate over unused roles and remove them from both the index and filesystem
        RoleIndex.getUnusedRoles().forEach { role ->
            Logger.debug("Removing role $role from index")

            val roleFile = roleFolder.resolve(role.string).toFile()

            if (roleFile.exists()) {
                roleFile.deleteRecursively()
                Logger.info("Role $role removed successfully.")
            } else {
                Logger.warning("Role $role does not exist in the filesystem.")
            }
        }
    }
}
