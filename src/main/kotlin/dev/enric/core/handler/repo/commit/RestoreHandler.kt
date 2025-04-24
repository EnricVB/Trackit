package dev.enric.core.handler.repo.commit

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.*
import dev.enric.exceptions.CommitNotFoundException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Handler responsible for restoring files from a specific [Commit].
 * Allows restoring a specific file or all files to their state in the given commit.
 *
 * @param commit The [Commit] from which to restore files. Defaults to the current commit (HEAD) if null.
 * @param file The specific [Path] to restore. If null, restores all files from the commit.
 * @param sudoArgs Optional credentials for sudo operations (not used in current implementation).
 */
class RestoreHandler(
    val commit: Commit? = CommitIndex.getCurrentCommit(),
    val file: Path? = null,
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Verifies if can restore the file to the commit status by checking
     * - If player has permissions to read on the branch.
     *
     * @return True if can restore, false otherwise
     * @throws InvalidPermissionException If the user does not have read permission on the branch.
     */
    fun canRestore(): Boolean {
        checkReadPermissionOnBranch(isValidSudoUser(sudoArgs))

        return true
    }

    /**
     * Checks if the user has the permission to read into the specified branch.
     */
    private fun checkReadPermissionOnBranch(user: User) {
        if (commit == null) {
            throw CommitNotFoundException("Commit not found in the repository.")
        }

        if (!hasReadPermissionOnBranch(user, commit.branch)) {
            val branch = Branch.newInstance(commit.branch)
            throw InvalidPermissionException("User does not have permissions to read branch ${branch.name} (${commit.branch})")
        }
    }

    /**
     * Restores the specified [file] or all files from the [commit] into the Working Directory.
     * - If [file] is null, all files in the commit are restored.
     * - If [commit] is null and no current commit is found, throws [CommitNotFoundException].
     * - Creates parent directories if needed.
     * - Writes content to files only if they do not already exist.
     *
     * @throws CommitNotFoundException if no commit is provided or found.
     */
    fun restore() {
        // If no commit is provided, get the current commit. If there is no current commit, throw an exception.
        if (commit == null) {
            throw CommitNotFoundException("No commit found.")
        }

        // Restore the commit's tree structure and content.
        commit.tree.forEach {
            val tree = Tree.newInstance(it)

            // Determine if we should restore this file
            val restoreAll = file == null
            val isCorrectFile = tree.serializablePath.pathString == file?.toString() || restoreAll
            if (!isCorrectFile) return@forEach

            // Ensure parent directories exist
            Path.of(tree.serializablePath.pathString).parent.toFile().mkdirs()

            // Write the content to the file
            Files.writeString(
                tree.serializablePath.toPath(),
                String(Content.newInstance(tree.content).content),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )

            Logger.info("Restored file: ${tree.serializablePath.relativePath(RepositoryFolderManager().getInitFolderPath())}")
        }

        Logger.info("Restore successful.")
    }
}