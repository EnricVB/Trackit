package dev.enric.core.handler.branch

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.repo.StagingHandler.StagingCache
import dev.enric.core.handler.repo.StatusHandler
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.common.FileStatus
import dev.enric.util.index.BranchIndex
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi

class MergeHandler(
    private val workingBranch: Branch?,
    private val mergeBranch: Branch?,
    private val sudoArgs: Array<String>?,
    private val force: Boolean = false,
) : CommandHandler() {

    /**
     * Checks if the user has permission to merge branches.
     *
     * @throws IllegalStateException if the working branch or merge branch is null.
     * @throws InvalidPermissionException if the user does not have permission to merge branches.
     * @return true if the user has permission to merge branches, false otherwise.
     */
    @OptIn(ExperimentalPathApi::class)
    fun canMerge(): Boolean {
        if (workingBranch == null || mergeBranch == null) {
            throw IllegalStateException("Branches cannot be null.")
        }

        val user = isValidSudoUser(sudoArgs)
        val workingBranchPermission = hasReadPermissionOnBranch(user, workingBranch.generateKey())
        val mergeBranchPermission = hasReadPermissionOnBranch(user, mergeBranch.generateKey())

        if (!workingBranchPermission || !mergeBranchPermission) {
            throw InvalidPermissionException("You don't have permission to merge branches.")
        }

        // If the user has not used the --force option, check if the working area is clean
        if (!force) {
            val stagingIsEmpty = StagingCache.getStagedFiles().isEmpty()
            val workingAreaUpToDate = !StatusHandler().getFilesStatus().containsKey(FileStatus.MODIFIED)

            StatusHandler().getFilesStatus().forEach {
                Logger.debug("File status: ${it.key} -> ${it.value}")
            }

            if (!workingAreaUpToDate) {
                throw IllegalStateException("Working area is not clean. Please commit or stash your changes before merging or use '--force'.")
            } else if (!stagingIsEmpty) {
                throw IllegalStateException("Staging area is not empty. Please commit or stash your changes before merging or use '--force'.")
            } else if (getFilesToMerge().isEmpty()) {
                throw IllegalStateException("No files to merge. Please check the branches and try again.")
            }
        }

        return true
    }

    /**
     * Merges the working branch with the merge branch.
     */
    fun doMerge() {
        val mergedFiles = prepareFileContent(getFilesToMerge())

        // Write the merged content to the files
        mergedFiles.forEach { file ->
            val (path, content) = file

            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    /**
     * Prepares the content of the files to be merged.
     *
     * This method compares the content of the files from the working branch and the merge branch,
     * and determines the result of the merge.
     *
     * @param filesToMerge a map where the key is the file path and the value is a Triple:
     * - first: content from the common ancestor
     * - second: content from the working branch
     * - third: content from the merging branch
     *
     * @return a map of file paths to their merged content
     */
    fun prepareFileContent(filesToMerge: Map<Path, Triple<String?, String?, String?>>): Map<Path, String> {
        val workingBranchName = workingBranch?.name ?: "CURRENT"
        val mergeBranchName = mergeBranch?.name ?: "THEIRS"
        val mergedFiles = mutableMapOf<Path, String>()

        filesToMerge.forEach { (path, triple) ->
            val (baseContent, workingContent, mergeContent) = triple

            val result = when {
                baseContent == workingContent && baseContent == mergeContent -> ConflictResult.SAME
                baseContent != workingContent && baseContent == mergeContent -> ConflictResult.OURS
                baseContent == workingContent && baseContent != mergeContent -> ConflictResult.THEIRS
                else -> ConflictResult.CONFLICT
            }

            when (result) {
                ConflictResult.SAME -> mergedFiles[path] = baseContent ?: ""
                ConflictResult.OURS -> mergedFiles[path] = workingContent ?: ""
                ConflictResult.THEIRS -> mergedFiles[path] = mergeContent ?: ""
                ConflictResult.CONFLICT -> {
                    val baseLines = baseContent ?: ""
                    val workingBranchPair = Pair(workingBranchName, workingContent ?: "")
                    val mergeBranchPair = Pair(mergeBranchName, mergeContent ?: "")

                    val mergedContent = tryMerge(baseLines, workingBranchPair, mergeBranchPair)
                    mergedFiles[path] = mergedContent
                }
            }
        }

        return mergedFiles
    }

    /**
     * Tries to merge two versions of a file.
     *
     * This method uses the DiffRowGenerator to generate a diff between the two versions and
     * formats the result as a string.
     *
     * @param ourVersion the version of the file from our branch and its content
     * @param theirVersion the version of the file from the other branch and its content
     * @return the merged result as a string, including conflict markers
     */
    fun tryMerge(baseVersion: String, ourVersion: Pair<String, String>, theirVersion: Pair<String, String>): String {
        val result = StringBuilder()
        val baseLines = baseVersion.split("\n")
        val ourLines = ourVersion.second.split("\n")
        val theirLines = theirVersion.second.split("\n")

        val maxLines = listOf(ourLines.size, theirLines.size).maxOrNull() ?: 0

        var insideConflict = false
        val oldBlock = mutableListOf<String>()
        val newBlock = mutableListOf<String>()

        // Function to flush the conflict blocks if needed
        fun flushConflictIfNeeded() {
            if (insideConflict) {
                result.appendLine("<<<<<< ${ourVersion.first}")
                oldBlock.forEach { result.appendLine(it) }
                result.appendLine("======")
                newBlock.forEach { result.appendLine(it) }
                result.appendLine(">>>>>> ${theirVersion.first}")
                oldBlock.clear()
                newBlock.clear()
                insideConflict = false
            }
        }

        // Goes through the lines of the base version and the other two versions
        // and checks for conflicts or differences.
        // If a conflict is found, it adds the lines to the conflict blocks.
        // If no conflict is found or empty line, it adds the lines to the result.
        for (i in 0 until maxLines) {
            val baseRaw = baseLines.getOrNull(i) ?: ""
            val ourRaw = ourLines.getOrNull(i) ?: ""
            val theirRaw = theirLines.getOrNull(i) ?: ""

            val baseLine = baseRaw.trim()
            val ourLine = ourRaw.trim()
            val theirLine = theirRaw.trim()

            val emptyLine = baseLine.isEmpty() && ourLine.isEmpty() && theirLine.isEmpty()
            val noChanges = baseLine == ourLine && baseLine == theirLine
            val conflict = baseLine != ourLine && baseLine != theirLine

            when {
                emptyLine || noChanges -> { // skip empty lines, add to result
                    flushConflictIfNeeded()
                    if (baseRaw.isNotEmpty()) result.appendLine(baseRaw)
                }

                conflict -> { // conflict found, add to conflict blocks
                    oldBlock.add(ourRaw)
                    newBlock.add(theirRaw)
                    insideConflict = true
                }
            }
        }

        // Flush any remaining conflict blocks
        flushConflictIfNeeded()

        return result.toString()
    }

    /**
     * Gets the files to be merged from the working branch, merge branch, and their common ancestor.
     *
     * This method compares the files in both branches and the common ancestor, and returns a map
     * where the key is the file path and the value is a Triple:
     * - first: content from the common ancestor
     * - second: content from the working branch
     * - third: content from the merging branch
     *
     * @return a map of file paths to a triple of their contents (base, ours, theirs)
     */
    fun getFilesToMerge(): Map<Path, Triple<String?, String?, String?>> {
        val workingBranchFiles = workingBranch?.let { getFilesFromBranch(it) }
        val mergeBranchFiles = mergeBranch?.let { getFilesFromBranch(it) }
        val commonAncestorFiles = getFilesFromCommit(getCommonAncestor())

        val filesToMerge = mutableMapOf<Path, Triple<String?, String?, String?>>()

        val allPaths = mutableSetOf<Path>()
        workingBranchFiles?.keys?.let { allPaths.addAll(it) }
        mergeBranchFiles?.keys?.let { allPaths.addAll(it) }
        commonAncestorFiles.keys.let { allPaths.addAll(it) }

        for (path in allPaths) {
            val baseContent = commonAncestorFiles[path]
            val workingContent = workingBranchFiles?.get(path)
            val mergeContent = mergeBranchFiles?.get(path)

            filesToMerge[path] = Triple(baseContent, workingContent, mergeContent)
        }

        return filesToMerge
    }


    /**
     * Gets the files from a branch.
     *
     * This method retrieves the files from the specified branch and returns a map where the key is the file path and the value is the file content.
     */
    fun getFilesFromBranch(branch: Branch): Map<Path, String> {
        val head = BranchIndex.getBranchHead(branch.generateKey())
        return getFilesFromCommit(head)
    }

    /**
     * Gets the files from a commit.
     *
     * This method retrieves the files from the specified commit and returns a map where the key is the file path and the value is the file content.
     */
    fun getFilesFromCommit(commit: Commit): Map<Path, String> {
        val files = mutableMapOf<Path, String>()

        commit.tree.forEach {
            val tree = Tree.newInstance(it)
            val filePath = Path.of(tree.serializablePath.pathString)
            val fileContent = String(Content.newInstance(tree.content).content)

            files[filePath] = fileContent
        }

        return files
    }

    /**
     * Gets the common ancestor of the working branch and merge branch.
     *
     * This method retrieves the common ancestor of the two branches and returns it as a commit.
     *
     * @return the common ancestor commit
     */
    fun getCommonAncestor(): Commit {
        val workingBranchAncestors = getAllAncestors(workingBranch!!).toSet()
        val mergeBranchAncestors = getAllAncestors(mergeBranch!!).toSet()

        return workingBranchAncestors.intersect(mergeBranchAncestors).firstOrNull()
            ?: throw IllegalStateException("No common ancestor found between branches.")
    }

    /**
     * Gets all ancestors of a branch.
     *
     * This method retrieves all ancestors of the specified branch and returns them as a list of commits.
     *
     * @param branch the branch to get ancestors from
     * @return a list of commits representing the ancestors of the branch
     */
    fun getAllAncestors(branch: Branch): List<Commit> {
        val ancestors = mutableListOf<Commit>()
        var commit: Commit? = BranchIndex.getBranchHead(branch.generateKey())

        while (commit != null) {
            ancestors.add(commit)
            commit = commit.previousCommit?.let { Commit.newInstance(it) }
        }

        return ancestors
    }

    enum class ConflictResult {
        SAME,
        OURS,
        THEIRS,
        CONFLICT
    }
}