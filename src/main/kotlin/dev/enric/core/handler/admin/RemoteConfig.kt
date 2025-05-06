package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * RemoteConfig handles storing and updating remote push and fetch URLs
 * in the local Trackit configuration file.
 *
 * This allows commands to retrieve push/fetch settings when performing remote operations.
 *
 * @property remotePush The push remote URL or path.
 * @property remoteFetch The fetch remote URL or path.
 */
class RemoteConfig(
    private val remotePush: String? = null,
    private val remoteFetch: String? = null
) : CommandHandler() {

    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    /**
     * Saves the remote push and/or fetch URLs into the config.cfg file.
     * If the file exists, it loads the current properties and updates them.
     */
    fun save() {
        val properties = Properties().apply {
            if (configFile.exists()) {
                runCatching { load(configFile.inputStream()) }
            }

            remotePush?.let { setProperty("TRACKIT_REMOTE_PUSH", it) }
            remoteFetch?.let { setProperty("TRACKIT_REMOTE_FETCH", it) }
        }

        runCatching {
            configFile.outputStream().use { properties.store(it, "Trackit configuration") }
        }.onFailure { it.printStackTrace() }
    }

    /**
     * Loads the remote configuration from the config.cfg file, if available.
     *
     * @return A pair of (push, fetch) URLs, or nulls if not found.
     */
    fun load(): Pair<String?, String?> {
        if (!configFile.exists()) return Pair(null, null)

        val properties = Properties().apply {
            runCatching { load(configFile.inputStream()) }
        }

        val push = properties.getProperty("TRACKIT_REMOTE_PUSH")
        val fetch = properties.getProperty("TRACKIT_REMOTE_FETCH")

        return Pair(push, fetch)
    }
}