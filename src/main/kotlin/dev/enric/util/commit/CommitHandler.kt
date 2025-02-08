package dev.enric.util.commit

import dev.enric.core.Hash
import dev.enric.core.objects.Commit
import dev.enric.core.objects.Content
import dev.enric.core.objects.Tree
import dev.enric.util.SerializablePath
import dev.enric.util.staging.StagingHandler
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.pathString

class CommitHandler {

    fun createCommitTree() {
        val trees = mapStagedFilesToTree(StagingHandler.getStagedFiles())
        val commit = Commit(tree = trees.map { it.encode(true).first })
        commit.encode(true)

        StagingHandler.clearStagingArea()
    }

    fun checkout(commitHash: Hash) {
        checkout(Commit.newInstance(commitHash))
    }

    fun checkout(commit: Commit) {
        commit.tree.forEach {
            val tree = Tree.newInstance(it)
            Path.of(tree.serializablePath.pathString).parent.toFile().mkdirs()

            if(!tree.serializablePath.toPath().toFile().exists()) {
                Files.writeString(
                    tree.serializablePath.toPath(),
                    String(Content.newInstance(tree.hash).content),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                )
            }
        }
    }

    /**
     * Maps the staged files, a pair ot hash and path, to a list of Tree objects.
     * @param stagedFiles List of pairs of hash and path that are going to be mapped to Tree objects.
     * @return List of Tree objects that represent the staged files.
     */
    private fun mapStagedFilesToTree(stagedFiles: List<Pair<Hash, Path>>): List<Tree> {
        return stagedFiles.mapNotNull { stagedFile ->
            val rootPath = File(".").absoluteFile.toPath()
            val relativePath = stagedFile.second.pathString.replace("\\.\\", "")
            val path = rootPath.resolve(relativePath)

            val fileContent = safeFileRead(path.toFile())

            if(fileContent != null) {
                val content = fileContent.encode(true).first
                return@mapNotNull Tree(SerializablePath.of(path), content)
            }

            return@mapNotNull null
        }
    }


    /**
     * Reads the content of a file and returns it as a Content object.
     * Even if the file is being used by another process, it will be read.
     * @param file File that is going to be read.
     * @return Content object that represents the content of the file. Null if the file could not be read.
     */
    private fun safeFileRead(file: File): Content? {
        try {
            FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
                val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

                return Content(ByteArray(buffer.remaining()).apply { buffer.get(this) })
            }
        } catch (exception : Exception) {
            return null
        }
    }

    fun isFileUpToDateToCommit(file: File, commit: Commit): Boolean {
        val content = Content(Files.readAllBytes(file.toPath()))

        return commit.findFile(content, file.toPath()) != null
    }
}