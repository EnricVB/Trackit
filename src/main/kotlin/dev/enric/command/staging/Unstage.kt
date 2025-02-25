package dev.enric.command.staging

import dev.enric.core.Hash
import dev.enric.util.RepositoryFolderManager
import dev.enric.core.handler.staging.StagingHandler
import picocli.CommandLine.*
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*


@Command(
    name = "unstage",
    description = ["Remove a file from the staging area"]
)
class Unstage : Callable<Int> {
    @Parameters(index = "0", paramLabel = "path/hash", description = ["The path of the file/directory, or hash, to be unstaged"])
    lateinit var file: String

    private val repositoryFolder = RepositoryFolderManager().initFolder
    private val stagingHandler = StagingHandler()

    /**
     * Unstages a file from the staging area
     * @return 0 if the file was unstaged successfully, 1 otherwise
     */
    override fun call(): Int {
        val filePath = repositoryFolder.resolve(file)

        if (filePath.exists()) {
            unstageFile(filePath)
        } else {
            unstageHash(Hash(file))
        }

        return 0
    }

    /**
     * Unstage a file from the staging area
     * If the file is a directory, it will unstage all the files inside it
     * @param file The file to unstage
     */
    fun unstageFile(file: Path) {
        when {
            file.isDirectory() -> unstageFolder(file)
            else -> stagingHandler.unstage(file)
        }
    }

    /**
     * Unstage all the files inside a folder.
     * @param directory The folder to unstage
     * @see unstageFile
     */
    fun unstageFolder(directory: Path) {
        getFilesToStage(directory).forEach { stagingHandler.unstage(it) }
    }

    /**
     * Unstage a file from the staging area using the hash of the file
     * @param hash The hash of the file to unstage
     */
    fun unstageHash(hash: Hash) {
        stagingHandler.unstage(hash)
    }

    /**
     * Get all the files inside a folder to stage
     * @param directory The folder to get the files from
     * @return A list of all the files inside the folder
     */
    @OptIn(ExperimentalPathApi::class)
    fun getFilesToStage(directory: Path): List<Path> {
        return directory.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .filter { !it.isDirectory() && !it.toFile().toString().contains(".trackit") }
            .toList()
    }
}