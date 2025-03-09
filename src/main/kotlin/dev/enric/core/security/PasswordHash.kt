package dev.enric.core.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.util.*

object PasswordHash {
    const val ITERATIONS = 2    // Lower values increase the speed of the hash          // TODO: Allow this to be configurable
    const val MEMORY = 512000   // Lower values increase the speed of the hash          // TODO: Allow this to be configurable
    const val PARALLELISM = 8   // Higher values increase the speed of the hash         // TODO: Allow this to be configurable
    const val SALT_LENGTH = 16  // Bytes of the salt

    @Throws(NoSuchAlgorithmException::class)
    fun hash(password: String, salt: ByteArray?): String {
        Security.addProvider(BouncyCastleProvider())
        val generator = Argon2BytesGenerator()
        val builder = Argon2Parameters.Builder()

        val hash = ByteArray(128)

        builder.withIterations(ITERATIONS)
        builder.withMemoryAsKB(MEMORY)
        builder.withParallelism(PARALLELISM)
        builder.withSalt(salt)

        val parameters = builder.build()

        generator.init(parameters)
        generator.generateBytes(password.toCharArray(), hash)
        return Base64.getEncoder().encodeToString(hash)
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        return salt
    }
}