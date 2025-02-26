package dev.enric.core.handler.config

import dev.enric.core.Hash
import dev.enric.util.AuthUtil
import dev.enric.util.EnvironmentVariables
import dev.enric.util.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

data class KeepSession(val username: String, val password: String?) {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    fun localSave() {
        val properties = Properties().apply {
            if (configFile.exists()) {
                runCatching { load(configFile.inputStream()) }
            }
            setProperty("TRACKIT_AUTH", AuthUtil.generateToken(username, Hash.parseText(password ?: "")))
        }

        runCatching {
            configFile.outputStream().use { properties.store(it, "Trackit configuration") }
        }.onFailure { it.printStackTrace() }
    }

    fun globalSave() {
        EnvironmentVariables.setEnv("TRACKIT_AUTH", AuthUtil.generateToken(username, Hash.parseText(password ?: "")))
    }
}