package dev.enric.core.`object`

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.TrackitObject
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import java.util.zip.Deflater

class Content(private val content: String) : TrackitObject<Content>(), Serializable {

    constructor() : this("")

    override fun decode(hash: Hash): Content {
        val contentFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = contentFolder.resolve(hash.toString())

        val decompressedStringData = decompressContent(Files.readAllBytes(objectFile)) ?: return Content() // If the file is empty, return an empty content

        return Content(String(decompressedStringData))
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

    override fun printInfo(): String {
        return "Content(content=$content)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        val newerContent = decode(newer)
        val oldestContent = decode(oldest)

        return "Newer content: ${newerContent.content}\nOldest content: ${oldestContent.content}" // TODO: Implement a better way to show differences
    }
}