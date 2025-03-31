package dev.enric.core.handler.repo.ignore

import dev.enric.core.handler.CommandHandler
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Class that handles the .ignore file in the repository.
 * It allows to ignore files and directories from being tracked.
 */
class IgnoreHandler : CommandHandler() {
    private val ignoreFile = RepositoryFolderManager().getInitFolderPath().resolve(".ignore")

    /**
     * Ignores a file or directory from the repository from being tracked.
     * It writes the path of the file or directory to the .ignore file in the repository.
     *
     * In case the path is a directory, it will ignore all the files and directories inside it.
     * @param path The file or directory of the file to be ignored
     */
    fun ignore(path: Path) {
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().getInitFolderPath())

        if(isIgnored(relativePath)) {
            Logger.log("The file or directory is already being ignored")
            return
        }

        try {
            Logger.log("Ignoring $relativePath")

            if (Files.notExists(path)) {
                throw IllegalStateException("File or directory does not exist")
            }

            Files.writeString(ignoreFile, "$relativePath\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
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

        return getIgnoredFiles().any { relativePath.startsWith(it) }
    }

    /**
     * Gets the list of ignored files and directories from the .ignore file.
     * @return List of ignored files and directories
     */
    fun getIgnoredFiles(): List<Path> {
        return try {
            Files.lines(ignoreFile).map { Paths.get(it) }.toList().sortedDescending()
        } catch (e: IOException) {
            Logger.error("Error while reading .ignore file: ${e.message}")
            emptyList()
        }
    }
}