package dev.enric.util.staging

import dev.enric.core.Hash
import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

data class StagingHandler(val force: Boolean = false) { // TODO: Implement force option
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()

    /**
     * Stages a file to be committed.
     * It writes the hash of the file and the path of the file to the staging index in this format:
     * hash : path
     * @param content The content of the file to be staged
     * @param path The path of the file to be staged
     * @return True if the file was staged successfully, false otherwise
     */
    fun stage(content: Content, path: Path): Boolean {
        val hash = content.encode(true).first
        if (checkIfStaged(hash, path)) return false

        return try {
            Files.writeString(
                stagingIndex,
                "$hash : ${relativePath(path)}\n",
                StandardOpenOption.APPEND
            )

            true
        } catch (e: IOException) {
            println("Error staging file $path ${e.printStackTrace()}")
            false
        }
    }

    /**
     * Unstages a file that was previously staged.
     * It removes the file from the staging index.
     * @param hash The hash of the file to be unstaged
     * @return True if the file was unstaged successfully, false otherwise
     */
    fun unstage(hash: Hash): Boolean {
        val tempFile = Files.createTempFile("temp", ".temp")

        return try {
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
            true
        } catch (e: IOException) {
            println("Error unstaging file $hash ${e.printStackTrace()}")
            false
        }
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

    /**
     * Checks if a file is staged to be committed.
     * @param hash The hash of the file to be checked
     * @param path The path of the file to be checked
     * @return True if the staged file is present and has the same content, false otherwise
     */
    fun checkIfStaged(hash: Hash, path: Path): Boolean {
        return getStagedFiles().any { it.first == hash && it.second == path }
    }

    /**
     * @param file The file to get the relative path from
     * @return The relative path of the file.
     */
    fun relativePath(file: Path): Path {
        return Path(file.toString().replace(RepositoryFolderManager().initFolder.toString(), "").substring(1))
    }

    companion object {
        /**
         * Gets the files that are staged to be committed.
         * @return A list of pairs with the hash of the file and the path of the file
         */
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