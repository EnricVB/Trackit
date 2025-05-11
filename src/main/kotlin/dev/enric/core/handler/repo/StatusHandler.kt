package dev.enric.core.handler.repo

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.repo.StagingHandler.StagingCache
import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.FileStatus
import dev.enric.util.common.FileStatus.DELETE
import dev.enric.util.common.SerializablePath
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi

/**
 * Handles the status of files in the repository.
 * This class provides functionality to check the state of files in the working directory,
 * determine which files have been modified, staged, or ignored, and display this information.
 */
@OptIn(ExperimentalPathApi::class)
class StatusHandler : CommandHandler() {
    /**
     * Prints the current repository status to the logger.
     * It displays:
     * - The current branch name.
     * - The latest commit details (hash, author, date).
     * - The list of files grouped by their status (modified, staged, untracked, etc.).
     *
     * @param shouldPrintFilesStatus If true, the status of files will be printed.
     * @param showIgnored If true, ignored files will be included in the status.
     * @param showUpdated If true, updated files will be included in the status.
     */
    fun printStatus(shouldPrintFilesStatus: Boolean, showIgnored: Boolean, showUpdated: Boolean) {
        val statusMap = getFilesStatus(showIgnored, showUpdated)
        val branch = BranchIndex.getCurrentBranch()
        val currentCommit = CommitIndex.getCurrentCommit()

        Logger.info("On branch ${branch.name}")

        if (currentCommit != null) {
            Logger.info("Current commit: ${currentCommit.generateKey()}")
            Logger.info("Author: ${User.newInstance(currentCommit.author).name}")
            Logger.info("Date: ${currentCommit.date}")
        }

        if (shouldPrintFilesStatus) {
            statusMap.forEach { (status, files) ->
                val pathToShow = files.map {
                    SerializablePath.of(it.path).relativePath(RepositoryFolderManager().getInitFolderPath()).toString()
                }

                Logger.info("")
                Logger.info(status.description)

                pathToShow.forEach { file -> Logger.info("\t[${status.symbol}] ${ColorUtil.message(file)}") }
            }
        }

        if (StagingCache.getStagedFiles().isEmpty()) {
            Logger.info("")
            Logger.info("no changes added to commit (use \"trackit stage\" and/or \"trackit commit -a\")")
        }
    }

    /**
     * Retrieves the status of all files in the working directory.
     *
     * @return A map where each key is a [FileStatus] representing the file status,
     *         and each value is a list of files that fall under that status.
     */
    fun getFilesStatus(showIgnored: Boolean = false, showUpdated: Boolean = false): Map<FileStatus, List<File>> {
        val ignorehandler = IgnoreHandler()

        val statusMap = mutableMapOf<FileStatus, MutableList<File>>()
        val repositoryFolderManager = RepositoryFolderManager()
        val initPath = repositoryFolderManager.getInitFolderPath()

        Files.walkFileTree(initPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                val fileObj = file?.toFile() ?: return FileVisitResult.CONTINUE
                val status = FileStatus.getStatus(fileObj)

                val isIgnored = ignorehandler.isIgnored(fileObj.toPath())
                val isUpdated = status == FileStatus.UNMODIFIED

                if ((isIgnored && !showIgnored) || (isUpdated && !showUpdated)) {
                    return FileVisitResult.CONTINUE
                }

                statusMap.getOrPut(status) { mutableListOf() }.add(fileObj)
                return super.visitFile(file, attrs)
            }

            override fun visitFileFailed(file: Path?, exc: IOException): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes): FileVisitResult {
                val dirObj = dir?.toFile() ?: return FileVisitResult.SKIP_SUBTREE

                return if (ignorehandler.isIgnored(dirObj.toPath()) && !showIgnored) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }
        })

        FileStatus.getDeletedFiles().forEach { file ->
            statusMap.getOrPut(DELETE) { mutableListOf() }.add(file)
        }

        return statusMap.mapValues { (_, files) -> files.sortedBy { it.path } }
    }
}