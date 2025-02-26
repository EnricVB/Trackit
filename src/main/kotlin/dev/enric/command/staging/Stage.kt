package dev.enric.command.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.staging.StagingHandler
import dev.enric.core.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.RepositoryFolderManager
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk


@Command(
    name = "stage",
    description = ["Stage files to be committed"]
)
class Stage : TrackitCommand() {
    @Option(names = ["--force"], description = ["Force the staging of files"])
    var force = false // TODO: Implement force option

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
        Logger.log("Staging file: $file")

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