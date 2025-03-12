package dev.enric.core.security

import dev.enric.domain.Hash
import dev.enric.domain.User
import dev.enric.util.common.EnvironmentVariables
import dev.enric.util.index.UserIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream

/**
 * The AuthUtil object provides utility methods for user authentication and managing authentication tokens in Trackit.
 * It allows for validating user credentials, retrieving the currently logged-in user, generating authentication tokens,
 * and accessing token storage from both configuration files and environment variables.
 */
object AuthUtil {
    private val repositoryManager = RepositoryFolderManager()
    private val configFile: Path = repositoryManager.getConfigFilePath()

    /**
     * Authenticates a user by checking if the provided username and password match the stored password hash.
     * The password is hashed using the stored salt before comparing it to the stored password hash.
     *
     * @param username The username of the user to authenticate.
     * @param password The password of the user to authenticate.
     * @return True if authentication is successful, false otherwise.
     */
    fun authenticate(username: String, password: String): Boolean {
        UserIndex.getUser(username).let {
            val salt = it?.salt ?: return false

            return PasswordHash.hash(password, salt) == it.password
        }
    }

    /**
     * Retrieves the currently logged-in user by comparing the authentication token in the configuration file
     * to the generated token for each user in the user index.
     *
     * @return The currently logged-in user, or null if no user is logged in.
     */
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

    /**
     * Retrieves the authentication token from the configuration file or environment variables.
     *
     * @return The authentication token, or null if it is not found.
     */
    fun getToken(): String? {
        val properties = Properties().apply { load(configFile.inputStream()) }

        return properties.getProperty("TRACKIT_AUTH")
            ?: EnvironmentVariables.getEnv("TRACKIT_AUTH")
    }

    /**
     * Generates an authentication token for a user by hashing the username, password, and secret key.
     *
     * @param username The username of the user to generate a token for.
     * @param password The password of the user to generate a token for.
     * @return The generated authentication token.
     */
    fun generateToken(username: String, password: String): String {
        return Hash.parseText("$username:$password:${SecretKey.getKey()}").toString()
    }
}