package dev.enric.core.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.util.*


object PasswordHash {

    @Throws(NoSuchAlgorithmException::class)
    fun hash(password: String, salt: ByteArray?): String {
        Security.addProvider(BouncyCastleProvider())
        val generator = Argon2BytesGenerator()
        val builder = Argon2Parameters.Builder()

        val pepper = ByteArray(16)
        SecureRandom.getInstanceStrong().nextBytes(salt)
        SecureRandom.getInstanceStrong().nextBytes(pepper)

        val hash = ByteArray(128)

        builder.withIterations(3)
        builder.withMemoryAsKB(2048000)
        builder.withParallelism(4)
        builder.withSalt(salt)
        builder.withSecret(pepper)
        builder.withVersion(1)

        val parameters = builder.build()

        generator.init(parameters)
        generator.generateBytes(password.toCharArray(), hash)
        return Base64.getEncoder().encodeToString(hash)
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16) // 128 bits
        SecureRandom().nextBytes(salt)
        return salt
    }
}