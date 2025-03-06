package dev.enric.domain

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.TREE
import dev.enric.core.TrackitObject
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path

data class Tree(
    val serializablePath: SerializablePath = SerializablePath("."),
    val hash: Hash = Hash("0".repeat(32))
) : TrackitObject<Tree>(), Serializable {

    constructor(path: Path, hash: Hash) : this(SerializablePath.of(path), hash)

    override fun decode(hash: Hash): Tree {
        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Tree() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Tree
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${serializablePath.toString()};${hash}", 15)

        return TREE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Tree Details"))

            append(ColorUtil.label("  Path: "))
            appendLine(
                if (serializablePath.toString().isNotEmpty()) ColorUtil.text(serializablePath.toString())
                else ColorUtil.message("No path assigned")
            )

            append(ColorUtil.label("  Hash: "))
            appendLine(
                if (hash.toString().isNotEmpty()) ColorUtil.text(hash.toString())
                else ColorUtil.message("No hash assigned")
            )
        }
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