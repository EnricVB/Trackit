package dev.enric.core.security

import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.security.SecureRandom

/**
 * The SecretKey object provides utility methods for generating and retrieving the secret key used for encryption.
 * It allows for generating a new secret key and retrieving the existing secret key from the configuration file.
 */
object SecretKey {

    /**
     * Generates a new secret key for encryption.
     *
     * @return The generated secret key as a hexadecimal string.
     */
    fun generateKey(): String {
        val random = SecureRandom()
        val key = ByteArray(32)

        random.nextBytes(key)

        return key.joinToString("") { "%02x".format(it) }
    }

    /**
     * Retrieves the secret key from the configuration file.
     *
     * @return The secret key as a hexadecimal string.
     */
    fun getKey(): String {
        return Files.readString(RepositoryFolderManager().getSecretKeyPath())
    }
}