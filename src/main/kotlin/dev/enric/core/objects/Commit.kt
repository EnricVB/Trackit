package dev.enric.core.objects

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMMIT
import dev.enric.core.TrackitObject
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    val previousCommit: Hash = Hash("0".repeat(32)),
    val tree: List<Hash> = listOf(),
    val branch: Hash = Hash("0".repeat(32)),
    val autor: Hash = Hash("0".repeat(32)),
    val confirmer: Hash = Hash("0".repeat(32)),
    val date: Timestamp = Timestamp.from(Instant.now()),
    val title: String = "",
    val message: String = "",
    val tag: String = ""
) : TrackitObject<Commit>(), Serializable {

    override fun decode(hash: Hash): Commit {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
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
        return "Commit(previousCommit=$previousCommit, tree=$tree, branch=$branch, autor=$autor, confirmer=$confirmer, date=$date, title='$title', message='$message', tag=$tag)"
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