package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.repo.ignore.IgnoreHandler
import dev.enric.core.repo.staging.StagingHandler
import dev.enric.domain.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*


@Command(
    name = "stage",
    description = ["Stage files to be committed"],
    mixinStandardHelpOptions = true,
)
class Stage : TrackitCommand() {
    /**
     * Force the staging of files, even if they are being ignored. This will override the ignore rules.
     */
    @Option(names = ["--force"], description = ["Force the staging of files"])
    var force = false

    /**
     * The path of the file/directory to be staged, relative to the repository folder.
     *
     * If the file is a folder, all the files inside the folder will be staged.
     *
     * If the file is being ignored, it will not be staged unless the --force flag is used.
     */
    @Parameters(index = "0", paramLabel = "path", description = ["The path of the file/directory to be staged"])
    lateinit var path: String

    private val repositoryFolder = RepositoryFolderManager().initFolder
    private val stagingHandler = StagingHandler(force)

    /**
     * Stage the file or folder. This will add the file to the staging index.
     * If the file is a folder, it will stage all the files inside the folder.
     * @return 0 if the file was staged successfully, 1 otherwise
     */
    override fun call(): Int {
        super.call()
        val file = repositoryFolder.resolve(path)

        if (file.isDirectory()) {
            stageFolder(file)
        } else {
            stageFile(file)
        }

        return 0
    }

    fun stageAllFiles() {
        stageFolder(repositoryFolder)
    }

    /**
     * Stage all the files inside the folder
     * @param directory The folder to stage
     * @see stageFile
     */
    fun stageFolder(directory: Path) {
        Logger.log("Staging folder: $directory")

        getFilesToStage(directory).forEach { stageFile(it) }
    }

    /**
     * Stage a single file and his content
     * @param file The file to stage
     */
    fun stageFile(file: Path) {
        if(IgnoreHandler.isIgnored(file) && !force) {
            val paramFile = repositoryFolder.resolve(path)

            if(file == paramFile) {
                Logger.error("The file is being ignored")
            }

            return
        }

        Logger.log("Staging file: ${SerializablePath.of(file).relativePath(repositoryFolder)}")
        stagingHandler.stage(Content(Files.readAllBytes(file)), file)
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