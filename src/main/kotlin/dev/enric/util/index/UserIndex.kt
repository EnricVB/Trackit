package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.USER
import dev.enric.domain.User
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object UserIndex {
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

    fun userAlreadyExists(username: String): Boolean {
        return getAllUsers().any { User.newInstance(it).name == username }
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