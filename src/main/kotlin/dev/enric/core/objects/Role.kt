package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.ROLE
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Role(val name: String = "", val permissionLevel: Int = -1, val permissions: MutableList<Hash> = mutableListOf()) : TrackitObject<Role>(),
    Serializable {

    override fun decode(hash: Hash): Role {
        val commitFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Role() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Role
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${toString().length};$name$permissionLevel$permissions", 15)

        return ROLE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "Role(name=$name, permissionLevel=$permissionLevel, permissions=$permissions)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {

        @JvmStatic
        fun newInstance(hash: Hash): Role {
            return Role().decode(hash)
        }
    }
}
