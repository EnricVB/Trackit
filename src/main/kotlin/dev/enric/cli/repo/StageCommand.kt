package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.logger.Logger
import picocli.CommandLine.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi

@OptIn(ExperimentalPathApi::class)
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

    private val stagingHandler = StagingHandler(force)
    private val stagedFilesCache = ConcurrentHashMap<Path, String>()

    /**
     * Stage the file or folder. This will add the file to the staging index.
     * If the file is a folder, it will stage all the files inside the folder.
     * @return 0 if the file was staged successfully, 1 otherwise
     */
    override fun call(): Int {
        super.call()

        val stagedFiles = stagingHandler.stagePath(stagePath)

        if (stagedFiles.isEmpty()) {
            Logger.warning("\nNo files to stage.")
            return 0
        }

        Logger.info("Files to Stage: [${stagedFiles.size}]")

        stagedFiles.forEachIndexed { index, (file, key) ->
            stagedFilesCache[file] = key
            val percent = ((index + 1).toFloat() / stagedFiles.size) * 100
            Logger.updateLine("Staging files... [${index + 1} / ${stagedFiles.size}] ($percent%)")
        }

        println()
        return 0
    }
}