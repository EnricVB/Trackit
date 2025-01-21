package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class SimpleTag(
    override val name: String,
    override val commit: Hash?
) : Tag, TrackitObject<SimpleTag>(), Serializable {

    constructor() : this("", null)

    constructor(name: String) : this(name, null)

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
