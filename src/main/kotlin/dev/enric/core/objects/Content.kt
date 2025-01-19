package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.TrackitObject
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.Inflater

class Content(private val content: String) : TrackitObject<Content> {

    override fun encode(): Pair<Hash, ByteArray> {
        return Pair(generateKey(), compressContent())
    }

    override fun encode(writeOnDisk: Boolean): Pair<Hash, ByteArray> {
        val encodedFile = encode()

        if(writeOnDisk) {
            val contentFolder = Main.repository.getObjectsFolderPath().resolve(encodedFile.first.toString().take(2))
            val objectFile = contentFolder.resolve(encodedFile.first.toString())
            contentFolder.toFile().mkdir()

            Files.write(objectFile, compressContent())
        }

        return encodedFile
    }

    override fun decode(hash: Hash): Content {
        val contentFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = contentFolder.resolve(hash.toString())

        return Content(decompressContent(Files.readAllBytes(objectFile)))
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashType = Hash.parseText("Content", 1)
        val hashData = Hash.parseText("${instantNow};${content.length};$content", 15)

        return hashType.plus(hashData)
    }

    override fun compressContent(): ByteArray {
        val deflater = Deflater()
        deflater.setInput(content.toByteArray())
        deflater.finish() // Indicates that all data has been sent

        val outputStream = ByteArrayOutputStream(content.toByteArray().size) // Stream to which is going to be sent the compressed data
        val buffer = ByteArray(1024)

        while (!deflater.finished()) {
            val length = deflater.deflate(buffer) // Deflate does compress the data to the buffer
            outputStream.write(buffer, 0, length) // Writes the buffered data to the outputStream

            // We need a buffer since outputStream can't be treated as a ByteArray, which is needed to send the data
            // A ByteArray is needed, and we can't send it all directly to a single ByteArray as we don't know the final length needed
        }

        return outputStream.toByteArray()
    }

    override fun decompressContent(compressedData : ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(compressedData)
        // This does not need all the data to be decompressed before starting

        val outputStream = ByteArrayOutputStream(compressedData.size) // Stream to which is going to be sent the decompressed data
        val buffer = ByteArray(1024)

        while (!inflater.finished()) {
            val length = inflater.inflate(buffer) // Decompress the data into the buffer
            outputStream.write(buffer, 0, length) // Writes the decompressed data into the outputStream
        }

        return String(outputStream.toByteArray())
    }

    override fun printInfo(): String {
        return "Content: $content"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        val newerContent = decode(newer)
        val oldestContent = decode(oldest)

        return "Newer content: ${newerContent.content}\nOldest content: ${oldestContent.content}" // TODO: Implement a better way to show differences
    }
}
