package dev.enric.command.staging

import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import dev.enric.util.staging.StagingHandler
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk


@Command(
    name = "stage",
    description = ["Stage files to be committed"]
)
class Stage : Callable<Int> {
    @Option(names = ["--force"], description = ["Force the staging of files"])
    var force = false

    @Parameters(paramLabel = "pattern", description = ["The path of the file to be staged"])
    var path: String = ""

    override fun call(): Int {
        val repository = RepositoryFolderManager().initFolder
        val stagingHandler = StagingHandler(force)
        val file = repository.resolve(path)

        if(file.isDirectory()) {
            stageFolder(file)
        } else {
            stagingHandler.stage(Content(Files.readAllBytes(file)), file)
        }

        return 0
    }

    fun stageFolder(path: Path) {
        val repository = RepositoryFolderManager().initFolder
        val stagingHandler = StagingHandler(force)

        getFilesToStage(path).forEach {
            val pathFile = repository.resolve(it)
            stagingHandler.stage(Content(Files.readAllBytes(pathFile)), pathFile)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun getFilesToStage(directory: Path): List<String> {
        return directory.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .filter { !it.isDirectory() && !it.toFile().absolutePath.toString().contains(".trackit") }
            .map { it.toString() }
            .toList()
    }
}