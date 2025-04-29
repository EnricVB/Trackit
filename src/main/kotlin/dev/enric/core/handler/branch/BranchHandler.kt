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

class BranchHandler(
    private val workingBranch: Branch? = null,
    private val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Creates a new branch based on the current working branch.
     *
     * This operation will create a new branch with the specified name and inherit permissions from the current working branch.
     * The initial commit for the new branch will be created based on the current commit of the working branch.
     *
     * @param branchName The name of the new branch to be created.
     *
     * @throws BranchNotFoundException if the working branch does not exist.
     * @throws InvalidPermissionException if the user does not have read permissions on the branch.
     * @throws IllegalStateException if the branch already exists.
     */
    fun createBranch(branchName: String) {
        val user = isValidSudoUser(sudoArgs)

        validateWorkingBranchExists()
        validateReadPermission(user)
        validateBranchDoesNotExist(branchName)

        val newBranch = Branch(branchName).apply { encode(true) }
        inheritBranchPermissions(newBranch, workingBranch!!)

        val commit = createInitialCommit(newBranch, user)
        tagInitialCommit(commit)

        BranchIndex.setBranchHead(newBranch.generateKey(), commit)
        CommitIndex.setCurrentCommit(commit)
        Logger.info("New branch '$branchName' created with hash ${newBranch.generateKey().abbreviate()}^")
    }

    /**
     * Removes the current working branch.
     * This operation is irreversible and should be done with caution.
     *
     * @throws BranchNotFoundException if the working branch does not exist.
     * @throws InvalidPermissionException if the user does not have write permissions on the branch.
     * @throws IllegalStateException if the branch is the last one in the repository.
     */
    fun removeBranch() {
        val user = isValidSudoUser(sudoArgs)

        validateWorkingBranchExists()
        validateIsNotLastBranch()
        validateWritePermission(user)

        val branchFile = getBranchFilePath()
        branchFile.deleteIfExists()
    }

    /**
     * Inherits branch permissions from the old branch to the new branch.
     * This is useful when creating a new branch based on an existing one.
     *
     * @param newBranch The new branch to which permissions will be inherited.
     * @param oldBranch The old branch from which permissions will be inherited.
     */
    fun inheritBranchPermissions(newBranch: Branch, oldBranch: Branch) {
        val oldPermissions = BranchPermissionIndex.getBranchPermissionsByBranch(oldBranch.name)
        val newBranchHash = newBranch.generateKey()

        UserIndex.getAllUsers().forEach { userHash ->
            val user = User.newInstance(userHash)

            user.roles.forEach { roleHash ->
                val role = Role.newInstance(roleHash)

                role.permissions
                    .filter { it in oldPermissions }
                    .forEach { matchingPermission ->
                        val permission = BranchPermission.newInstance(matchingPermission)

                        val newPermission = BranchPermission(
                            permission.readPermission,
                            permission.readPermission,
                            newBranchHash
                        ).encode(true).first

                        // Save the new permission to the role
                        role.permissions.add(newPermission)

                        Logger.debug(
                            "Copied permission from ${oldBranch.name} to ${newBranch.name} " +
                            "for user ${user.name}, role ${role.name}: " +
                            "Read=${permission.readPermission}, Write=${permission.writePermission}"
                        )
                    }

                // Save the updated role back to the user
                role.encode(true)
            }
        }
    }

    // --- Validations ---

    /**
     * Validates that the working branch exists.
     * This is to ensure that the user is currently on a branch before performing any operations.
     *
     * @throws BranchNotFoundException if the working branch does not exist.
     */
    private fun validateWorkingBranchExists() {
        if (workingBranch == null) {
            throw BranchNotFoundException("You are not working on a branch.")
        }
    }

    /**
     * Validates that the user has read permissions on the branch.
     * This is to ensure that the user can view the branch and its contents before creating a fork.
     *
     * @throws InvalidPermissionException if the user does not have read permissions.
     */
    private fun validateReadPermission(user: User) {
        val key = workingBranch!!.generateKey()
        if (!hasReadPermissionOnBranch(user, key)) {
            throw InvalidPermissionException("You need read permissions over ${key.abbreviate()} to create a new fork.")
        }
    }

    /**
     * Validates that the user has write permissions on the branch.
     * This is to ensure that the user can modify or delete the branch.
     *
     * @throws InvalidPermissionException if the user does not have write permissions.
     */
    private fun validateWritePermission(user: User) {
        val key = workingBranch!!.generateKey()
        if (!hasWritePermissionOnBranch(user, key)) {
            throw InvalidPermissionException("You need write permissions over ${key.abbreviate()} to delete this branch.")
        }
    }

    /**
     * Validates that the branch does not already exist in the repository.
     * This is to prevent the creation of duplicate branches.
     *
     * @throws IllegalStateException if the branch already exists.
     */
    private fun validateBranchDoesNotExist(branchName: String) {
        if (BranchIndex.exists(branchName)) {
            throw IllegalStateException("Branch $branchName already exists.")
        }
    }

    /**
     * Validates that the branch is not the last one in the repository.
     * This is to prevent the deletion of the last branch, which would leave the repository without any branches.
     *
     * @throws IllegalStateException if the branch is the last one.
     */
    private fun validateIsNotLastBranch() {
        val branchFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(BRANCH_PERMISSION.hash.string)
        val files = branchFolder.toFile().listFiles()
        if (files == null || files.size <= 1) {
            throw IllegalStateException("You cannot delete the last branch.")
        }
    }

    // --- Helpers ---

    /**
     * Creates the initial commit for the new branch.
     * This commit is based on the current commit of the working branch.
     *
     * Default Title: "Created new Branch ${newBranch.name} from ${previousCommit?.branch?.abbreviate()}^"
     * Default Message: "This is an automated commit."
     *
     * This commit helps to establish the new branch's history.
     *
     * @param newBranch The new branch where commit has to created.
     * @param user The user creating the branch.
     * @return The hash of the initial commit.
     */
    private fun createInitialCommit(newBranch: Branch, user: User): Hash {
        val previousCommit = CommitIndex.getCurrentCommit()
        val commit = Commit(
            previousCommit = previousCommit?.generateKey() ?: Hash.empty32(),
            tree = previousCommit?.tree ?: mutableListOf(),
            branch = newBranch.generateKey(),
            author = user.generateKey(),
            confirmer = user.generateKey(),
            title = "Created new Branch ${newBranch.name} from Branch ${Branch.newInstance(previousCommit?.branch!!).name}",
            message = "This is an automatic commit."
        )

        return commit.encode(true).first
    }

    /**
     * Tags the initial commit of the new branch with a simple tag.
     */
    private fun tagInitialCommit(commit: Hash) {
        val tag = SimpleTag("NewBranch").encode(true).first
        TagIndex.addTagToCommit(tag, commit)
    }

    private fun getBranchFilePath() = RepositoryFolderManager()
        .getObjectsFolderPath()
        .resolve(BRANCH_PERMISSION.hash.string)
        .resolve(workingBranch!!.generateKey().toString())

    /**
     * Lists all branches in the repository.
     *
     * This operation retrieves all branches and displays their names and hashes.
     * It is useful for users to see the available branches in the repository.
     */
    fun listBranches() {
        val branches = BranchIndex.getAllBranches().map { Branch.newInstance(it) }
        if (branches.isEmpty()) {
            Logger.info("No branches found.")
            return
        }

        Logger.info("Branches:")
        branches.forEach { branch ->


            Logger.info("- ${branch.name} (${branch.generateKey().abbreviate()}^)")
        }
    }
}