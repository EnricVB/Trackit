package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.User
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.nio.file.Files


/**
 * BlameHandler allows identifying the author of changes in a file within a repository.
 * It is based on the commit history to track the authorship of each line in the file.
 */
class BlameHandler : CommandHandler() {

    /**
     * Retrieves the content of a file within a specific commit.
     *
     * @param commit The commit where the file will be searched.
     * @param file The file to look for within the commit.
     * @return A list with the file's lines if found, or `null` if it does not exist in the commit.
     */
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

    /**
     * Compares the lines of the current file with the version stored in the commit and assigns authorship.
     *
     * @param originalString List of lines from the current file.
     * @param commitString List of lines from the file in the analyzed commit.
     * @param commit The commit where changes will be analyzed.
     * @param blameString StringBuilder where the analysis result will be stored.
     */
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

    /**
     * Generates a history of changes in a file, indicating the author of each line.
     *
     * @param file The file to be analyzed.
     * @param commit Commit from which the search for changes will begin.
     * @return A string with the authorship of each line in the file.
     */
    fun blame(file: File, commit: Commit): String {
        val originalString = Files.readAllLines(file.toPath()).toList()
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