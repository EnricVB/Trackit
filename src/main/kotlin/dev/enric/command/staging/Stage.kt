package dev.enric.command.staging

import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import dev.enric.util.staging.StagingHandler
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*


@Command(
    name = "stage",
    description = ["Stage files to be committed"]
)
class Stage : Callable<Int> {
    @Option(names = ["--force"], description = ["Force the staging of files"])
    var force = false

    @Parameters(paramLabel = "path", description = ["The path of the file/directory to be staged"])
    var path: String = ""

    private val repositoryFolder = RepositoryFolderManager().initFolder
    private val stagingHandler = StagingHandler(force)

    /**
     * Stage the file or folder. This will add the file to the staging index.
     * If the file is a folder, it will stage all the files inside the folder.
     * @return 0 if the file was staged successfully, 1 otherwise
     */
    override fun call(): Int {
        val file = repositoryFolder.resolve(path)

        if(file.isDirectory()) {
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
        getFilesToStage(directory).forEach { stageFile(it) }
    }

    /**
     * Stage a single file and his content
     * @param file The file to stage
     */
    fun stageFile(file: Path) {
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