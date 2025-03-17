package dev.enric.core.repo.staging

import dev.enric.core.repo.ignore.IgnoreHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.FileStatusTypes
import dev.enric.util.common.SerializablePath
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

/**
 * Handles the status of files in the repository.
 * This class provides functionality to check the state of files in the working directory,
 * determine which files have been modified, staged, or ignored, and display this information.
 */
object StatusHandler {
    val repositoryFolderManager = RepositoryFolderManager()

    /**
     * Prints the current repository status to the logger.
     * It displays:
     * - The current branch name.
     * - The latest commit details (hash, author, date).
     * - The list of files grouped by their status (modified, staged, untracked, etc.).
     */
    fun printStatus() {
        val statusMap = getFilesStatus()
        val branch = BranchIndex.getCurrentBranch()
        val currentCommit = CommitIndex.getCurrentCommit()

        Logger.log("On branch ${branch.name}")

        if (currentCommit != null) {
            Logger.log("Current commit: ${currentCommit.encode().first}")
            Logger.log("Author: ${User.newInstance(currentCommit.author).name}")
            Logger.log("Date: ${currentCommit.date}")
        }

        statusMap.forEach { (status, files) ->
            val pathToShow = files.map {
                SerializablePath.of(it.path).relativePath(repositoryFolderManager.getInitFolderPath()).toString()
            }

            Logger.log("")
            Logger.log(status.description)

            pathToShow.forEach { file -> Logger.log("\t[${status.symbol}] ${ColorUtil.message(file)}") }
        }

        if (StagingHandler.getStagedFiles().isEmpty()) {
            Logger.log("")
            Logger.log("no changes added to commit (use \"trackit stage\" and/or \"trackit commit -a\")")
        }
    }

    /**
     * Retrieves the status of all files in the working directory.
     *
     * @return A map where each key is a [FileStatusTypes] representing the file status,
     *         and each value is a list of files that fall under that status.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getFilesStatus(): Map<FileStatusTypes, List<File>> {
        val statusMap = mutableMapOf<FileStatusTypes, MutableList<File>>()

        repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .toMutableList()
            .filterNot {
                it.toFile().isDirectory ||
                        it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath()) ||
                        it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath()) ||
                        it.toRealPath() == repositoryFolderManager.getInitFolderPath()
            }
            .forEach { path ->
                val file = path.toFile()
                val status = getStatus(file)

                statusMap.getOrPut(status) { mutableListOf() }.add(file)
            }

        getDeletedFiles().forEach {
            statusMap.getOrPut(FileStatusTypes.DELETE) { mutableListOf() }.add(it)
        }

        return statusMap.mapValues { (_, files) -> files.sortedBy { it.path } }
    }

    /**
     * Retrieves the status of all files in the working directory.
     *
     * @return A map where each key is a [FileStatusTypes] representing the file status,
     *         and each value is a list of files that fall under that status.
     */
    private fun getStatus(file: File): FileStatusTypes {
        if (!file.exists()) return FileStatusTypes.DELETE
        if (IgnoreHandler.isIgnored(file.toPath())) return FileStatusTypes.IGNORED

        val hash = Content(file.readBytes()).generateKey()
        val contentExists = contentExists(hash)
        val isUpToDate = isUpToDate(file)
        val isStaged = StagingHandler.getStagedFiles().any { it.first == hash }

        return when {
            isStaged -> FileStatusTypes.STAGED
            contentExists && isUpToDate -> FileStatusTypes.UNMODIFIED
            contentExists && !isUpToDate -> FileStatusTypes.MODIFIED
            else -> FileStatusTypes.UNTRACKED
        }
    }

    /**
     * Checks if a given content has been added to the repository.
     *
     * @param hash The hash of the content to check.
     * @return `true` if the content exists in the repository, `false` otherwise.
     */
    private fun contentExists(hash: Hash): Boolean {
        val currentCommit = CommitIndex.getCurrentCommit() ?: return false

        currentCommit.tree.forEach { treeHash ->
            val tree = Tree.newInstance(treeHash)

            if (tree.content == hash) {
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
    private fun isUpToDate(file: File): Boolean {
        val currentCommit = CommitIndex.getCurrentCommit() ?: return true

        currentCommit.tree.forEach { tree ->
            val treeObject = Tree.newInstance(tree)
            val treePath = treeObject.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
            val filePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

            if (treePath == filePath) {
                val treeContent = treeObject.content
                val fileContent = Content(file.readText().toByteArray())

                return treeContent == fileContent.generateKey()
            }
        }

        return false
    }
}