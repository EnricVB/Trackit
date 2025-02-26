package dev.enric.util

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.USER
import dev.enric.core.objects.User
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

object AuthUtil {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()
    private val secretKeyFile: Path = repositoryManager.getSecretKeyPath()

    fun authenticate(username: String, password: String): Boolean {
        return getUser(username, password) != null
    }

    fun getUser(username: String, password: String): User? {
        val hashPassword = Hash.parseText(password)

        getAllUsers().forEach {
            val user = User.newInstance(it)

            if(user.name == username && user.password == hashPassword) {
                return user
            }
        }

        return null
    }

    fun getLoggedUser(): User? {
        val token = getToken()

        getAllUsers().forEach {
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

    @OptIn(ExperimentalPathApi::class)
    fun getAllUsers(): List<Hash> {
        val usersFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(USER.hash.toString())

        return usersFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + USER.hash + "\\"))
        }.toList()
    }
}