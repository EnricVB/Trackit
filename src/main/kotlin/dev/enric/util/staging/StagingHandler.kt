package dev.enric.util.staging

import dev.enric.core.Hash
import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

data class StagingHandler(val force: Boolean = false) { // TODO: Implement force option
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()

    fun stage(content: Content, path: Path): Boolean {
        val hash = content.encode(true).first
        val pathString = path.toString().replace("\\.\\", "\\")

        if(checkIfStaged(hash, path)) return false

        try {
            Files.writeString(
                stagingIndex.toAbsolutePath(),
                "$hash : $pathString\n",
                StandardOpenOption.APPEND
            )

            return true
        } catch (e: IOException) {
            println("Error staging file $pathString ${e.printStackTrace()}")
        }

        return false
    }

    fun unstage(hash: Hash): Boolean {
        val tempFile = Files.createTempFile("temp", ".temp")

        try {
            Files.newBufferedReader(stagingIndex).use { reader ->
                Files.newBufferedWriter(tempFile, StandardOpenOption.WRITE).use { writer ->
                    reader.forEachLine { line ->
                        if (!line.startsWith(hash.toString())) {
                            writer.write(line)
                            writer.newLine()
                        }
                    }
                }
            }

            Files.move(tempFile, stagingIndex, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            println("Error unstaging file $hash ${e.printStackTrace()}")
        }

        return false
    }

    fun getStatus(): String {
        val stagedFiles = getStagedFiles()
        var statusMessage = ""

        stagedFiles.forEach { (hash, path) ->
            statusMessage += "$hash : $path\n"
        }

        return statusMessage
    }

    fun getStatus(hash: Hash, hideUntracked: Boolean = false): String {
        return "Getting staged files for hash $hash"
    }

    fun showDifferences(): String {
        return "Showing differences"
    }

    fun checkIfStaged(hash: Hash, path : Path): Boolean {
        return getStagedFiles().any { it.first == hash && it.second == path }
    }

    companion object {
        @JvmStatic
        fun getStagedFiles(): List<Pair<Hash, Path>> {
            val repositoryFolderManager = RepositoryFolderManager()
            val stagingIndex = repositoryFolderManager.getStagingIndexPath()
            val stagedFiles = mutableListOf<Pair<Hash, Path>>()

            try {
                Files.newBufferedReader(stagingIndex).use { reader ->
                    reader.forEachLine { line ->
                        val hash = Hash(line.split(" : ")[0])
                        val path = Path.of(line.split(" : ")[1])

                        stagedFiles.add(Pair(hash, path))
                    }
                }
            } catch (e: IOException) {
                println("Error getting staged files")
            }

            return stagedFiles
        }
    }
}