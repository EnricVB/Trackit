package dev.enric.core.handler.admin

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRow.Tag.*
import com.github.difflib.text.DiffRowGenerator
import dev.enric.core.handler.CommandHandler
import dev.enric.core.handler.repo.StagingHandler.StagingCache
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
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
@OptIn(ExperimentalPathApi::class)
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
     * Displays diffs for all given file paths. Filters by [fileFilter] if provided.
     * Skips files with identical content.
     *
     * @param allPaths Map of file paths to pairs of content hashes (first vs second version).
     */
    private fun showDiffs(allPaths: Map<Path, Pair<Hash?, Hash?>>) {
        if (!fileFilter.isNullOrEmpty()) {
            Logger.info("Showing diffs for files containing '$fileFilter'")
        }

        allPaths.forEach {
            val versions = it.value
            val path = SerializablePath.of(it.key).relativePath(RepositoryFolderManager().getInitFolderPath())

            // Skip if path doesn't match filter
            if (!fileFilter.isNullOrEmpty() && !path.toString().contains(fileFilter)) return@forEach

            val version1 = versions.first?.let { hash -> String(Content.newInstance(hash).content) } ?: ""
            val version2 = versions.second?.let { hash -> String(Content.newInstance(hash).content) } ?: ""

            // Skip identical content
            if (version1 == version2) return@forEach

            Logger.info("File: $path")
            Logger.info(fileDiff(version1, version2))
            Logger.info("--------------------")
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

        val repoRoot = RepositoryFolderManager().getInitFolderPath()

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
        val repositoryFolderManager = RepositoryFolderManager()

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
        return StagingCache.getStagedFiles().map {
            val tree = Tree(it.second, it.first)
            tree.encode(true).first
        }
    }

    /**
     * Generates a diff between two text strings.
     * This method uses the DiffRowGenerator to create a diff representation.
     *
     * The diff is formatted with:
     *
     * - Lines starting with "+" indicate additions.
     * - Lines starting with "-" indicate deletions.
     * - Lines starting with " " indicate unchanged lines.
     *
     * @param text1 The first text string.
     * @param text2 The second text string.
     *
     * @return A string representing the diff between the two texts.
     */
    fun fileDiff(text1: String, text2: String): String {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .ignoreWhiteSpaces(true)
            .reportLinesUnchanged(true)
            .mergeOriginalRevised(false)
            .inlineDiffByWord(false)
            .replaceOriginalLinefeedInChangesWithSpaces(true)
            .build()

        val rows: List<DiffRow> = generator.generateDiffRows(text1.split("\n"), text2.split("\n"))
        val diff = StringBuilder()

        rows.forEach {
            when (it.tag ?: EQUAL) {
                INSERT -> diff.appendLine("+ ${ColorUtil.insertLine(it.newLine)}")
                DELETE -> diff.appendLine("- ${ColorUtil.deleteLine(it.oldLine)}")
                CHANGE -> diff.appendLine(
                    """
                    - ${if (it.oldLine.isNotBlank()) ColorUtil.deleteLine(it.oldLine) else ""}
                    + ${if (it.newLine.isNotBlank()) ColorUtil.deleteLine(it.oldLine) else ""}"""
                        .trimIndent()
                )

                else -> diff.appendLine("  ${it.oldLine}")
            }
        }

        return diff.toString()
    }

}
