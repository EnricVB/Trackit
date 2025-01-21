package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class ComplexTag(
    override val name: String,
    override val commit: Hash?,
    val user: Hash,
    val date: Timestamp,
    val message: String
) : Tag, TrackitObject<ComplexTag>(), Serializable {

    constructor() : this("", null, Hash("0".repeat(32)), Timestamp.from(Instant.now()), "")

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
        val hashType = Hash.parseText("SimpleTag", 1)
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$name", 15)

        return hashType.plus(hashData)
    }

    override fun printInfo(): String {
        return "SimpleTag(name=$name, hash=$commit)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

}
