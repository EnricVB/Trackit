package dev.enric.core.handler.repo.staging

import dev.enric.domain.Hash
import dev.enric.domain.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the staging area in Trackit, handling the addition and removal of files before committing.
 * It interacts with the staging index, which keeps track of staged files and their hashes.
 * @param force If true, allows forcing changes in staging.
 */
data class StagingHandler(val force: Boolean = false) {
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()
    private val stagedFilesCache = ConcurrentHashMap<String, Path>()

    /**
     * Stages a file to be committed by storing its hash and path in the staging index.
     * @param path The absolute path of the file to be staged.
     * @return True if the file was successfully staged, false otherwise.
     */
    fun stage(path: Path): Boolean {
        val content = Content(Files.readString(path).toByteArray())
        content.encode(true)

        return stage(content, path)
    }

    /**
     * Stages a file to be committed by storing its hash and path in the staging index.
     * @param content The content of the file to be staged.
     * @param path The absolute path of the file to be staged.
     * @return True if the file was successfully staged, false otherwise.
     */
    fun stage(content: Content, path: Path): Boolean {
        val hash = content.encode(true).first
        val relativePath = SerializablePath.of(path).relativePath(repositoryFolderManager.getInitFolderPath())

        if (checkIfOutdated(hash, relativePath)) {
            return replaceOutdatedFile(hash, relativePath)
        }

        if (!checkIfStaged(hash, relativePath)) {
            return stageNewFile(hash, relativePath)
        }

        return false
    }

    /**
     * Replaces a previously staged file if its hash or path has changed.
     * @param hash The new hash of the file.
     * @param path The updated path of the file.
     * @return True if the replacement was successful, false otherwise.
     */
    private fun replaceOutdatedFile(hash: Hash, path: Path): Boolean {
        return try {
            // Read and replace file content in memory first, then write to the file at once
            val stagingIndexContent = Files.readAllLines(stagingIndex).toMutableList()

            // Update the index content in memory
            stagingIndexContent.replaceAll { line ->
                val (lineHash, linePath) = line.split(" : ").let { Hash(it[0]) to Path.of(it[1]) }
                if (lineHash != hash && linePath == path || lineHash == hash && linePath != path) {
                    "$hash : $path"
                } else {
                    line
                }
            }

            Files.write(stagingIndex, stagingIndexContent)
            true
        } catch (e: Exception) {
            Logger.error("Failed to replace outdated file: $e")
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
        try {
            Files.write(stagingIndex, "$hash : $relativePath\n".toByteArray(), StandardOpenOption.APPEND)
            stagedFilesCache[hash.toString()] = relativePath
            return true
        } catch (e: Exception) {
            Logger.error("Failed to stage new file: $e")
            return false
        }
    }

    /**
     * Removes a file from the staging index.
     * @param hash The hash of the file to be unstaged.
     * @return True if the file was successfully unstaged, false otherwise.
     */
    fun unstage(hash: Hash): Boolean {
        return unstageStagingIndex { line ->
            !line.startsWith(hash.toString())
        }
    }

    /**
     * Removes a file from the staging index by its path.
     * @param unstagePath The path of the file to be unstaged.
     * @return True if the file was successfully unstaged, false otherwise.
     */
    fun unstage(unstagePath: Path): Boolean {
        val relativeUnstagePath = SerializablePath.of(unstagePath).relativePath(RepositoryFolderManager().getInitFolderPath())
        return unstageStagingIndex { line ->
            val linePath = Path.of(line.split(" : ")[1])
            !linePath.startsWith(relativeUnstagePath)
        }
    }

    /**
     * Unstage the staging index by filtering out lines based on a condition.
     * This approach avoids using temporary files by updating the file in place.
     * @param filterCondition A function that determines whether a line should be included.
     * @return True if the modification was successful, false otherwise.
     */
    private fun unstageStagingIndex(filterCondition: (String) -> Boolean): Boolean {
        return try {
            Files.newBufferedReader(stagingIndex).use { reader ->
                // This allows us to overwrite the file directly with updated content
                val writer = Files.newBufferedWriter(stagingIndex, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                var firstLine = true
                reader.forEachLine { line ->
                    if (filterCondition(line)) {
                        // Write the line back to the file if it satisfies the filter condition
                        if (!firstLine) writer.newLine()
                        writer.write(line)
                        firstLine = false
                    }
                }
                writer.flush() // Ensure all data is written
            }
            true
        } catch (e: IOException) {
            Logger.error("Error modifying the staging index: ${e.message}")
            false
        }
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
                Logger.error("Error getting staged files")
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
                Logger.error("Error clearing staging area ${e.printStackTrace()}")
            }
        }
    }
}