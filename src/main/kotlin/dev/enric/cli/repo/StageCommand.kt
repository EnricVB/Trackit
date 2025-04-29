package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.domain.objects.Content
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.common.FileStatus
import dev.enric.util.common.FileStatus.*
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*


@Command(
    name = "stage",
    description = ["Stage files to be committed"],
    mixinStandardHelpOptions = true,
    usageHelpWidth = 500,
    footer = [
        "",
        "Example:",
        "  trackit stage build/",
        "    Stages all files inside the 'build' directory, making them ready to be committed.",
        "  trackit stage build/main.kt",
        "    Stages the 'main.kt' file inside the 'build' directory, making it ready to be committed.",
        "",
        "Notes:",
        "  - This command stages files for the next commit. Files can be added individually or entire directories can be staged.",
        "  - If a file is ignored, it will not be staged unless the '--force' flag is used.",
        "  - Directories are processed recursively. All files inside a directory will be staged.",
        "",
    ]
)
class StageCommand : TrackitCommand() {
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
    var stagePath: Path = Path.of("")

    private val repositoryFolder = RepositoryFolderManager().getInitFolderPath()
    private val stagingHandler = StagingHandler(force)
    private val stagedFilesCache = ConcurrentHashMap<Path, String>()

    /**
     * Stage the file or folder. This will add the file to the staging index.
     * If the file is a folder, it will stage all the files inside the folder.
     * @return 0 if the file was staged successfully, 1 otherwise
     */
    override fun call(): Int {
        super.call()

        if (stagePath.isDirectory()) {
            stageFolder(stagePath)
        } else {
            stageFile(stagePath)
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
     * @param path The file to stage
     */
    fun stageFile(path: Path) {
        if (!path.exists()) {
            throw IllegalStateException("The file does not exist: $path")
        }

        when (val status = FileStatus.getStatus(path.toFile())) {
            MODIFIED, UNTRACKED, IGNORED -> {
                if (!force && status == IGNORED) {
                    if(path == stagePath) Logger.error("The file is being ignored: $path")
                    return
                }

                val content = Content(Files.readAllBytes(path))
                val relativePath = SerializablePath.of(path).relativePath(repositoryFolder)

                // Stage the content, cache the result, and log
                stagingHandler.stage(content, path)
                stagedFilesCache[path] = content.generateKey().toString()
                Logger.info("Staging file: $relativePath")
            }

            else -> {}
        }
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