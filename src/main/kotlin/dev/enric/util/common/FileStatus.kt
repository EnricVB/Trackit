package dev.enric.util.common

import dev.enric.core.handler.repo.IgnoreHandler
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.core.handler.repo.StagingHandler.StagingCache
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
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
    UNTRACKED(
        "?",
        """Untracked files:
            |   (use "trackit stage <file>..." to include in what will be committed)""".trimMargin()
    ),

    /**
     * The file is tracked and has not been modified since the last commit.
     */
    UNMODIFIED("*", """Files up to date:"""),

    /**
     * The file has been modified but is not yet staged for commit.
     */
    MODIFIED(
        "M",
        """Files with changes:
            |   (use "trackit stage <file>..." to update what will be committed)""".trimMargin()
    ),

    /**
     * The file has been modified and staged for the next commit.
     */
    STAGED(
        "S",
        """Changes to be committed:
            |   (use "trackit commit" to commit changes)
            |   (use "trackit unstage" to unstage changes)""".trimMargin()
    ),

    /**
     * The file has been deleted and the deletion has been staged.
     */
    DELETE(
        "D",
        """Deleted files:
            |   (use "trackit restore <file>..." to restore the file)""".trimMargin()
    ),

    /**
     * The file is ignored based on `.ignore` rules and will not be tracked.
     */
    IGNORED(
        "I",
        """Ignored files:
            |   (use "trackit unignore <file>..." to start tracking)""".trimMargin()
    );

    companion object {
        private val fileStatusCache = mutableMapOf<String, FileStatus>()

        /**
         * Checks if a given content has been added to the repository.
         *
         * @param file The file to check.
         * @return `true` if the content exists in the repository, `false` otherwise.
         */
        fun fileExists(file: File): Boolean {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return false
            val repositoryFolderManager = RepositoryFolderManager()

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
            val initFolderPath = RepositoryFolderManager().getInitFolderPath()

            val deletedFilesCache = mutableListOf<File>()

            runBlocking {
                currentCommit.tree.map { treeHash ->
                    async(Dispatchers.IO) {
                        val treeObject = Tree.newInstance(treeHash)
                        val treePathRelative = treeObject.serializablePath.relativePath(initFolderPath)
                        val filePath = initFolderPath.resolve(treePathRelative)
                        val file = filePath.toFile()

                        if (!file.exists()) {
                            deletedFilesCache.add(file)
                        }
                    }
                }.awaitAll()
            }

            return deletedFilesCache
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
            val repositoryFolderManager = RepositoryFolderManager()
            val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

            val treePaths = currentCommit.tree.map { tree ->
                val treeObject = Tree.newInstance(tree)
                treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
            }.toSet()

            return file.exists().not() && filePath in treePaths
        }

        /**
         * Determines if a file is up to date with the latest commit.
         *
         * @param file The file to check.
         * @return `true` if the file content matches the latest commit, `false` otherwise.
         */
        fun isUpToDate(file: File): Boolean {
            val currentCommit = CommitIndex.getCurrentCommit() ?: return true
            val repositoryFolderManager = RepositoryFolderManager()
            val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

            val treeContentMap = currentCommit.tree.associate { tree ->
                val treeObject = Tree.newInstance(tree)
                val treePath = treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
                val treeContent = String(Content.newInstance(treeObject.content).content).trim()
                treePath to treeContent
            }

            return treeContentMap[filePath]?.let { treeContent ->
                val fileContent = file.readText().trim()
                treeContent == fileContent
            } ?: false
        }

        /**
         * Retrieves the status of all files in the working directory.
         *
         * @return A map where each key is a [FileStatus] representing the file status,
         *         and each value is a list of files that fall under that status.
         */
        fun getStatus(file: File): FileStatus {
            val hash = Content(file.readBytes()).generateKey()
            val filePath = file.path

            fileStatusCache[filePath]?.let {
                return it
            }

            val status = when {
                !file.exists() -> DELETE
                IgnoreHandler().isIgnored(file.toPath()) -> IGNORED
                StagingCache.getStagedFiles().any { it.first == hash } -> STAGED
                else -> {
                    val contentExists = fileExists(file)
                    val isUpToDate = isUpToDate(file)

                    when {
                        contentExists && isUpToDate -> UNMODIFIED
                        contentExists && !isUpToDate -> MODIFIED
                        else -> UNTRACKED
                    }
                }
            }

            fileStatusCache[filePath] = status
            return status
        }
    }
}