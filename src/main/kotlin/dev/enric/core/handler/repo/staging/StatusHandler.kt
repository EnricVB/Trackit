package dev.enric.core.handler.repo.staging

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.FileStatus
import dev.enric.util.common.FileStatus.*
import dev.enric.util.common.SerializablePath
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

/**
 * Handles the status of files in the repository.
 * This class provides functionality to check the state of files in the working directory,
 * determine which files have been modified, staged, or ignored, and display this information.
 */
object StatusHandler : CommandHandler() {
    /**
     * Prints the current repository status to the logger.
     * It displays:
     * - The current branch name.
     * - The latest commit details (hash, author, date).
     * - The list of files grouped by their status (modified, staged, untracked, etc.).
     */
    fun printStatus(showIgnored: Boolean) {
        val statusMap = getFilesStatus()
        val branch = BranchIndex.getCurrentBranch()
        val currentCommit = CommitIndex.getCurrentCommit()

        Logger.log("On branch ${branch.name}")

        if (currentCommit != null) {
            Logger.log("Current commit: ${currentCommit.generateKey()}")
            Logger.log("Author: ${User.newInstance(currentCommit.author).name}")
            Logger.log("Date: ${currentCommit.date}")
        }

        statusMap.forEach { (status, files) ->
            val pathToShow = files.map {
                SerializablePath.of(it.path).relativePath(RepositoryFolderManager().getInitFolderPath()).toString()
            }

            Logger.log("")
            Logger.log(status.description)

            pathToShow.forEach { file -> if(showIgnored || status != IGNORED) Logger.log("\t[${status.symbol}] ${ColorUtil.message(file)}") }
        }

        if (StagingHandler.getStagedFiles().isEmpty()) {
            Logger.log("")
            Logger.log("no changes added to commit (use \"trackit stage\" and/or \"trackit commit -a\")")
        }
    }

    /**
     * Retrieves the status of all files in the working directory.
     *
     * @return A map where each key is a [FileStatus] representing the file status,
     *         and each value is a list of files that fall under that status.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getFilesStatus(): Map<FileStatus, List<File>> {
        val statusMap = mutableMapOf<FileStatus, MutableList<File>>()
        val repositoryFolderManager = RepositoryFolderManager()

        repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .toMutableList()
            .filterNot {
                it.toFile().isDirectory || it.toRealPath().toString().contains(".trackit")
            }
            .forEach { path ->
                val file = path.toFile()
                val status = FileStatus.getStatus(file)

                statusMap.getOrPut(status) { mutableListOf() }.add(file)
            }

        FileStatus.getDeletedFiles().forEach { file ->
            statusMap.getOrPut(DELETE) { mutableListOf() }.add(file)
        }

        return statusMap.mapValues { (_, files) -> files.sortedBy { it.path } }
    }
}