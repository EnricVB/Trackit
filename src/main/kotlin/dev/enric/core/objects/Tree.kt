package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.TREE
import dev.enric.core.TrackitObject
import dev.enric.util.SerializablePath
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Tree(val child: Map<Hash, SerializablePath> = mapOf()) : TrackitObject<Tree>(), Serializable {

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
        val hashData = Hash.parseText("${instantNow};${child.size};$child", 15)

        return TREE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        var message = ""
        child.forEach { (hash, path) -> message += "$hash -> $path\n" }

        return message
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : Tree {
            return Tree().decode(hash)
        }
    }
}