package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.core.handler.repo.staging.StatusHandler.repositoryFolderManager
import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.exceptions.CommitNotFoundException
import dev.enric.logger.Logger
import dev.enric.util.common.SerializablePath
import dev.enric.util.common.Utility
import dev.enric.util.index.BranchIndex
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * Handles diff operations between Workdir, Staging Area, Commits, and Branches.
 * Allows showing differences in content between various repository states.
 *
 * @param fileFilter Optional file filter to only diff matching paths.
 */
data class DiffHandler(
    val fileFilter: String?
) : CommandHandler() {

    /**
     * Shows differences between the current Working Directory and the Staging Area.
     * Only diffs files present in both areas.
     */
    fun executeDiffInWorkdir() {
        val allPaths = getAllFilesInTrees(getStagingAreaHashes(), getWorkDirHashes(), onlyCommonFiles = true)
        showDiffs(allPaths)
    }

    /**
     * Shows differences between a given Commit and the current Working Directory.
     *
     * @param commit The commit to compare against.
     */
    fun executeDiffBetweenWorkdirAndCommit(commit: Commit) {
        val allPaths = getAllFilesInTrees(commit.tree, getWorkDirHashes(), onlyCommonFiles = false)
        showDiffs(allPaths)
    }

    /**
     * Shows differences between two given Commits.
     *
     * @param commit1 The first commit to compare.
     * @param commit2 The second commit to compare.
     */
    fun executeDiffBetweenCommits(commit1: Commit, commit2: Commit) {
        val allPaths = getAllFilesInTrees(commit1.tree, commit2.tree, onlyCommonFiles = false)
        showDiffs(allPaths)
    }

    /**
     * Shows differences between two Branches based on their respective HEAD Commits.
     *
     * @param branch1 The first branch.
     * @param branch2 The second branch.
     * @throws CommitNotFoundException If the HEAD of either branch is not found.
     */
    fun executeDiffBetweenBranches(branch1: Branch, branch2: Branch) {
        val commit1 = BranchIndex.getBranchHead(branch1.generateKey())
            ?: throw CommitNotFoundException("Branch Head for ${branch1.name} not found")
        val commit2 = BranchIndex.getBranchHead(branch2.generateKey())
            ?: throw CommitNotFoundException("Branch Head for ${branch2.name} not found")

        val allPaths = getAllFilesInTrees(commit1.tree, commit2.tree, onlyCommonFiles = false)
        showDiffs(allPaths)
    }

    /**
     * Displays diffs for all given file paths. Filters by [fileFilter] if provided.
     * Skips files with identical content.
     *
     * @param allPaths Map of file paths to pairs of content hashes (first vs second version).
     */
    private fun showDiffs(allPaths: Map<Path, Pair<Hash?, Hash?>>) {
        if (!fileFilter.isNullOrEmpty()) {
            Logger.log("Showing diffs for files containing '$fileFilter'")
        }

        allPaths.forEach {
            val versions = it.value
            val path = SerializablePath.of(it.key).relativePath(repositoryFolderManager.getInitFolderPath())

            // Skip if path doesn't match filter
            if (!fileFilter.isNullOrEmpty() && !path.toString().contains(fileFilter)) return@forEach

            val version1 = versions.first?.let { hash -> String(Content.newInstance(hash).content) } ?: ""
            val version2 = versions.second?.let { hash -> String(Content.newInstance(hash).content) } ?: ""

            // Skip identical content
            if (version1 == version2) return@forEach

            Logger.log("File: $path")
            val diff = Utility.fileDiff(version1, version2)
            Logger.log(diff)
            Logger.log("--------------------")
        }
    }

    /**
     * Retrieves all file paths from two lists of Tree hashes.
     * Compares and pairs their content hashes.
     *
     * @param treeList1 List of Tree hashes from the first source.
     * @param treeList2 List of Tree hashes from the second source.
     * @param onlyCommonFiles If true, only returns paths present in both sources.
     * @return Map of file paths to pairs of content hashes (first, second).
     */
    private fun getAllFilesInTrees(
        treeList1: List<Hash>,
        treeList2: List<Hash>,
        onlyCommonFiles: Boolean
    ): Map<Path, Pair<Hash?, Hash?>> {
        val result = mutableMapOf<Path, Pair<Hash?, Hash?>>()
        val map1 = mutableMapOf<Path, Hash>()
        val map2 = mutableMapOf<Path, Hash>()

        val repoRoot = repositoryFolderManager.getInitFolderPath()

        // Populate map1 with paths and hashes from treeList1
        treeList1.forEach { hash ->
            val tree = Tree.newInstance(hash)
            val realPath = tree.serializablePath.toPath().toRealPath().normalize().relativeTo(repoRoot)
            map1[realPath] = tree.content
        }

        // Populate map2 with paths and hashes from treeList2
        treeList2.forEach { hash ->
            val tree = Tree.newInstance(hash)
            val realPath = tree.serializablePath.toPath().toRealPath().normalize().relativeTo(repoRoot)
            map2[realPath] = tree.content
        }

        // Determine paths to include based on intersection or union
        val pathsToConsider = if (onlyCommonFiles) {
            map1.keys.intersect(map2.keys)
        } else {
            map1.keys.union(map2.keys)
        }

        for (path in pathsToConsider) {
            val hash1 = map1[path]
            val hash2 = map2[path]
            result[path] = Pair(hash1, hash2)
        }

        return result
    }

    /**
     * Retrieves all file hashes from the current Working Directory.
     *
     * @return List of Tree hashes representing files in the Working Directory.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun getWorkDirHashes(): List<Hash> {
        return repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .toMutableList()
            .filterNot {
                it.toFile().isDirectory ||
                        it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath()) ||
                        it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath()) ||
                        it.toRealPath() == repositoryFolderManager.getInitFolderPath()
            }.map {
                val content = Content(it.toFile().readBytes())
                val tree = Tree(it.toFile().toPath(), content.encode(true).first)
                tree.encode(true).first
            }
    }

    /**
     * Retrieves all file hashes from the current Staging Area.
     *
     * @return List of Tree hashes representing staged files.
     */
    private fun getStagingAreaHashes(): List<Hash> {
        return StagingHandler.getStagedFiles().map {
            val tree = Tree(it.second, it.first)
            tree.encode(true).first
        }
    }
}
