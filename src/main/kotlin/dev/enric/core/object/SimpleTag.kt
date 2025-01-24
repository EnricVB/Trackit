package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMPLEX_TAG
import dev.enric.core.Hash.HashType.SIMPLE_TAG
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class SimpleTag(
    override val name: String = "",
    override val commit: Hash? = null
) : Tag, TrackitObject<SimpleTag>(), Serializable {

    override fun decode(hash: Hash): SimpleTag {
        val treeFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return SimpleTag() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as SimpleTag
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$name", 15)

        return SIMPLE_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "SimpleTag(name=$name, hash=$commit)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun decode(hash : Hash) : SimpleTag {
            return SimpleTag().decode(hash)
        }
    }
}
