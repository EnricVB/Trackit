package dev.enric.core.objects.tag

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMPLEX_TAG
import dev.enric.core.TrackitObject
import dev.enric.util.ColorUtil
import dev.enric.util.RepositoryFolderManager
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
        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return ComplexTag() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as ComplexTag
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${this.toString().length};$name$date$user", 15)

        return COMPLEX_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Complex Tag Details"))

            append(ColorUtil.label("  Name: "))
            appendLine(if (name.isNotEmpty()) ColorUtil.text(name) else ColorUtil.message("No name assigned"))

            append(ColorUtil.label("  Commit: "))
            appendLine(commit?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No commit assigned"))

            append(ColorUtil.label("  User: "))
            appendLine(ColorUtil.text(user.toString()))

            append(ColorUtil.label("  Date: "))
            appendLine(ColorUtil.text(date.toString()))

            append(ColorUtil.label("  Message: "))
            appendLine(if (message.isNotEmpty()) ColorUtil.text(message) else ColorUtil.message("No message assigned"))
        }
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
