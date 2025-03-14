package dev.enric.command.repo.staging

import dev.enric.command.TrackitCommand
import dev.enric.core.repo.staging.StatusHandler
import dev.enric.util.repository.RepositoryFolderManager
import picocli.CommandLine.Command
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

@Command(
    name = "status",
    description = ["Show the status of the working directory"],
    mixinStandardHelpOptions = true,
)
class Status : TrackitCommand() {

    @OptIn(ExperimentalPathApi::class)
    override fun call(): Int {
        super.call()

        val repositoryFolderManager = RepositoryFolderManager()

        repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .forEach {
                val isTrackitFolder = it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath())
                val isSecretKey = it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath())
                val isRootFolder = it.toRealPath() == repositoryFolderManager.getInitFolderPath()

                if (isTrackitFolder || isSecretKey || isRootFolder) return@forEach

                println("File: ${it.fileName} ${StatusHandler.getStatus(it.toFile())}")
            }

        return 0
    }
}