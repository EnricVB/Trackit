package dev.enric.core.handler.branch

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH_PERMISSION
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.exceptions.BranchNotFoundException
import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.index.*
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.deleteIfExists

/**
 * Handles branch-related operations such as creating and removing branches.
 *
 * @property workingBranch The current branch being worked on.
 * @property sudoArgs Optional sudo credentials to validate permissions.
 */
class BranchHandler(
    private val workingBranch: Branch?,
    private val sudoArgs: Array<String>?
) : CommandHandler() {

    /**
     * Creates a new branch forked from the current working branch.
     *
     * @param branchName The name of the new branch to be created.
     * @throws BranchNotFoundException if no working branch is selected.
     * @throws InvalidPermissionException if the user lacks read permissions.
     * @throws IllegalStateException if the branch already exists.
     */
    fun createBranch(branchName: String) {
        val user = isValidSudoUser(sudoArgs)

        checkNotNull(workingBranch) { throw BranchNotFoundException("You are not working on a branch.") }

        if (!hasReadPermissionOnBranch(user, workingBranch.generateKey())) {
            throw InvalidPermissionException(
                "You need read permissions over ${workingBranch.generateKey().abbreviate()} to create a new fork."
            )
        }

        if (BranchIndex.exists(branchName)) {
            throw IllegalStateException("Branch $branchName already exists.")
        }

        // Create and encode the new branch
        val newBranch = Branch(branchName).apply { encode(true) }

        // Copy permissions from the source branch
        inheritBranchPermissions(newBranch, workingBranch)

        // Create a dummy commit pointing to the new branch
        val commit = CommitIndex.getCurrentCommit().let { previousCommit ->
            Commit(
                previousCommit = previousCommit?.generateKey() ?: Hash.empty32(),
                tree = previousCommit?.tree ?: mutableListOf(),
                branch = newBranch.generateKey(),
                author = user.generateKey(),
                confirmer = user.generateKey(),
                title = "Created new Branch $branchName from ${previousCommit?.branch?.abbreviate()}",
                message = "This is an automated commit."
            )
        }.encode(true).first

        // Tag the initial commit
        TagIndex.addTagToCommit(SimpleTag("NewBranch").encode(true).first, commit)

        // Set new branch head
        BranchIndex.setBranchHead(newBranch.generateKey(), commit)

        Logger.log("New branch created with hash ${newBranch.generateKey()}")
    }

    /**
     * Deletes the current working branch.
     *
     * @throws BranchNotFoundException if no working branch is selected.
     * @throws InvalidPermissionException if the user lacks write permissions.
     * @throws IllegalStateException if trying to delete the last branch.
     */
    fun removeBranch() {
        val branchFolder = RepositoryFolderManager().getObjectsFolderPath()
            .resolve(BRANCH_PERMISSION.hash.string)

        val files = branchFolder.toFile().listFiles()
        if (files == null || files.size <= 1) {
            throw IllegalStateException("You cannot delete the last branch.")
        }

        checkNotNull(workingBranch) { throw BranchNotFoundException("You are not working on a branch.") }

        val user = isValidSudoUser(sudoArgs)

        if (!hasWritePermissionOnBranch(user, workingBranch.generateKey())) {
            throw InvalidPermissionException(
                "You need write permissions over ${workingBranch.generateKey().abbreviate()} to delete this branch."
            )
        }

        // Delete the branch file
        val branchFile = branchFolder.resolve(workingBranch.generateKey().toString())
        branchFile.deleteIfExists()
    }

    /**
     * Inherits permissions from the original branch to the newly created one.
     *
     * @param newBranch The new branch receiving the permissions.
     * @param oldBranch The original branch from which to copy permissions.
     */
    fun inheritBranchPermissions(newBranch: Branch, oldBranch: Branch) {
        val oldPermissions = BranchPermissionIndex.getBranchPermissionsByBranch(oldBranch.name)

        UserIndex.getAllUsers().forEach { userHash ->
            val user = User.newInstance(userHash)

            // For each role, check if has permissions for oldBranch
            // if it has, copy the previous branch permissions to newBranch
            user.roles.forEach { roleHash ->
                val role = Role.newInstance(roleHash)

                role.permissions.filter { it in oldPermissions }.forEach { matchingPermission ->
                    val permission = BranchPermission.newInstance(matchingPermission)

                    val newPermission = BranchPermission(
                        permission.readPermission,
                        permission.readPermission,
                        newBranch.generateKey()
                    ).encode(true).first

                    // Save role data on disk
                    role.permissions.add(newPermission)
                    role.encode(true)
                }
            }
        }
    }
}
