package dev.enric.core.handler.repo

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.admin.RemotePathConfig
import dev.enric.core.handler.remote.PushHandler
import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.remote.packet.message.data.BranchSyncStatusResponseData.BranchSyncStatus.*
import dev.enric.util.common.*
import dev.enric.util.common.FileStatus.DELETED
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
     * @param showIgnored If true, ignored files will be included in the status.
     * @param showUpdated If true, updated files will be included in the status.
     */
    suspend fun printStatus(showIgnored: Boolean, showUpdated: Boolean) {
        printBranchStatus()
        printRemoteStatus()
        printFileStatus(showIgnored, showUpdated)
    }

    fun printBranchStatus() {
        val branch = BranchIndex.getCurrentBranch()
        val branchHEAD = BranchIndex.getBranchHead(branch.generateKey())
        val currentCommit = CommitIndex.getCurrentCommit()

        Logger.info("On branch ${branch.name}")
        currentCommit?.let {
            Logger.info("Current commit: ${it.generateKey()} ${if (branchHEAD.generateKey() == it.generateKey()) "HEAD" else ""}")
            Logger.info("Author: ${User.newInstance(it.author).name}")
            Logger.info("Date: ${it.date}")
        }
    }

    suspend fun printRemoteStatus() {
        try {
            if (!RemotePathConfig().isRemotePushSet()) {
                return
            }

            val remotePushUrl = RemoteUtil.loadAndValidateRemotePushUrl()
            val handler = PushHandler(remotePushUrl)
            val socket = handler.connectToRemote()
            val currentBranch = BranchIndex.getCurrentBranch()

            val response = RemoteUtil.askForRemoteBranchStatus(currentBranch.name, socket)
            val status = response.first

            when (status) {
                SYNCED -> {
                    Logger.info("Your branch is up to date with the remote branch.")
                }

                ONLY_LOCAL -> {
                    Logger.info("Your branch is only present locally.")
                    Logger.info("Use \"trackit push\" to create the remote branch.")
                }

                ONLY_REMOTE -> {
                    Logger.info("Your branch is only present on the remote.")
                    Logger.info("Use \"trackit pull\" to create the local branch.")
                }

                DIVERGED -> {
                    Logger.info("Your branch has diverged from the remote branch by ${response.second.size} commits locally and ${response.third.size} commits remotely.")
                    Logger.info("Use \"trackit pull\" to update your local branch.")
                }

                AHEAD -> {
                    Logger.info("Your branch is ahead of the remote branch by ${response.second.size} commits.")
                    Logger.info("Use \"trackit push\" to update the remote branch.")
                }

                BEHIND -> {
                    Logger.info("Your branch is behind the remote branch by ${response.third.size} commits.")
                    Logger.info("Use \"trackit pull\" to update your local branch.")
                }
            }
        } catch (ex: Exception) {
            // Don't print anything if the remote is not set or if the connection fails
        }
    }

    fun printFileStatus(showIgnored: Boolean, showUpdated: Boolean) {
        val statusMap = getFilesStatus(showIgnored, showUpdated)

        if (statusMap.isEmpty()) {
            Logger.info("\nWorking directory clean")
            return
        }

        statusMap.forEach { (status, files) ->
            val relativePaths = files.map {
                val repoPath = RepositoryFolderManager().getInitFolderPath()
                SerializablePath.of(it.path).relativePath(repoPath).toString()
            }

            Logger.info("")
            Logger.info(status.description)
            relativePaths.forEach { path ->
                Logger.info("\t[${status.symbol}] ${ColorUtil.message(path)}")
            }
        }

        if (!StagingHandler.hasStagedFiles()) {
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
            statusMap.getOrPut(DELETED) { mutableListOf() }.add(file)
        }

        return statusMap.mapValues { (_, files) -> files.sortedBy { it.path } }
    }
}