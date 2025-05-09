package dev.enric.core.handler.repo

import dev.enric.core.handler.CommandHandler
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.common.SerializablePath
import dev.enric.util.repository.RepositoryFolderManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

/**
 * Class that handles the .ignore file in the repository.
 * It allows to ignore files and directories from being tracked.
 */
class IgnoreHandler : CommandHandler() {
    private val ignoreFile = RepositoryFolderManager().getInitFolderPath().resolve(".ignore")
    private val ignoredFilesCache = mutableListOf<String>()

    init {
        loadIgnoredFiles()
    }

    /**
     * Loads the ignored files from the .ignore file into the cache.
     * It reads the file line by line and adds each line to the cache.
     */
    private fun loadIgnoredFiles() {
        try {
            Files.lines(ignoreFile).map { it.trim() }.forEach { ignoredFilesCache.add(it) }
        } catch (e: IOException) {
            Logger.error("Error while reading .ignore file: ${e.message}")
        }
    }

    /**
     * Ignores a file or directory from the repository from being tracked.
     * It writes the path of the file or directory to the .ignore file in the repository.
     *
     * In case the path is a directory, it will ignore all the files and directories inside it.
     * @param path The file or directory of the file to be ignored
     */
    fun ignore(path: Path) {
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().getInitFolderPath())

        if (ignoredFilesCache.contains(relativePath.toString())) {
            Logger.warning("The file or directory is already being ignored")
            return
        }

        try {
            Logger.info("Ignoring $relativePath")
            if (Files.notExists(path)) {
                throw IllegalStateException("File or directory does not exist")
            }

            Files.writeString(ignoreFile, "$relativePath\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            ignoredFilesCache.add(relativePath.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if a file or directory is ignored, inside .ignore.
     * @param path The file or directory to check if it is ignored
     * @return True if the file or directory is being ignored, false otherwise
     */
    fun isIgnored(path: Path): Boolean {
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().getInitFolderPath())

        ignoredFilesCache.forEach { ignoredPattern ->
            if (matchesPattern(ignoredPattern, relativePath.toString())) {
                return true
            }
        }

        return false
    }

    /**
     * Matches a path with a glob-like pattern (supports * and **).
     * @param pattern The pattern to match, supporting * and **.
     * @param path The path to check.
     * @return True if the path matches the pattern, false otherwise.
     */
    private fun matchesPattern(pattern: String, path: String): Boolean {
        val regexPattern = pattern
            .replace("**", ".*")
            .replace("*", "[^/]*")

        // Compile and match the regex
        return Pattern.matches(regexPattern, path)
    }

    /**
     * Gets the list of ignored files and directories from the .ignore file.
     * @return List of ignored files and directories
     */
    fun getIgnoredFiles(): List<String> {
        return ignoredFilesCache
    }
}
