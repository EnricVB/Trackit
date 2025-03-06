package dev.enric.core.security

import dev.enric.core.Hash
import dev.enric.domain.User
import dev.enric.util.common.EnvironmentVariables
import dev.enric.util.index.UserIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream

object AuthUtil {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()
    private val secretKeyFile: Path = repositoryManager.getSecretKeyPath()

    fun authenticate(username: String, password: String): Boolean {
        return UserIndex.getUser(username, password) != null
    }

    fun getLoggedUser(): User? {
        val token = getToken()

        UserIndex.getAllUsers().forEach {
            val user = User.newInstance(it)

            if(generateToken(user.name, user.password) == token) {
                return user
            }
        }

        return null
    }

    fun getToken(): String? {
        val properties = Properties().apply { load(configFile.inputStream()) }

        return properties.getProperty("TRACKIT_AUTH")
            ?: EnvironmentVariables.getEnv("TRACKIT_AUTH")
    }


    fun generateToken(username: String, password: Hash): String {
        val passwordHash = password.toString()
        val secretKeyContent = Files.readString(secretKeyFile) ?: ""

        return Hash.parseText("$username:$passwordHash:$secretKeyContent").toString()
    }
}