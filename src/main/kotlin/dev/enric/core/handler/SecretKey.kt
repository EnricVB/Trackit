package dev.enric.core.handler

import dev.enric.util.RepositoryFolderManager
import java.nio.file.Files
import java.security.SecureRandom

object SecretKey {

    fun generateKey(): String {
        val random = SecureRandom()
        val key = ByteArray(32)

        random.nextBytes(key)

        return key.joinToString("") { "%02x".format(it) }
    }

    fun getKey(): String {
        return Files.readString(RepositoryFolderManager().getSecretKeyPath())
    }
}