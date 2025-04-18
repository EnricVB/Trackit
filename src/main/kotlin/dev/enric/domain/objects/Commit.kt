package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.index.TagIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    var previousCommit: Hash? = null,
    var tree: MutableList<Hash> = mutableListOf(),
    var branch: Hash = Hash.empty32(),
    var author: Hash = Hash.empty32(),
    var confirmer: Hash = Hash.empty32(),
    var date: Timestamp = Timestamp.from(Instant.now()),
    var title: String = "",
    var message: String = ""
) : TrackitObject<Commit>(), Serializable {

    override fun decode(hash: Hash): Commit {
        if (!hash.string.startsWith(COMMIT.hash.string)) throw IllegalHashException("Hash $hash is not a Commit hash")

        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Commit with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Commit() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Commit
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${this.toString().length};$this", 15)

        return COMMIT.hash.plus(hashData)
    }

    fun findFile(content: Content, path: Path): Tree? {
        tree.forEach {
            val tree = Tree.newInstance(it)

            if (tree.serializablePath.pathString == path.toString() && tree.content == content.generateKey()) {
                return tree
            }
        }

        return null
    }

    override fun printInfo(): String {
        val author = User.newInstance(author)
        val confirmer = User.newInstance(confirmer)

        return buildString {
            append(ColorUtil.label("Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append("Author: ${author.name}")
            if (author.mail.isNotBlank()) append(" <${author.mail}>")
            if (author.phone.isNotBlank()) append(" <${author.phone}>")
            appendLine(" <${author.generateKey()}>")

            if (confirmer != author) {
                append("Confirmer: ${confirmer.name}")
                if (confirmer.mail.isNotBlank()) append(" <${confirmer.mail}>")
                if (confirmer.phone.isNotBlank()) append(" <${confirmer.phone}>")
                appendLine(" <${confirmer.generateKey()}>")
            }

            TagIndex.getTagsByCommit(generateKey()).forEach {
                val isComplexTag = it.string.startsWith(COMPLEX_TAG.hash.string)
                val isSimpleTag = it.string.startsWith(SIMPLE_TAG.hash.string)

                if (isComplexTag) {
                    appendLine("Tag: ${ComplexTag.newInstance(it).name}")
                } else if (isSimpleTag) {
                    appendLine("Tag: ${SimpleTag.newInstance(it).name}")
                }
            }

            appendLine("Date: $date")

            appendLine("\t$title")
            appendLine("\t$message")
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash : Hash) : Commit {
            return Commit().decode(hash)
        }
    }
}