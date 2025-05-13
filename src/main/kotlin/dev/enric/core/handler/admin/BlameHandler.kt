package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.domain.objects.User
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.SerializablePath
import dev.enric.util.common.Utility
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.nio.file.Files


/**
 * BlameHandler allows identifying the author of changes in a file within a repository.
 * It is based on the commit history to track the authorship of each line in the file.
 */
class BlameHandler : CommandHandler() {
    /**
     * Generates a history of changes in a file, indicating the author of each line.
     *
     * @param file The file to be analyzed.
     * @param commit Commit from which the search for changes will begin.
     * @return A string with the authorship of each line in the file.
     */
    fun blame(file: File, commit: Commit): String {
        val originalString = Files.readAllLines(file.toPath()).toList()
        val blameString = StringBuilder().append(getCommitContent(commit, file)?.joinToString { "\n" })

        var currentCommit: Commit? = commit

        // Iterate through the commit history until the initial commit is reached
        while (currentCommit != null) {
            // If the commitString is null, it means the file was not found in this commit
            // So we can stop searching
            val commitString = getCommitContent(currentCommit, file) ?: break

            // Compare the lines of the current file with the version stored in the commit and assign authorship
            compareLines(originalString, commitString, currentCommit, blameString)

            // If the commitString is not null, it means the file was found in this commit
            // So we can searching in previous commits
            currentCommit = currentCommit.previousCommit?.let { Commit.newInstance(it) }
        }

        return blameString.toString()
    }

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
            val serializableFilePath =
                SerializablePath.of(file.path).relativePath(repositoryFolderManager.getInitFolderPath())

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
    fun compareLines(
        originalString: List<String>,
        commitString: List<String>,
        commit: Commit,
        blameString: StringBuilder
    ) {
        val author = User.newInstance(commit.author)
        val lines = blameString.toString().lines().toMutableList()

        // Per each line in the originalString, check if it exists in the commitString
        // If it exists, it means it was added in this commit
        // If it doesn't exist, it means it was added in a previous commit
        for (lineIndex in originalString.indices) {
            val isAddedOnThisCommit = commitString.contains(originalString[lineIndex])
            val padStart = lines.size.toString().length - lineIndex.toString().length

            // If the line exists in the commitString, it means it was added in this commit
            if (isAddedOnThisCommit) {
                lines[lineIndex] = "[${lineIndex}${"]".padStart(padStart + 1, ' ')}" +
                        "${ColorUtil.hashColor(commit.generateKey().abbreviate())}^ | " +
                        "${author.name} | " +
                        "${Utility.formatDateTime(commit.date, "yyyy-MM-dd HH:mm:ss")}) | " +
                        originalString[lineIndex]
            }
        }

        blameString.clear()
        blameString.append(lines.joinToString("\n"))
    }
}