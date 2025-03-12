package dev.enric.domain

import dev.enric.domain.Hash.HashType.COMMIT
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    var previousCommit: Hash? = null,
    var tree: List<Hash> = listOf(),
    var branch: Hash = Hash("0".repeat(32)),
    var author: Hash = Hash("0".repeat(32)),
    var confirmer: Hash = Hash("0".repeat(32)),
    var date: Timestamp = Timestamp.from(Instant.now()),
    var title: String = "",
    var message: String = "",
    var tag: String = ""
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

            if (tree.serializablePath.pathString == path.toString() && tree.hash == content.generateKey()) {
                return tree
            }
        }

        return null
    }

    override fun printInfo(): String {
        val author = User.newInstance(author)
        val confirmer = User.newInstance(confirmer)

        return buildString {
            appendLine(ColorUtil.title("commit ${generateKey()}"))

            append("Author: \t${author.name}")
            if (author.mail.isNotBlank()) append(" <${author.mail}>")
            if (author.phone.isNotBlank()) append(" <${author.phone}>")
            appendLine(" : ${author.encode().first}")

            append("Confirmer: \t${confirmer.name}")
            if (confirmer.mail.isNotBlank()) append(" <${confirmer.mail}>")
            if (confirmer.phone.isNotBlank()) append(" <${confirmer.phone}>")
            appendLine(" : ${confirmer.encode().first}")

            appendLine("Date: $date")

            appendLine("\n\t$title")
            appendLine("\n\t$message")
        }
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : Commit {
            return Commit().decode(hash)
        }
    }
}