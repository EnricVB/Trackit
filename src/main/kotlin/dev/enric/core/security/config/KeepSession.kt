package dev.enric.core.security.config

import dev.enric.core.security.AuthUtil
import dev.enric.core.security.PasswordHash
import dev.enric.util.common.EnvironmentVariables
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class KeepSession(val username: String, val password: String?, val salt: ByteArray?) {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    fun localSave() {
        val properties = Properties().apply {
            if (configFile.exists()) {
                runCatching { load(configFile.inputStream()) }
            }
            setProperty("TRACKIT_AUTH", AuthUtil.generateToken(username, PasswordHash.hash(password ?: "", salt)))
        }

        runCatching {
            configFile.outputStream().use { properties.store(it, "Trackit configuration") }
        }.onFailure { it.printStackTrace() }
    }

    fun globalSave() {
        EnvironmentVariables.setEnv("TRACKIT_AUTH", AuthUtil.generateToken(username, PasswordHash.hash(password ?: "", salt)))
    }
}