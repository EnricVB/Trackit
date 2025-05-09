package dev.enric.core.handler.repo

import dev.enric.domain.Hash
import dev.enric.domain.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manages the staging area in Trackit, handling the addition and removal of files before committing.
 * It interacts with the staging index, which keeps track of staged files and their hashes.
 * @param force If true, allows forcing changes in staging.
 */
data class StagingHandler(val force: Boolean = false) {
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()
    private val indexCache: MutableMap<Hash, Path> = mutableMapOf()
    private val lock = ReentrantLock()

    init {
        loadIndexIntoCache()
    }

    /**
     * Loads the staging index into the cache for faster access.
     * This is done by reading the staging index file line by line and storing the hash and path in a map.
     *
     * This method is called during the initialization of the StagingHandler.
     * It is not thread-safe and should be called only once.
     *
     * @throws IOException if an error occurs while reading the staging index file.
     */
    private fun loadIndexIntoCache() {
        try {
            Files.lines(stagingIndex).forEach { line ->
                val (hash, path) = line.split(" : ")
                indexCache[Hash(hash)] = Path.of(path)
            }
        } catch (e: IOException) {
            Logger.error("Error loading staging index into cache: ${e.message}")
        }
    }

    /**
     * Stages a file to be committed by storing its hash and path in the staging index.
     * @param path The absolute path of the file to be staged.
     * @return True if the file was successfully staged, false otherwise.
     */
    fun stage(path: Path): Boolean {
        val content = Content(Files.readAllBytes(path))
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

        if (checkIfOutdated(hash, relativePath)) return replaceOutdatedFile(hash, relativePath)
        if (!checkIfStaged(hash, relativePath)) return stageNewFile(hash, relativePath)

        return false
    }

    /**
     * Adds a new file to the staging index.
     * @param hash The hash of the file.
     * @param relativePath The relative path of the file.
     * @return True if the file was successfully staged, false otherwise.
     */
    private fun stageNewFile(hash: Hash, relativePath: Path): Boolean {
        if (indexCache.contains(hash)) return false

        return try {
            lock.withLock {
                Files.writeString(
                    stagingIndex,
                    "$hash : $relativePath\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                )
            }
            indexCache[hash] = relativePath
            true
        } catch (e: IOException) {
            Logger.error("Error staging file $relativePath: ${e.message}")
            false
        }
    }

    /**
     * Replaces a previously staged file if its hash or path has changed.
     * @param hash The new hash of the file.
     * @param path The updated path of the file.
     * @return True if the replacement was successful, false otherwise.
     */
    private fun replaceOutdatedFile(hash: Hash, path: Path): Boolean {
        lock.withLock {
            val tempFile = Files.createTempFile("temp", ".temp")

            try {
                Files.newBufferedReader(stagingIndex).use { reader ->
                    Files.newBufferedWriter(tempFile, StandardOpenOption.WRITE).use { writer ->
                        reader.forEachLine { line ->
                            val (lineHash, linePath) = line.split(" : ")
                            if (lineHash == hash.toString() && linePath != path.toString()) {
                                writer.write("$hash : $path")
                            } else {
                                writer.write(line)
                            }
                            writer.newLine()
                        }
                    }
                }

                Files.move(tempFile, stagingIndex, StandardCopyOption.REPLACE_EXISTING)
                indexCache[hash] = path
                return true
            } catch (e: IOException) {
                Logger.error("Error replacing outdated staged file $path: ${e.message}")
                return false
            }
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
            Logger.error("Error unstaging file with hash $hash: ${e.message}")
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
        val relativeUnstagePath = SerializablePath.of(unstagePath).relativePath(RepositoryFolderManager().getInitFolderPath())

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
            Logger.error("Error unstaging file $unstagePath ${e.printStackTrace()}")
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
        return indexCache.contains(hash) && indexCache[hash] == path
    }

    /**
     * Checks if a file was modified or renamed and is staged to be committed.
     * @param hash The hash of the file to be checked
     * @param path The path of the file to be checked
     * @return True if the staged file is present and has the same content, false otherwise
     */
    private fun checkIfOutdated(hash: Hash, path: Path): Boolean {
        return indexCache.contains(hash) && indexCache[hash] != path
    }

    object StagingCache {
        private var stagedFiles: List<Pair<Hash, Path>>? = null

        fun getStagedFiles(): List<Pair<Hash, Path>> {
            if (stagedFiles == null) {
                stagedFiles = loadStagedFiles()
            }
            return stagedFiles!!
        }

        private fun loadStagedFiles(): List<Pair<Hash, Path>> {
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
    }

    companion object {

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