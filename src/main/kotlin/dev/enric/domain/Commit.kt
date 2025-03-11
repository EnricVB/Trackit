package dev.enric.domain

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMMIT
import dev.enric.core.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    var previousCommit: Hash = Hash("0".repeat(32)),
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
        return buildString {
            appendLine(ColorUtil.title("Commit Details"))

            append(ColorUtil.label("  Previous Commit: "))
            appendLine(ColorUtil.text(previousCommit.toString()))

            append(ColorUtil.label("  Tree Hashes: "))
            if (tree.isNotEmpty()) {
                tree.forEach { appendLine("    - " + ColorUtil.text(it.toString())) }
            } else {
                appendLine(ColorUtil.message("No tree objects assigned"))
            }

            append(ColorUtil.label("  Branch: "))
            appendLine(ColorUtil.text(branch.toString()))

            append(ColorUtil.label("  Author: "))
            appendLine(ColorUtil.text(author.toString()))

            append(ColorUtil.label("  Confirmer: "))
            appendLine(ColorUtil.text(confirmer.toString()))

            append(ColorUtil.label("  Date: "))
            appendLine(ColorUtil.text(date.toString()))

            append(ColorUtil.label("  Title: "))
            appendLine(if (title.isNotEmpty()) ColorUtil.text(title) else ColorUtil.message("No title assigned"))

            append(ColorUtil.label("  Message: "))
            appendLine(if (message.isNotEmpty()) ColorUtil.text(message) else ColorUtil.message("No message assigned"))

            append(ColorUtil.label("  Tag: "))
            appendLine(if (tag.isNotEmpty()) ColorUtil.text(tag) else ColorUtil.message("No tag assigned"))
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