package dev.enric.util.common

import dev.enric.core.handler.repo.ignore.IgnoreHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.core.handler.repo.staging.StatusHandler.repositoryFolderManager
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.util.index.CommitIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Enum class representing the different types of file status in Trackit.
 * Each status indicates the state of a file in the working directory, staging area, or repository.
 *
 * @param symbol The shorthand symbol used to represent the file status.
 * @param description A human-readable description of the status.
 */
enum class FileStatus(val symbol: String, val description: String) {

    /**
     * The file is not being tracked by Trackit. It exists in the working directory but has not been added to the index.
     */
    UNTRACKED("?",
        """Untracked files:
            |   (use "trackit stage <file>..." to include in what will be committed)""".trimMargin()),

    /**
     * The file is tracked and has not been modified since the last commit.
     */
    UNMODIFIED("*", """Files up to date:"""),

    /**
     * The file has been modified but is not yet staged for commit.
     */
    MODIFIED("M",
        """Files with changes:
            |   (use "trackit stage <file>..." to update what will be committed)""".trimMargin()),

    /**
     * The file has been modified and staged for the next commit.
     */
    STAGED("S",
        """Changes to be committed:
            |   (use "trackit commit" to commit changes)
            |   (use "trackit unstage" to unstage changes)""".trimMargin()),

    /**
     * The file has been deleted and the deletion has been staged.
     */
    DELETE("D",
        """Deleted files:
            |   (use "trackit restore <file>..." to restore the file)""".trimMargin()),

    /**
     * The file has been renamed or moved.
     */
    RENAMED("R",
        """Renamed files:
            |   (use "trackit restore <file>..." to restore the file)""".trimMargin()),

    /**
     * The file is ignored based on `.ignore` rules and will not be tracked.
     */
    IGNORED("I",
        """Ignored files:
            |   (use "trackit unignore <file>..." to start tracking)""".trimMargin());

    companion object {
        /**
         * Retrieves a [FileStatus] from its symbol.
         * Returns null if the symbol does not match any status.
         */
        fun fromSymbol(symbol: String): FileStatus? {
            return entries.find { it.symbol == symbol }
        }

        /**
         * Checks if a given content has been added to the repository.
         *
         * @param file The file to check.
         * @return `true` if the content exists in the repository, `false` otherwise.
         */
        fun contentExists(file: File): Boolean {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return false

            currentCommit.tree.forEach { treeHash ->
                val treeObject = Tree.newInstance(treeHash)
                val treePath = treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
                val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

                if (treePath == filePath) {
                    return true
                }
            }

            return false
        }

        /**
         * Retrieves the list of files that have been deleted from the repository.
         * This is done by comparing the current commit tree with the working directory.
         *
         * @return A list of files that have been deleted.
         */
        fun getDeletedFiles(): List<File> {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return emptyList()
            val initFolderPath = repositoryFolderManager.getInitFolderPath()

            return runBlocking {
                currentCommit.tree.map { treeHash ->
                    async (Dispatchers.IO) {
                        val treeObject = Tree.newInstance(treeHash)
                        val treePathRelative = treeObject.serializablePath.relativePath(initFolderPath)
                        val filePath = initFolderPath.resolve(treePathRelative)
                        val file = filePath.toFile()

                        if (!file.exists()) file else null
                    }
                }.awaitAll().filterNotNull()
            }
        }

        /**
         * Determines if a file has been deleted from the repository.
         * This is done by comparing the current commit tree with the working directory.
         *
         * @param file The file to check.
         * @return `true` if the file has been deleted, `false` otherwise.
         * @see CommitIndex
         */
        fun hasBeenDeleted(file: File): Boolean {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return false

            currentCommit.tree.forEach { tree ->
                val treeObject = Tree.newInstance(tree)
                val treePath = treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
                val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

                if (treePath == filePath) {
                    if (!file.exists()) {
                        return true
                    }
                }
            }

            return false
        }

        /**
         * Determines if a file is up to date with the latest commit.
         *
         * @param file The file to check.
         * @return `true` if the file content matches the latest commit, `false` otherwise.
         */
        fun isUpToDate(file: File): Boolean {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return true

            currentCommit.tree.forEach { tree ->
                val treeObject = Tree.newInstance(tree)
                val treePath = treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
                val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

                if (treePath == filePath) {
                    val treeFileContent = String(Content.newInstance(treeObject.content).content).trim()
                    val fileContent = String(file.readText().toByteArray()).trim()

                    return treeFileContent == fileContent
                }
            }

            return false
        }

        /**
         * Retrieves the status of all files in the working directory.
         *
         * @return A map where each key is a [FileStatus] representing the file status,
         *         and each value is a list of files that fall under that status.
         */
        fun getStatus(file: File): FileStatus {
            if (!file.exists()) return DELETE
            if (IgnoreHandler.isIgnored(file.toPath())) return IGNORED

            val hash = Content(file.readBytes()).generateKey()
            val contentExists = FileStatus.contentExists(file)
            val isUpToDate = FileStatus.isUpToDate(file)
            val isStaged = StagingHandler.getStagedFiles().any { it.first == hash }

            return when {
                isStaged -> STAGED
                contentExists && isUpToDate -> UNMODIFIED
                contentExists && !isUpToDate -> MODIFIED
                else -> UNTRACKED
            }
        }
    }
}