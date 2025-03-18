package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Path
import kotlin.io.path.*


@Command(
    name = "unstage",
    description = ["Remove a file from the staging area"],
    mixinStandardHelpOptions = true,
)
class Unstage : TrackitCommand() {

    /**
     * The path of the file/directory, or hash, to be unstaged.
     *
     * The path can be a file or a directory. If it is a directory, all the files inside it will be unstaged.
     *
     * The path can also be a hash. In this case, the file with the hash will be unstaged.
     */
    @Parameters(
        index = "0",
        paramLabel = "path/hash",
        description = ["The path of the file/directory, or hash, to be unstaged"]
    )
    lateinit var file: String

    private val repositoryFolder = RepositoryFolderManager().getInitFolderPath()
    private val stagingHandler = StagingHandler()

    /**
     * Unstages a file from the staging area
     * @return 0 if the file was unstaged successfully, 1 otherwise
     */
    override fun call(): Int {
        super.call()
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
            file.isDirectory() -> {
                unstageFolder(file)
            }

            else -> {
                Logger.log("Unstaging file: $file")
                stagingHandler.unstage(file)
            }
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
        Logger.log("Staging file with hash: $hash")

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