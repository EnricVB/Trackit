package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Handles the configuration of remote directives for the Trackit application.
 * This includes saving and loading directives related to remote repository operations.
 *
 * The configuration is stored in a properties file located in the repository folder.
 */
class RemoteDirectivesConfig() : CommandHandler() {

    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    /**
     * Saves the auto-push directive to the configuration file.
     * This directive determines whether to enable or disable auto-push for the remote repository.
     *
     * @param authopush Boolean value indicating whether to enable or disable auto-push.
     */
    fun saveAutoPushDirective(authopush: Boolean) {
        val properties = Properties().apply {
            if (configFile.exists()) {
                runCatching { load(configFile.inputStream()) }
            }

            setProperty("TRACKIT_REMOTE_AUTOPUSH", authopush.toString())
        }

        runCatching {
            configFile.outputStream().use {
                properties.store(
                    it,
                    "TRACKIT_REMOTE_AUTOPUSH -> " +
                            "If true, missing data on remote, as Users, Roles and Permissions will be sent on Push. " +
                            "Otherwise, will throw an error."
                )
            }
        }.onFailure { it.printStackTrace() }
    }

    /**
     * Loads the auto-push directive from the configuration file.
     */
    fun loadAutopushDirective(): Boolean {
        if (!configFile.exists()) return false

        val properties = Properties().apply {
            runCatching { load(configFile.inputStream()) }
        }

        return properties.getProperty("TRACKIT_REMOTE_AUTOPUSH").toBoolean()
    }
}