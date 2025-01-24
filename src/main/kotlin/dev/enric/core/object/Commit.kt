package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Commit(
    val previousCommit: Hash,
    val tree: Hash,
    val branch: Hash,
    val autor: Hash,
    val confirmer: Hash,
    val date: Timestamp,
    val title: String,
    val message: String,
    val tag: String
) : TrackitObject<Commit>(), Serializable {

    constructor(
        previousCommit: Hash?,
        tree: Hash,
        branch: Hash,
        autor: Hash,
        title: String,
        message: String
    ) : this(
        previousCommit ?: Hash("0".repeat(32)),
        tree,
        branch,
        autor,
        autor,
        Timestamp.from(Instant.now()),
        title,
        message,
        ""
    )

    constructor() : this(
        previousCommit = Hash("0".repeat(32)),
        tree = Hash("0".repeat(32)),
        branch = Hash("0".repeat(32)),
        autor = Hash("0".repeat(32)),
        confirmer = Hash("0".repeat(32)),
        date = Timestamp.from(Instant.now()),
        title = "",
        message = "",
        tag = ""
    )

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
        val hashType = Hash.parseText("Commit", 1)
        val hashData = Hash.parseText("${instantNow};${this.toString().length};$this", 15)

        return hashType.plus(hashData)
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