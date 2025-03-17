package dev.enric.core.handler.config

import dev.enric.core.security.AuthUtil
import dev.enric.core.security.PasswordHash
import dev.enric.util.common.EnvironmentVariables
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


/**
 * The KeepSession class manages the storage of authentication details (username, password, and salt)
 * for Trackit. It supports saving authentication data both locally in a configuration file and globally
 * via environment variables.
 *
 * @property username The username of the user to authenticate.
 * @property password The password of the user. Can be null.
 * @property salt The salt used for password hashing. Can be null.
 */
class KeepSession(val username: String, val password: String?, val salt: ByteArray?) {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    /**
     * Saves the authentication details locally in a configuration file.
     * The method generates an authentication token by hashing the password (or an empty string if password is null)
     * using the provided salt. The generated token is stored in the configuration file under the key "TRACKIT_AUTH".
     *
     * If the configuration file already exists, its properties are loaded and updated with the new token.
     * The updated properties are then saved back to the configuration file.
     */
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

    /**
     * Saves the authentication details globally by setting an environment variable.
     * The method generates an authentication token by hashing the password (or an empty string if password is null)
     * using the provided salt, and sets it as the value of the "TRACKIT_AUTH" environment variable.
     */
    fun globalSave() {
        EnvironmentVariables.setEnv("TRACKIT_AUTH", AuthUtil.generateToken(username, PasswordHash.hash(password ?: "", salt)))
    }
}