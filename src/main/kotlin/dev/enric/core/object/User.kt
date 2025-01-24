package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.USER
import dev.enric.core.TrackitObject
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class User(
    val name: String = "",
    val password: Hash = Hash("0".repeat(32)),
    val email: String = "",
    val phone: String = "",
    val role: Hash = Hash("0".repeat(32))
) : TrackitObject<User>() {

    override fun decode(hash: Hash): User {
        val treeFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return User() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as User
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$name$password$email$phone$role", 15)

        return USER.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "User(name=$name, password=$password, email=$email, phone=$phone, role=$role)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun decode(hash : Hash) : User {
            return User().decode(hash)
        }
    }
}
