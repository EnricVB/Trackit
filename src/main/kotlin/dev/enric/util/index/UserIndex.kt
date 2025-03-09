package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.USER
import dev.enric.core.security.PasswordHash
import dev.enric.domain.User
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * This object is responsible for managing the users index.
 * It allows to get all the users, check if a user already exists and get a user by its name.
 */
object UserIndex {

    /**
     * Retrieves a user by its name and validates the password.
     *
     * @param username The name of the user to retrieve.
     * @param password The password to validate.
     * @return A [User] object if the username and password match, or `null` if no matching user is found.
     */
    fun getUser(username: String, password: String): User? {
        getAllUsers().forEach {
            val user = User.newInstance(it)
            val hashPassword = PasswordHash.hash(password, user.salt)

            if(user.name == username && user.password == hashPassword) {
                return user
            }
        }

        return null
    }

    /**
     * Retrieves a user by its name.
     *
     * @param username The name of the user to retrieve.
     * @return A [User] object if the user is found, or `null` if no user with the given name exists.
     */
    fun getUser(username: String): User? {
        getAllUsers().forEach {
            val user = User.newInstance(it)

            if(user.name == username) {
                return user
            }
        }

        return null
    }

    /**
     * Checks if a user with the given name already exists.
     *
     * @param username The name of the user to check.
     * @return `true` if a user with the given name exists, `false` otherwise.
     */
    fun userAlreadyExists(username: String): Boolean {
        return getAllUsers().any { User.newInstance(it).name == username }
    }

    /**
     * Retrieves all the users.
     *
     * @return A list of [Hash] objects representing the users.
     */
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