package dev.enric.core.objects.tag

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMPLEX_TAG
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class ComplexTag(
    override val name: String = "",
    override val commit: Hash? = null,
    val user: Hash = Hash("0".repeat(32)),
    val date: Timestamp = Timestamp.from(Instant.now()),
    val message: String = ""
) : Tag, TrackitObject<ComplexTag>(), Serializable {

    override fun decode(hash: Hash): ComplexTag {
        val treeFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return ComplexTag() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as ComplexTag
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$name", 15)

        return COMPLEX_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "SimpleTag(name=$name, hash=$commit)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : ComplexTag {
            return ComplexTag().decode(hash)
        }
    }
}
