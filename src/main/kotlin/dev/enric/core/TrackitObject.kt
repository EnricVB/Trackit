package dev.enric.core

import dev.enric.util.RepositoryFolderManager
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

abstract class TrackitObject<T : TrackitObject<T>> {

    open fun encode(): Pair<Hash, ByteArray> {
        return Pair(generateKey(), compressContent())
    }

    open fun encode(writeOnDisk: Boolean = false): Pair<Hash, ByteArray> {
        val encodedFile = encode()

        if (writeOnDisk) {
            val objectFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(encodedFile.first.toString().take(2))
            val objectFile = objectFolder.resolve(encodedFile.first.toString())
            objectFolder.toFile().mkdir()

            Files.write(objectFile, compressContent())
        }

        return encodedFile
    }

    abstract fun decode(hash: Hash): T

    abstract fun generateKey(): Hash

    open fun compressContent(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOStream = ObjectOutputStream(byteArrayOutputStream)
        objectOStream.writeObject(this)
        objectOStream.flush()

        val deflater = Deflater()
        deflater.setInput(byteArrayOutputStream.toByteArray())
        deflater.finish() // Indicates that all data has been sent

        val outputStream = ByteArrayOutputStream(byteArrayOutputStream.toByteArray().size) // Stream to which is going to be sent the compressed data
        val buffer = ByteArray(2048)

        while (!deflater.finished()) {
            val length = deflater.deflate(buffer) // Deflate does compress the data to the buffer
            outputStream.write(buffer, 0, length) // Writes the buffered data to the outputStream

            // We need a buffer since outputStream can't be treated as a ByteArray, which is needed to send the data
            // A ByteArray is needed, and we can't send it all directly to a single ByteArray as we don't know the final length needed
        }

        return outputStream.toByteArray()
    }

    open fun decompressContent(compressedData: ByteArray): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(compressedData)
        // This does not need all the data to be decompressed before starting

        val outputStream =
            ByteArrayOutputStream(compressedData.size) // Stream to which is going to be sent the decompressed data
        val buffer = ByteArray(1024)

        try {
            while (!inflater.finished()) {
                val length = inflater.inflate(buffer) // Decompress the data into the buffer
                outputStream.write(buffer, 0, length) // Writes the decompressed data into the outputStream
            }

            return outputStream.toByteArray()
        } catch (e: DataFormatException) {
            e.printStackTrace()
            println("Error decompressing data. Check that the data is compressed correctly.")
        }

        return null
    }

    abstract fun printInfo(): String

    abstract fun showDifferences(newer: Hash, oldest: Hash): String
}