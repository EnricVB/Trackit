package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.COMMIT
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    val previousCommit: Hash = Hash("0".repeat(32)),
    val tree: Hash = Hash("0".repeat(32)),
    val branch: Hash = Hash("0".repeat(32)),
    val autor: Hash = Hash("0".repeat(32)),
    val confirmer: Hash = Hash("0".repeat(32)),
    val date: Timestamp = Timestamp.from(Instant.now()),
    val title: String = "",
    val message: String = "",
    val tag: String = ""
) : TrackitObject<Commit>(), Serializable {

    override fun decode(hash: Hash): Commit {
        val commitFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Commit() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Commit
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$this", 15)

        return COMMIT.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "Commit(previousCommit=$previousCommit, tree=$tree, branch=$branch, autor=$autor, confirmer=$confirmer, date=$date, title='$title', message='$message', tag=$tag)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun decode(hash : Hash) : Commit {
            return Commit().decode(hash)
        }
    }
}