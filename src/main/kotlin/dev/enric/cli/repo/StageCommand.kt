package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.IgnoreHandler
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.domain.objects.Content
import dev.enric.logger.Logger
import dev.enric.util.common.FileStatus
import dev.enric.util.common.FileStatus.*
import dev.enric.util.repository.RepositoryFolderManager
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

        val filesToStage = if (stagePath.isDirectory()) {
            getFilesToStage(stagePath)
        } else {
            listOf(stagePath)
        }

        if (filesToStage.isEmpty()) {
            Logger.warning("\nNo files to stage.")
            return 0
        }

        Logger.info("Files to Stage: [${filesToStage.size}]")

        var stagedCount = 0
        val totalCount = filesToStage.size

        filesToStage.forEachIndexed { _, file ->
            val content = Content(Files.readAllBytes(file))
            stagingHandler.stage(content, file)
            stagedFilesCache[file] = content.generateKey().toString()
            stagedCount++

            val percent = (stagedCount.toFloat() / totalCount.toFloat()) * 100
            Logger.updateLine("Staging files... [$stagedCount / $totalCount] ($percent%)")
        }

        println()
        return 0
    }

    /**
     * Check if the file should be staged.
     *
     * A file should be staged if:
     * - The file exists
     * - The file is modified or untracked
     * - The file is ignored, but the force flag is set
     *
     * @param path The path of the file to check
     * @return True if the file should be staged, false otherwise
     */
    private fun shouldStage(path: Path): Boolean {
        if (!path.exists()) {
            Logger.error("The file does not exist: $path")
            return false
        }

        return when (FileStatus.getStatus(path.toFile())) {
            MODIFIED, UNTRACKED -> true
            IGNORED -> if (force || path != stagePath) true else run {
                if (path == stagePath) {
                    Logger.error("The file is being ignored: $path")
                }
                false
            }

            else -> false
        }
    }

    /**
     * Get all the files inside a folder to stage
     * @param directory The folder to get the files from
     * @return A list of all the files inside the folder
     */
    @OptIn(ExperimentalPathApi::class)
    fun getFilesToStage(directory: Path): List<Path> {
        val result = mutableListOf<Path>()
        var scanned = 0
        var toStage = 0

        directory
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .forEach { path ->
                Logger.updateLine("Scanning: [$scanned] files scanned, [$toStage] to be staged")
                val normalized = path.normalize().toString().replace(".\\.", ".")
                val isIgnored = IgnoreHandler().isIgnored(Path.of(normalized))
                scanned++

                if (isIgnored) return@forEach
                if (path.isDirectory() || !shouldStage(path)) return@forEach

                toStage++
                result.add(path)
            }

        Logger.updateLine("Files found to stage: ${result.size}")
        println()
        return result
    }
}