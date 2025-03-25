package dev.enric.core.handler.administration

import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.User
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.nio.file.Files

object BlameHandler {

    fun getCommitContent(commit: Commit, file: File): List<String>? {
        val repositoryFolderManager = RepositoryFolderManager()

        return commit.tree.find { treeHash ->
            val tree = Tree.newInstance(treeHash)
            val serializableTreePath = tree.serializablePath.relativePath(repositoryFolderManager.getInitFolderPath())
            val serializableFilePath = SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

            serializableTreePath == serializableFilePath
        }?.let { treeHash ->
            val tree = Tree.newInstance(treeHash)
            val content = tree.content.let { Content.newInstance(it) }
            String(content.content).lines()
        }
    }

    fun compareLines(originalString: List<String>, commitString: List<String>, commit: Commit, blameString: StringBuilder) {
        val author = User.newInstance(commit.author)

        for (i in originalString.indices) {
            if (i < commitString.size && originalString[i] == commitString[i]) {
                blameString.appendLine(
                    "^${commit.generateKey().abbreviate()} (${author.name} ${commit.date})  $i) ${commitString[i]}"
                )
            }
        }
    }

    fun blame(file: File, commit: Commit): String {
        val originalString = Files.readAllLines(file.toPath())
        val blameString = StringBuilder()

        var currentCommit: Commit? = commit

        while (currentCommit != null) {
            val commitString = getCommitContent(currentCommit, file)

            if (commitString.isNullOrEmpty()) {
                currentCommit = currentCommit.previousCommit?.let { Commit.newInstance(it) }
                continue
            }

            compareLines(originalString, commitString, currentCommit, blameString)

            currentCommit = currentCommit.previousCommit?.let { Commit.newInstance(it) }
        }

        return blameString.toString()
    }
}