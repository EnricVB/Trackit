package dev.enric.core.handler.repo.commit

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.*
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

class CheckoutHandler(
    val commit: Commit,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {
    /**
     * Verifies if can checkout to the commit by checking
     * - If player has permissions to read on the branch.
     *
     * @return True if can checkout, false otherwise
     * @throws InvalidPermissionException If the user does not have write permission on the branch.
     */
    fun canDoCommit(): Boolean {
        checkReadPermissionOnBranch(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Checks if the user has the permission to write into the specified branch.
     */
    private fun checkReadPermissionOnBranch(user: User) {
        if (!hasReadPermissionOnBranch(user)) {
            throw InvalidPermissionException("User does not have read permission on branch ${BranchIndex.getCurrentBranch().encode().first}")
        }
    }

    /**
     * Checks if the user has read permission on the commit branch.
     * @param user The user to check.
     * @return True if the user has read permission on the branch, false otherwise.
     */
    private fun hasReadPermissionOnBranch(user: User) : Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getBranchPermissions().any { it.branch == commit.branch && it.readPermission }
        }
    }

    /**
     * Deletes all files and folders from Working Space except .trackit folder and key.secret file.
     *
     * This method is called before checking out a commit to avoid conflicts with the current state of the repository.
     */
    @OptIn(ExperimentalPathApi::class)
    fun preCheckout() {
        val repositoryFolderManager = RepositoryFolderManager()

        repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .forEach {
                val isTrackitFolder = it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath())
                val isSecretKey = it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath())
                val isRootFolder = it.toRealPath() == repositoryFolderManager.getInitFolderPath()

                val isValidFolder = !isTrackitFolder && !isSecretKey && !isRootFolder
                val canDelete = it.toFile().exists()

                if (isValidFolder && canDelete) {
                    it.toFile().deleteRecursively()
                }
            }
    }

    /**
     * Checks out a specific commit by restoring its tree structure and content.
     */
    fun checkout() {
        commit.tree.forEach {
            val tree = Tree.newInstance(it)
            Path.of(tree.serializablePath.pathString).parent.toFile().mkdirs()

            if (!tree.serializablePath.toPath().toFile().exists()) {
                Files.writeString(
                    tree.serializablePath.toPath(),
                    String(Content.newInstance(tree.content).content),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                )
            }
        }

        Logger.log("Checkout successful.")
    }
}