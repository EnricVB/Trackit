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
            getFilesToStage(repository.resolve(path)).forEach {
                val path = repository.resolve(it)
                stagingHandler.stage(Content(Files.readString(path)), path)
            }
        } else {
            stagingHandler.stage(Content(Files.readString(file)), file)
        }

        StagingHandler.getStagedFiles().forEach {
            (hash, path) -> println("$hash : $path")
        }

        return 0
    }

    @OptIn(ExperimentalPathApi::class)
    fun getFilesToStage(directory: Path): List<String> {
        return directory.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .filter { !it.isDirectory() }
            .map { it.toString() }
            .toList()
    }
}