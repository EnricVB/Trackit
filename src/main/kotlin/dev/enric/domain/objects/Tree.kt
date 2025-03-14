package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.TREE
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path

data class Tree(
    val serializablePath: SerializablePath = SerializablePath("."),
    val content: Hash = Hash("0".repeat(32))
) : TrackitObject<Tree>(), Serializable {

    constructor(path: Path, hash: Hash) : this(SerializablePath.of(path), hash)

    override fun decode(hash: Hash): Tree {
        if (!hash.string.startsWith(TREE.hash.string)) throw IllegalHashException("Hash $hash is not a Tree hash")

        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Tree with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Tree() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Tree
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("$serializablePath;${content}", 15)

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

            append(ColorUtil.label("  Content: "))
            appendLine(
                if (content.toString().isNotEmpty()) ColorUtil.text(content.toString())
                else ColorUtil.message("No content assigned")
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