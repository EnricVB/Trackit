package dev.enric.domain.objects.tag

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class ComplexTag(
    override val name: String = "",
    override val commit: Hash? = null,
    val user: Hash = Hash.empty32(),
    val date: Timestamp = Timestamp.from(Instant.now()),
    val message: String = ""
) : Tag, TrackitObject<ComplexTag>(), Serializable {

    override fun decode(hash: Hash): ComplexTag {
        if (!hash.string.startsWith(COMPLEX_TAG.hash.string)) throw IllegalHashException("Hash $hash is not a ComplexTag hash")

        val complexTagFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = complexTagFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("ComplexTag with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return ComplexTag() // If the file is empty, return an empty ComplexTag

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

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : ComplexTag {
            return ComplexTag().decode(hash)
        }
    }
}
