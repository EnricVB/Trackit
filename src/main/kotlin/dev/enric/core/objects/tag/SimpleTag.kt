package dev.enric.core.objects.tag

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.SIMPLE_TAG
import dev.enric.core.TrackitObject
import dev.enric.util.ColorUtil
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class SimpleTag(
    override val name: String = "",
    override val commit: Hash? = null
) : Tag, TrackitObject<SimpleTag>(), Serializable {

    override fun decode(hash: Hash): SimpleTag {
        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return SimpleTag() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as SimpleTag
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${this.toString().length};$name", 15)

        return SIMPLE_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Simple Tag Details"))

            append(ColorUtil.label("  Name: "))
            appendLine(if (name.isNotEmpty()) ColorUtil.text(name) else ColorUtil.message("No name assigned"))

            append(ColorUtil.label("  Commit: "))
            appendLine(commit?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No commit assigned"))
        }
    }


    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : SimpleTag {
            return SimpleTag().decode(hash)
        }
    }
}
