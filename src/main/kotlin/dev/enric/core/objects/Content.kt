package dev.enric.core.objects

import dev.enric.core.TrackitObject
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.io.path.Path

class Content(private val content: String) : TrackitObject<Content> {

    override fun encode(): Pair<Hash, ByteArray> {
        return Pair(generateKey(), compressContent())
    }

    override fun encode(writeOnDisk: Boolean): Pair<Hash, ByteArray> {
        TODO("Not yet implemented")
    }

    override fun decode(hash: Hash): Content {
        val path = Path("") // TODO: Poner que el path se obtenga desde donde se ejecute
        val compressedData = Files.readAllBytes(path)

        return Content(decompressContent(compressedData))
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
        TODO("Not yet implemented")
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

}
