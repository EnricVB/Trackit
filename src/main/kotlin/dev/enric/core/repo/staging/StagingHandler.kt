package dev.enric.core.repo.staging

import dev.enric.domain.Hash
import dev.enric.domain.objects.Content
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Manages the staging area in Trackit, handling the addition and removal of files before committing.
 * It interacts with the staging index, which keeps track of staged files and their hashes.
 * @param force If true, allows forcing changes in staging.
 */
data class StagingHandler(val force: Boolean = false) {
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()

    /**
     * Stages a file to be committed by storing its hash and path in the staging index.
     * @param content The content of the file to be staged.
     * @param path The absolute path of the file to be staged.
     * @return True if the file was successfully staged, false otherwise.
     */
    fun stage(content: Content, path: Path): Boolean {
        val hash = content.encode(true).first
        val relativePath = SerializablePath.of(path).relativePath(repositoryFolderManager.initFolder)

        if (checkIfOutdated(hash, relativePath)) return replaceOutdatedFile(hash, relativePath)
        if (!checkIfStaged(hash, relativePath)) return stageNewFile(hash, relativePath)

        return false
    }

    /**
     * Replaces a previously staged file if its hash or path has changed.
     * @param hash The new hash of the file.
     * @param path The updated path of the file.
     * @return True if the replacement was successful, false otherwise.
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
            println("Error replacing outdated staged file $path: ${e.message}")
            false
        }
    }

    /**
     * Adds a new file to the staging index.
     * @param hash The hash of the file.
     * @param relativePath The relative path of the file.
     * @return True if the file was successfully staged, false otherwise.
     */
    private fun stageNewFile(hash: Hash, relativePath: Path): Boolean {
        return try {
            Files.writeString(
                stagingIndex,
                "$hash : $relativePath\n",
                StandardOpenOption.APPEND
            )
            true
        } catch (e: IOException) {
            println("Error staging file $relativePath: ${e.message}")
            false
        }
    }

    /**
     * Removes a file from the staging index.
     * @param hash The hash of the file to be unstaged.
     * @return True if the file was successfully unstaged, false otherwise.
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
            println("Error unstaging file $hash: ${e.message}")
            false
        }
    }

    /**
     * Removes a file from the staging index.
     * @param unstagePath The path of the file to be unstaged.
     * @return True if the file was successfully unstaged, false otherwise.
     */
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

    /**
     * Shows the differences between the working directory and the staging area.
     * @return A string with the differences between the working directory and the staging area
     */
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
                Files.write(stagingIndex, byteArrayOf())
            } catch (e: IOException) {
                println("Error clearing staging area ${e.printStackTrace()}")
            }
        }
    }
}