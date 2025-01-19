package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.TrackitObject
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.Inflater

data class Tree(val child: Map<Hash, Path>) : TrackitObject<Tree> {
    override fun encode(): Pair<Hash, ByteArray> {
        return Pair(generateKey(), compressContent())
    }

    override fun encode(writeOnDisk: Boolean): Pair<Hash, ByteArray> {
        val encodedFile = encode()

        if (writeOnDisk) {
            val contentFolder = Main.repository.getObjectsFolderPath().resolve(encodedFile.first.toString().take(2))
            val objectFile = contentFolder.resolve(encodedFile.first.toString())
            contentFolder.toFile().mkdir()

            Files.write(objectFile, compressContent())
        }

        return encodedFile
    }

    override fun decode(hash: Hash): Tree {
        val contentFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = contentFolder.resolve(hash.toString())

        val decompressedStringData = decompressContent(Files.readAllBytes(objectFile)).replace(Regex("[{}\\s]"), "")
        val children: MutableMap<Hash, Path> = mutableMapOf()

        decompressedStringData.split(",").forEach {
            val childHash = Hash(it.split("=").first())
            val childPath = Path.of(it.split("=").last())

            children[childHash] = childPath
        }

        return Tree(children)
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashType = Hash.parseText("Tree", 1)
        val hashData = Hash.parseText("${instantNow};${child.size};$child", 15)

        return hashType.plus(hashData)
    }

    override fun compressContent(): ByteArray {
        val deflater = Deflater()
        deflater.setInput(child.toString().toByteArray())
        deflater.finish() // Indicates that all data has been sent

        val outputStream = ByteArrayOutputStream(
            child.toString().toByteArray().size
        ) // Stream to which is going to be sent the compressed data
        val buffer = ByteArray(2048)

        while (!deflater.finished()) {
            val length = deflater.deflate(buffer) // Deflate does compress the data to the buffer
            outputStream.write(buffer, 0, length) // Writes the buffered data to the outputStream

            // We need a buffer since outputStream can't be treated as a ByteArray, which is needed to send the data
            // A ByteArray is needed, and we can't send it all directly to a single ByteArray as we don't know the final length needed
        }

        return outputStream.toByteArray()
    }

    override fun decompressContent(compressedData: ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(compressedData)

        val outputStream = ByteArrayOutputStream(compressedData.size)
        val buffer = ByteArray(1024)

        while (!inflater.finished()) {
            val length = inflater.inflate(buffer)
            outputStream.write(buffer, 0, length)
        }

        return outputStream.toString()
    }

    override fun printInfo(): String {
        var message = ""
        child.forEach { (hash, path) -> message += "$hash -> $path\n" }

        return message
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }
}
