package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH
import dev.enric.core.TrackitObject
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Branch(
    val name : String = ""
) : TrackitObject<Branch>() {

    override fun decode(hash: Hash): Branch {
        val commitFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Branch() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Branch
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${name.length};$name", 15)

        return BRANCH.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "Branch(name=$name)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun decode(hash : Hash) : Branch {
            return Branch().decode(hash)
        }
    }
}