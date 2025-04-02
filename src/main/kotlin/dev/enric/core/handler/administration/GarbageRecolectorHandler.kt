package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.objects.Commit
import dev.enric.logger.Logger
import dev.enric.util.index.*
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path

class GarbageRecolectorHandler : CommandHandler() {
    val repositoryPath: Path = RepositoryFolderManager().getObjectsFolderPath()

    /**
     * Removes all tags that are not associated with any commit.
     */
    fun recolectTags() {
        val complexTagFolder = repositoryPath.resolve(COMPLEX_TAG.hash.string)
        val simpleTagFolder = repositoryPath.resolve(SIMPLE_TAG.hash.string)  // Corrected to SIMPLE_TAG

        TagIndex.getUnusedTags().forEach { tag ->
            Logger.log("Removing tag $tag")

            // Remove tag from the index
            TagIndex.removeTag(tag)

            // Remove tag from the file system
            val tagFolder = if (tag.string.startsWith(COMPLEX_TAG.hash.string)) complexTagFolder else simpleTagFolder
            val tagFile = tagFolder.resolve(tag.string).toFile()

            if (tagFile.exists()) {
                tagFile.deleteRecursively()
                Logger.log("Tag $tag removed successfully from the filesystem.")
            } else {
                Logger.warning("Tag $tag does not exist in the filesystem.")
            }
        }
    }

    /**
     * Removes all commits that are not associated with the current branch.
     */
    fun recolectCommits() {
        val currentBranch = BranchIndex.getCurrentBranch()
        val commitFolder = repositoryPath.resolve(COMMIT.hash.string)

        CommitIndex.getAllCommit().forEach { commitHash ->
            val commit = Commit.newInstance(commitHash)

            // Only remove commits not on the current branch
            if (commit.branch != currentBranch.generateKey()) {
                Logger.log("Removing commit $commitHash")

                val commitFile = commitFolder.resolve(commitHash.string).toFile()

                if (commitFile.exists()) {
                    commitFile.deleteRecursively()
                    Logger.log("Commit $commitHash removed successfully.")
                } else {
                    Logger.warning("Commit $commitHash does not exist in the filesystem.")
                }
            }
        }
    }

    /**
     * Removes all unused permissions (role and branch) from the repository.
     */
    fun recolectPermissions() {
        val rolePermissionFolder = repositoryPath.resolve(ROLE_PERMISSION.hash.string)
        val branchPermissionFolder = repositoryPath.resolve(BRANCH_PERMISSION.hash.string)

        RolePermissionIndex.getUnusedPermissions().forEach { permission ->
            Logger.log("Removing role permission $permission")

            val permissionFile = rolePermissionFolder.resolve(permission.string).toFile()

            if (permissionFile.exists()) {
                permissionFile.deleteRecursively()
                Logger.log("Role permission $permission removed successfully.")
            } else {
                Logger.warning("Role permission $permission does not exist in the filesystem.")
            }
        }

        BranchPermissionIndex.getUnusedPermissions().forEach { permission ->
            Logger.log("Removing branch permission $permission")

            val permissionFile = branchPermissionFolder.resolve(permission.string).toFile()

            if (permissionFile.exists()) {
                permissionFile.deleteRecursively()
                Logger.log("Branch permission $permission removed successfully.")
            } else {
                Logger.warning("Branch permission $permission does not exist in the filesystem.")
            }
        }
    }

    /**
     * Removes all unused roles from the repository.
     */
    fun recolectRoles() {
        val roleFolder = repositoryPath.resolve(ROLE.hash.string)

        RoleIndex.getUnusedRoles().forEach { role ->
            Logger.log("Removing role $role")

            val roleFile = roleFolder.resolve(role.string).toFile()

            if (roleFile.exists()) {
                roleFile.deleteRecursively()
                Logger.log("Role $role removed successfully.")
            } else {
                Logger.warning("Role $role does not exist in the filesystem.")
            }
        }
    }
}