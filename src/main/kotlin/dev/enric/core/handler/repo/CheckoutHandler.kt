package dev.enric.core.handler.repo

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.*
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

class CheckoutHandler(
    val commit: Commit,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {
    /**
     * Verifies if can checkout to the commit by checking
     * - If player has permissions to read on the branch.
     *
     * @return True if can checkout, false otherwise
     * @throws InvalidPermissionException If the user does not have read permission on the branch.
     */
    fun canDoCheckout(): Boolean {
        checkReadPermissionOnBranch(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Checks if the user has the permission to read into the specified branch.
     */
    private fun checkReadPermissionOnBranch(user: User) {
        if (!hasReadPermissionOnBranch(user, commit.branch)) {
            val branch = Branch.newInstance(commit.branch)
            throw InvalidPermissionException("User does not have permissions to read branch ${branch.name} (${commit.branch})")
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
                val isTrackitFolder =
                    it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath().toRealPath())
                val isSecretKey = it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath().toRealPath())
                val isRootFolder = it.toRealPath() == repositoryFolderManager.getInitFolderPath().toRealPath()

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
        val repoRoot = RepositoryFolderManager().getInitFolderPath()

        commit.tree.forEach {
            val tree = Tree.newInstance(it)
            val relativePath = Path.of(tree.serializablePath.pathString)
            val file = repoRoot.resolve(relativePath)
            if (!file.parent.exists()) {
                file.parent.createDirectories()
            }

            try {
                Files.writeString(
                    file,
                    String(Content.newInstance(tree.content).content),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
                )
            } catch (e: Exception) {
                Logger.error("Error while writing file: ${file.pathString}")
            }
        }

        Logger.info("Checkout successful.")
    }
}