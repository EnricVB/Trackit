package dev.enric.core.repo.staging

import dev.enric.core.repo.ignore.IgnoreHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.CONTENT
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import dev.enric.util.common.FileStatusTypes
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File

object StatusHandler {

    fun getStatus(file: File): FileStatusTypes {
        val fileContent = file.readText()
        val hash = Content(fileContent.toByteArray()).generateKey()

        val contentExists = contentExists(hash)
        val isUpToDate = isUpToDate(file)
        val isStaged = StagingHandler.getStagedFiles().any { it.first == hash }
        val isIgnored = IgnoreHandler.isIgnored(file.toPath())

        return when {
            isStaged -> FileStatusTypes.STAGED
            contentExists && isUpToDate -> FileStatusTypes.UNMODIFIED
            contentExists && !isUpToDate -> FileStatusTypes.MODIFIED
            isIgnored -> FileStatusTypes.IGNORED
            else -> FileStatusTypes.UNTRACKED
        }
    }

    fun contentExists(hash: Hash): Boolean {
        val contentFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(CONTENT.hash.toString())
        val contentFile = contentFolder.resolve(hash.toString())

        return contentFile.toFile().exists()
    }

    fun isUpToDate(file: File): Boolean {
        val currentCommit = CommitIndex.getCurrentCommit()?.let { Commit.newInstance(it) } ?: return true

        currentCommit.tree.forEach { tree ->
            val treeObject = Tree.newInstance(tree)
            val treePath = treeObject.serializablePath.toString()

            if (treePath == file.path) {
                val treeContent = treeObject.content
                val fileContent = Content(file.readText().toByteArray())

                return treeContent == fileContent.generateKey()
            }
        }

        return false
    }
}