package dev.enric.core.handler.config

import dev.enric.core.Hash
import dev.enric.util.EnvironmentVariables
import dev.enric.util.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

data class KeepSession(val username: String, val password: String?) {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()
    private val secretKeyFile: Path = repositoryManager.getSecretKeyPath()

    fun localSave() {
        val properties = Properties().apply {
            if (configFile.exists()) {
                runCatching { load(configFile.inputStream()) }
            }
            setProperty("[auth]", generateToken())
        }

        runCatching {
            configFile.outputStream().use { properties.store(it, "Trackit configuration") }
        }.onFailure { it.printStackTrace() }
    }

    fun globalSave() {
        EnvironmentVariables.setEnv("[auth]", generateToken())
    }

    fun getToken(): String? {
        val properties = Properties().apply { load(configFile.inputStream()) }

        return properties.getProperty("[auth]")
            ?: EnvironmentVariables.getEnv("[auth]")
    }

    fun generateToken(): String {
        val passwordHash = Hash.parseText(password ?: "").toString()
        val secretKeyContent = Files.readString(secretKeyFile) ?: ""

        return Hash.parseText("$username:$passwordHash:$secretKeyContent").toString()
    }
}