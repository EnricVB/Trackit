package dev.enric.util.ignore

import dev.enric.util.RepositoryFolderManager
import dev.enric.util.SerializablePath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object IgnoreHandler {
    private val repositoryFolderManager = RepositoryFolderManager()

    /**
     * Ignores a file or directory from the repository from being tracked.
     * It writes the path of the file or directory to the .ignore file in the repository.
     *
     * In case the path is a directory, it will ignore all the files and directories inside it.
     * @param path The file or directory of the file to be ignored
     */
    fun ignore(path: Path) {
        val ignore = repositoryFolderManager.initFolder.resolve(".ignore")
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().initFolder)

        if(isIgnored(relativePath)) return

        try {
            Files.writeString(ignore, "$relativePath\n", StandardOpenOption.APPEND)
        } catch (e: IOException) {
            println("Error ignoring file $path ${e.printStackTrace()}")
        }
    }

    fun isIgnored(path: Path): Boolean {
        val relativePath = SerializablePath.of(path).relativePath(RepositoryFolderManager().initFolder)

        return getIgnoredFiles().any {
            println("Comparing $relativePath with $it")
            relativePath.startsWith(it)
        }
    }

    fun getIgnoredFiles(): List<Path> {
        val ignore = repositoryFolderManager.initFolder.resolve(".ignore")
        val ignoredPaths = mutableListOf<Path>()

        try {
            Files.newBufferedReader(ignore).use { reader ->
                reader.forEachLine { ignoredPaths.add(Path.of(it)) }
            }
        } catch (e: IOException) {
            println("Error getting ignored files ${e.printStackTrace()}")
        }

        return ignoredPaths.sortedByDescending { Files.isDirectory(it) }
    }
}