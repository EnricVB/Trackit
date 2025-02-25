package dev.enric.util

import dev.enric.core.Hash
import dev.enric.core.objects.User
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object AuthUtil {

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

    @OptIn(ExperimentalPathApi::class)
    fun getAllUsers(): List<Hash> {
        val usersFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(Hash.HashType.USER.hash.toString())

        return usersFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\7b\\"))
        }.toList()
    }
}