package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.logger.Logger
import dev.enric.util.common.SerializablePath
import dev.enric.util.common.Utility
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path

class DiffHandler(
    fileFilter: String?
) : CommandHandler() {

    fun executeDiffBetweenCommits(commit1: Commit, commit2: Commit) {
        val allPaths = getAllFilesInTrees(commit1.tree, commit2.tree)

        allPaths.forEach {
            val versions = it.value
            val path = SerializablePath.of(it.key).relativePath(RepositoryFolderManager().getInitFolderPath())

            val version1 = versions.first?.let { hash -> String(Content.newInstance(hash).content) } ?: ""
            val version2 = versions.second?.let { hash -> String(Content.newInstance(hash).content) } ?: ""

            val diff = Utility.showDiff(version1, version2)

            Logger.log("$path\n$diff")
        }
    }

    fun executeDiffBetweenWorkdirAndCommit(commit: Commit) {
    }

    fun executeDiffInWorkdir() {
    }

    fun executeDIffInStagingArea() {
    }

    private fun getAllFilesInTrees(treeList1: List<Hash>, treeList2: List<Hash>): Map<Path, Pair<Hash?, Hash?>> {
        val result = mutableMapOf<Path, Pair<Hash?, Hash?>>()

        val map1 = mutableMapOf<Path, Hash>()
        val map2 = mutableMapOf<Path, Hash>()

        // Adds all paths from first tree list to map1
        treeList1.forEach { hash ->
            val tree = Tree.newInstance(hash)
            map1[tree.serializablePath.toPath()] = tree.content
        }

        // Adds all paths from second tree list to map2
        treeList2.forEach { hash ->
            val tree = Tree.newInstance(hash)
            map2[tree.serializablePath.toPath()] = tree.content
        }

        // Merges both maps to have Path -> Hash1, Hash2
        val allPaths = map1.keys + map2.keys
        for (path in allPaths) {
            val hash1 = map1[path]
            val hash2 = map2[path]

            result[path] = Pair(hash1, hash2)
        }

        return result
    }
}