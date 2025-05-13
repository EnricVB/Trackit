package dev.enric.core.handler.repo

import dev.enric.domain.Hash
import dev.enric.domain.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.common.FileStatus
import dev.enric.util.common.FileStatus.*
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.*

/**
 * Manages the staging area in Trackit, handling the addition and removal of files before committing.
 * It interacts with the staging index, which keeps track of staged files and their hashes.
 * @param force If true, allows forcing changes in staging.
 */
@ExperimentalPathApi
data class StagingHandler(val force: Boolean = false) {
    private val repositoryFolderManager = RepositoryFolderManager()
    private val stagingIndex = repositoryFolderManager.getStagingIndexPath()
    private val lock = ReentrantLock()

    init {
        StagingCache.loadStagedFiles()
    }

    /**
     * Stages a file or directory to be committed.
     * If the path is a directory, all files inside it will be staged.
     *
     * @param path The path of the file or directory to be staged.
     */
    fun stagePath(path: Path) {
        IS_STAGING_FILES = true

        // Get the files to stage filtered by the shouldStage function
        val filesToStage = if (path.isDirectory()) {
            Logger.debug("Staging directory: $path")
            getFilesToStage(path)
        } else {
            Logger.debug("Staging file: $path")
            listOf(path).filter { shouldStage(it, path) }
        }

        Logger.info("Files to Stage: [${filesToStage.size}]")

        // Check if there are files to stage, if not, return
        if (filesToStage.isEmpty()) {
            Logger.warning("\nNo more files to stage.")
            IS_STAGING_FILES = false

            return
        }

        // Stage the files
        filesToStage.forEachIndexed { index, file ->
            val content = Content(Files.readAllBytes(file))
            val percent = ((index + 1).toFloat() / filesToStage.size) * 100

            stage(content, file)
            Logger.updateLine("Staging files... [${index + 1} / ${filesToStage.size}] ($percent%)")
        }

        println()
        IS_STAGING_FILES = false
    }


    /**
     * Get all the files inside a folder to stage
     * @param directory The folder to get the files from
     * @return A list of all the files inside the folder
     */
    @OptIn(ExperimentalPathApi::class)
    private fun getFilesToStage(directory: Path): List<Path> {
        val result = mutableListOf<Path>()
        var scanned = 0
        var toStage = 0

        directory
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .forEach { path ->
                Logger.updateLine("Scanning: [$scanned] files scanned,  [$toStage] to be staged")
                val normalized = path.normalize().toString().replace(".\\.", ".")
                val isIgnored = IgnoreHandler().isIgnored(Path.of(normalized))
                scanned++


                if (isIgnored) return@forEach
                if (path.isDirectory() || !shouldStage(path, directory)) return@forEach

                toStage++
                result.add(path)
            }

        println()
        return result
    }


    /**
     * Check if the file should be staged.
     *
     * A file should be staged if:
     * - The file exists
     * - The file is modified or untracked
     * - The file is ignored, but the force flag is set
     *
     * @param path The path of the file to check
     * @param originalPath The original path of the file
     * @return True if the file should be staged, false otherwise
     */
    private fun shouldStage(path: Path, originalPath: Path): Boolean {
        if (!path.exists()) {
            Logger.error("The file does not exist: $path")
            return false
        }

        return when (FileStatus.getStatus(path.toFile())) {
            MODIFIED, UNTRACKED -> true
            IGNORED -> if (force || path != originalPath) true else run {
                if (path == originalPath) {
                    Logger.error("The file is being ignored: $path")
                }
                false
            }

            else -> false
        }
    }

    /**
     * Stages a file to be committed by storing its hash and path in the staging index.
     * @param content The content of the file to be staged.
     * @param path The absolute path of the file to be staged.
     * @return True if the file was successfully staged, false otherwise.
     */
    private fun stage(content: Content, path: Path): Boolean {
        val hash = content.encode(true).first
        val relativePath = SerializablePath.of(path).relativePath(repositoryFolderManager.getInitFolderPath())

        if (checkIfOutdated(hash, relativePath)) return replaceOutdatedFile(hash, relativePath)
        if (!checkIfStaged(hash, relativePath)) return stageNewFile(hash, relativePath)

        StagingCache.saveStagedFile(hash, relativePath)
        return false
    }

    /**
     * Adds a new file to the staging index.
     * @param hash The hash of the file.
     * @param relativePath The relative path of the file.
     * @return True if the file was successfully staged, false otherwise.
     */
    private fun stageNewFile(hash: Hash, relativePath: Path): Boolean {
        if (StagingCache.containsHash(hash)) return false

        return try {
            lock.withLock {
                Files.writeString(
                    stagingIndex,
                    "$hash : $relativePath\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                )
            }

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
        val relativeUnstagePath =
            SerializablePath.of(unstagePath).relativePath(RepositoryFolderManager().getInitFolderPath())

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
        val indexCache = StagingCache.getStagedFiles().associate { it.first to it.second }
        return indexCache.contains(hash) && indexCache[hash] == path
    }

    /**
     * Checks if a file was modified or renamed and is staged to be committed.
     * @param hash The hash of the file to be checked
     * @param path The path of the file to be checked
     * @return True if the staged file is present and has the same content, false otherwise
     */
    private fun checkIfOutdated(hash: Hash, path: Path): Boolean {
        val indexCache = StagingCache.getStagedFiles().associate { it.first to it.second }
        return indexCache.contains(hash) && indexCache[hash] != path
    }

    object StagingCache {
        private var stagedFiles: List<Pair<Hash, Path>> = emptyList()

        fun getStagedFiles(): List<Pair<Hash, Path>> {
            if (stagedFiles.isEmpty()) {
                stagedFiles = loadStagedFiles()
            }

            return stagedFiles
        }

        fun containsHash(hash: Hash): Boolean {
            return getStagedFiles().any { it.first == hash }
        }

        fun saveStagedFile(hash: Hash, path: Path) {
            stagedFiles = getStagedFiles() + Pair(hash, path)
        }

        fun clearStagedFiles() {
            stagedFiles = emptyList()
        }

        fun loadStagedFiles(): List<Pair<Hash, Path>> {
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
        @Volatile
        var IS_STAGING_FILES: Boolean = false

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
                StagingCache.clearStagedFiles()
            } catch (e: IOException) {
                Logger.error("Error clearing staging area ${e.printStackTrace()}")
            }
        }
    }
}