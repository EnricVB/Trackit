package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.TrackitObject
import dev.enric.util.SerializablePath
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Tree(val child: Map<Hash, SerializablePath>) : TrackitObject<Tree>(), Serializable {

    constructor() : this(mapOf())

    override fun decode(hash: Hash): Tree {
        val treeFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Tree() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Tree
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashType = Hash.parseText("Tree", 1)
        val hashData = Hash.parseText("${instantNow};${child.size};$child", 15)

        return hashType.plus(hashData)
    }

    override fun printInfo(): String {
        var message = ""
        child.forEach { (hash, path) -> message += "$hash -> $path\n" }

        return message
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }
}
