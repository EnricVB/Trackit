package dev.enric.domain.objects.tag

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class SimpleTag(
    override val name: String = "",
    override val commit: Hash? = null
) : Tag, TrackitObject<SimpleTag>(), Serializable {

    override fun decode(hash: Hash): SimpleTag {
        if (!hash.string.startsWith(SIMPLE_TAG.hash.string)) throw IllegalHashException("Hash $hash is not a SimpleTag hash")

        val simpleTagFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = simpleTagFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("SimpleTag with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return SimpleTag() // If the file is empty, return an empty SimpleTag

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
