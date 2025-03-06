package dev.enric.core.repo.ignore

import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object IgnoreHandler {
    private val repositoryFolderManager = RepositoryFolderManager()
    private val ignoreFile = repositoryFolderManager.initFolder.resolve(".ignore")

    /**
     * Ignores a file or directory from the repository from being tracked.
     * It writes the path of the file or directory to the .ignore file in the repository.
     *
     * In case the path is a directory, it will ignore all the files and directories inside it.
     * @param path The file or directory of the file to be ignored
     */
    fun ignore(path: Path) {
        val relativePath = SerializablePath.of(path).relativePath(repositoryFolderManager.initFolder)

        if(isIgnored(relativePath)) {
            Logger.log("The file or directory is already being ignored")
            return
        }

        try {
            Logger.log("Ignoring $relativePath")

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
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().initFolder)

        return getIgnoredFiles().any { relativePath.startsWith(it) }
    }

    /**
     * Gets the list of files and directories that are being ignored.
     * @return List of files and directories that are being ignored
     */
    fun getIgnoredFiles(): List<Path> {
        val ignore = repositoryFolderManager.initFolder.resolve(".ignore")
        val ignoredPaths = mutableListOf<Path>()

        try {
            Files.newBufferedReader(ignore).use { reader ->
                reader.forEachLine { ignoredPaths.add(Path.of(it)) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ignoredPaths.sortedByDescending { Files.isDirectory(it) }
    }
}