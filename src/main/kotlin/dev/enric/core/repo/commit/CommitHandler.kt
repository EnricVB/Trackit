package dev.enric.core.repo.commit

import dev.enric.core.Hash
import dev.enric.domain.Commit
import dev.enric.domain.Content
import dev.enric.domain.Tree
import dev.enric.util.common.SerializablePath
import dev.enric.core.repo.staging.StagingHandler
import dev.enric.util.index.CommitIndex
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.pathString

/**
 * Handles the process of committing changes to the repository.
 * This includes creating commit trees, checking out commits,
 * and verifying file states in relation to commits.
 */
class CommitHandler {

    /**
     * Processes a new commit by creating its corresponding tree structure.
     * @param commit The commit object to process.
     * @return The processed commit with an updated tree.
     */
    fun processCommit(commit: Commit): Commit {
        val commitTree = createCommitTree()
        commit.tree = commitTree.map { it.encode(true).first }

        return commit
    }

    /**
     * Does all the post commit operations as save into indexes the new commit hash, or replace the branch.
     * @param commit The commit object that has been created.
     */
    fun postCommit(commit: Commit) {
        commit.encode(true)
        CommitIndex.setCurrentCommit(commit.encode().first)
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
            val relativePath = stagedFile.second.pathString.replace("\\.\\", "")
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
     * Checks if a file is up to date with the latest commit.
     * @param file The file to check.
     * @param commit The commit to compare against.
     * @return True if the file is already committed, false otherwise.
     */
    fun isFileUpToDateToCommit(file: File, commit: Commit): Boolean {
        val content = Content(Files.readAllBytes(file.toPath()))
        return commit.findFile(content, file.toPath()) != null
    }
}