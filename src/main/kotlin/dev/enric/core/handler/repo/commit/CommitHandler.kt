package dev.enric.core.handler.repo.commit

import dev.enric.command.repo.staging.Stage
import dev.enric.core.handler.repo.tag.TagHandler
import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.domain.objects.*
import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.common.FileStatus
import dev.enric.util.common.SerializablePath
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.sql.Timestamp
import kotlin.io.path.pathString

/**
 * Handles the process of committing changes to the repository.
 * This includes creating commit trees, checking out commits,
 * and verifying file states in relation to commits.
 */
data class CommitHandler(val commit: Commit) : CommandHandler() {

    /**
     * Verifies if the commit can be done by checking
     * - If player has permissions to write on the branch.
     * - If staged files are not empty.
     *
     * @return True if the commit can be done, false otherwise.
     * @throws InvalidPermissionException If the user does not have write permission on the branch.
     * @throws IllegalStateException If there are no files to commit.
     */
    fun canDoCommit(): Boolean {
        checkWritePermissionOnBranch(User.newInstance(commit.confirmer))
        hasFilesToCommit()

        return true
    }

    /**
     * Checks if the user has the permission to write into the specified branch.
     */
    private fun checkWritePermissionOnBranch(user: User) {
        if (!hasWritePermissionOnBranch(user)) {
            val branch = BranchIndex.getCurrentBranch()
            throw InvalidPermissionException("User does not have permission to write on branch ${branch.name} (${branch.generateKey()})")
        }
    }

    /**
     * Checks if there are files to commit in the staging area.
     */
    private fun hasFilesToCommit() {
        val hasDeletedFilesToCommit = FileStatus.getDeletedFiles().isNotEmpty()

        if (StagingHandler.getStagedFiles().isEmpty() && !hasDeletedFilesToCommit) {
            throw IllegalStateException("The staging area is empty. Add files to commit.")
        } else if (hasDeletedFilesToCommit) {
            Logger.warning("There are only files to be deleted. The commit will be empty.")
        }
    }

    /**
     * Checks if the user has write permission on the current branch.
     * @param user The user to check.
     * @return True if the user has write permission on the branch, false otherwise.
     */
    private fun hasWritePermissionOnBranch(user: User) : Boolean {
        return user.roles.map { Role.newInstance(it) }.any { role ->
            role.getBranchPermissions().any { it.branch == commit.branch && it.writePermission }
        }
    }

    /**
     * Initializes the commit properties.
     *
     * - Sets the commit date to the current time.
     * - Sets the previous commit to the current commit.
     * - Sets the branch to the current branch.
     */
    fun initializeCommitProperties(author: Array<String>?, confirmer: Array<String>?) {
        commit.previousCommit = CommitIndex.getCurrentCommit()?.encode()?.first
        commit.branch = BranchIndex.getCurrentBranch().generateKey()

        Logger.log("Logging for author...")
        commit.author = isValidSudoUser(author).generateKey()

        Logger.log("Logging for confirmer...")
        commit.confirmer = isValidSudoUser(confirmer).generateKey()

        commit.date = Timestamp.from(java.time.Instant.now())
    }

    /**
     * Does all the pre commit operations. This includes staging all files if needed.
     */
    fun preCommit(stageAllFiles: Boolean) {
        if (stageAllFiles) {
            stageAllFiles()
        }
    }

    /**
     * Processes a new commit by creating its corresponding tree structure.
     * @return The processed commit with an updated tree.
     */
    fun processCommit(): Commit {
        commitTree()
        keepPreviousCommitTree()

        return commit
    }

    /**
     * Commits the tree structure to the commit object.
     * This is done by creating a tree structure based on the staged files.
     *
     * @see createCommitTree
     */
    private fun commitTree() {
        val commitTree = createCommitTree()
        commit.tree = commitTree.map { it.encode(true).first }.toMutableList()
    }

    /**
     * Keeps the previous commit tree.
     * Filters the previous commit tree to keep only the files that are still present in the working directory.
     */
    private fun keepPreviousCommitTree() {
        val previousCommit = CommitIndex.getCurrentCommit() ?: return

        // Remove deleted files from the previous commit tree
        previousCommit.tree.removeIf { treeHash ->
            val tree = Tree.newInstance(treeHash)
            return@removeIf FileStatus.hasBeenDeleted(tree.serializablePath.toPath().toFile())
        }

        // Add new files to the current commit tree
        val currentPaths = commit.tree.map { Tree.newInstance(it).serializablePath.toPath() }.toSet()

        // Add files that are not in the current commit tree but are in the previous commit tree
        previousCommit.tree.forEach { treeHash ->
            val tree = Tree.newInstance(treeHash)
            val treePath = tree.serializablePath.toPath()

            if (treePath !in currentPaths) {
                commit.tree.add(treeHash)
            }
        }
    }

    /**
     * Does all the post commit operations as save into indexes the new commit hash, or replace the branch.
     */
    fun postCommit(tags: List<String>) {
        val commitHash = commit.encode(true).first

        // Updates the commit index and the branch index
        CommitIndex.setCurrentCommit(commitHash)
        BranchIndex.setBranchHead(commit.branch, commitHash)

        // Assigns a tag to the commit
        tags.forEach {
            TagHandler(it).assignTag(commit)
        }

        // Removes all the staged files after the commit
        StagingHandler.clearStagingArea()
    }

    /**
     * Creates a tree structure based on the current staged files.
     * @return A list of Tree objects representing the commit tree.
     */
    private fun createCommitTree(): List<Tree> {
        return mapStagedFilesToTree(StagingHandler.getStagedFiles())
    }

    /**
     * Maps staged files (hash and path) to a list of Tree objects.
     * @param stagedFiles List of pairs containing file hash and its path.
     * @return A list of Tree objects representing the staged files.
     */
    private fun mapStagedFilesToTree(stagedFiles: List<Pair<Hash, Path>>): List<Tree> {
        return stagedFiles.mapNotNull { stagedFile ->
            val rootPath = File(".").absoluteFile.toPath()
            val relativePath = stagedFile.second.pathString.replace("${File.separator}.${File.separator}", "")
            val path = rootPath.resolve(relativePath)

            val fileContent = safeFileRead(path.toFile())

            if (fileContent != null) {
                val content = fileContent.encode(true).first
                return@mapNotNull Tree(SerializablePath.of(path), content)
            }

            return@mapNotNull null
        }
    }

    /**
     * Reads a file safely, even if it is being used by another process.
     * @param file The file to be read.
     * @return A Content object representing the file content, or null if reading fails.
     */
    private fun safeFileRead(file: File): Content? {
        return try {
            FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
                val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                Content(ByteArray(buffer.remaining()).apply { buffer[this] })
            }
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Stages all the files before committing
     *
     * @see Stage
     */
    fun stageAllFiles() {
        Logger.log("Staging all files before committing")

        val stageCommand = Stage()

        stageCommand.stagePath = RepositoryFolderManager().getInitFolderPath()
        stageCommand.force = false

        stageCommand.call()
    }
}