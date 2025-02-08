package dev.enric.util.staging

import dev.enric.core.Hash
import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import dev.enric.util.SerializablePath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

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
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().initFolder)

        if (checkIfOutdated(hash, relativePath)) return replaceOutdatedFile(hash, relativePath)
        if (!checkIfStaged(hash, relativePath)) return stageNewFile(hash, relativePath)

        return false
    }

    /**
     * Replaces a file that was previously staged.
     * It updates the path of the file in the staging index in case the file was renamed.
     * If the file was modified, the hash is updated.
     * @param hash The hash of the file to be replaced
     * @param path The path of the file to be replaced
     */
    private fun replaceOutdatedFile(hash: Hash, path: Path): Boolean {
        val tempFile = Files.createTempFile("temp", ".temp")

        return try {
            Files.newBufferedReader(stagingIndex).use { reader ->
                Files.newBufferedWriter(tempFile, StandardOpenOption.WRITE).use { writer ->
                    reader.forEachLine { line ->
                        val (lineHash, linePath) = line.split(" : ").let { Hash(it[0]) to Path.of(it[1]) }
                        val changedHash = (lineHash != hash && linePath == path)
                        val changedPath = (lineHash == hash && linePath != path)

                        if (changedHash || changedPath) {
                            writer.write("$hash : $path")
                        } else {
                            writer.write(line)
                        }

                        writer.newLine()
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

    /**
     * Stages a file to be committed.
     * It writes the hash of the file and the path of the file to the staging index in this format:
     * hash : path
     * @param relativePath The path of the file to be staged
     * @return True if the file was staged successfully, false otherwise
     */
    private fun stageNewFile(hash: Hash, relativePath: Path): Boolean {
        return try {
            Files.writeString(
                stagingIndex,
                "$hash : ${relativePath}\n",
                StandardOpenOption.APPEND
            )

            true
        } catch (e: IOException) {
            println("Error staging file $relativePath ${e.printStackTrace()}")
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

    fun unstage(unstagePath: Path): Boolean {
        val tempFile = Files.createTempFile("temp", ".temp")
        val relativeUnstagePath = SerializablePath.of(unstagePath).relativePath(RepositoryFolderManager().initFolder)

        return try {
            Files.newBufferedReader(stagingIndex).use { reader ->
                Files.newBufferedWriter(tempFile, StandardOpenOption.WRITE).use { writer ->
                    reader.forEachLine { line ->
                        val linePath = Path.of(line.split(" : ")[1])

                        if (!linePath.startsWith(relativeUnstagePath)) {
                            writer.write(line)
                            writer.newLine()
                        }
                    }
                }
            }

            Files.move(tempFile, stagingIndex, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            println("Error unstaging file $unstagePath ${e.printStackTrace()}")
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
     * @return True if the file is staged, false otherwise
     */
    private fun checkIfStaged(hash: Hash, path: Path): Boolean {
        return getStagedFiles().any { it.first == hash && it.second == path }
    }

    /**
     * Checks if a file was modified or renamed and is staged to be committed.
     * @param hash The hash of the file to be checked
     * @param path The path of the file to be checked
     * @return True if the staged file is present and has the same content, false otherwise
     */
    private fun checkIfOutdated(hash: Hash, path: Path): Boolean {
        return getStagedFiles().any { (it.first != hash && it.second == path) || (it.first == hash && it.second != path) }
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
                        val (hash, path) = line.split(" : ").let { Hash(it[0]) to Path.of(it[1]) }

                        stagedFiles.add(Pair(hash, path))
                    }
                }
            } catch (e: IOException) {
                println("Error getting staged files")
            }

            return stagedFiles
        }

        /**
         * Clears the staging area.
         * It removes all the files from the staging index.
         */
        @JvmStatic
        fun clearStagingArea() {
            val repositoryFolderManager = RepositoryFolderManager()
            val stagingIndex = repositoryFolderManager.getStagingIndexPath()

            try {
                Files.newBufferedWriter(stagingIndex, StandardOpenOption.WRITE).use { writer ->
                    writer.write("")
                }
            } catch (e: IOException) {
                println("Error clearing staging area ${e.printStackTrace()}")
            }
        }
    }
}