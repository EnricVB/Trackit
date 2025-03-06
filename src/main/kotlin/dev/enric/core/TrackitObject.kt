package dev.enric.core

import dev.enric.util.repository.RepositoryFolderManager
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * This class is the base class for all the objects that are going to be stored in the repository.
 *
 * Provides the basic methods to encode and decode the objects, as well as compress and decompress them.
 */
abstract class TrackitObject<T : TrackitObject<T>> {

    /**
     * Encodes the object into a pair of a Hash and a ByteArray.
     * The Hash is the key that is going to be used to store the object in the repository.
     * The ByteArray is the compressed object.
     * @return Pair<Hash, ByteArray> with the key and the compressed object.
     * @see Hash
     * @see encode(writeOnDisk: Boolean)
     */
    open fun encode(): Pair<Hash, ByteArray> {
        return Pair(generateKey(), compressContent())
    }

    /**
     * Encodes the object into a pair of a Hash and a ByteArray.
     * The Hash is the key that is going to be used to store the object in the repository.
     * The ByteArray is the compressed object.
     * If writeOnDisk is true, the object is going to be written in disk as a file named with the Hash and the compressed content.
     * @param writeOnDisk Boolean that indicates if the object is going to be written in disk.
     * @return Pair<Hash, ByteArray> with the key and the compressed object.
     * @see Hash
     */
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

    /**
     * Decodes the object from a Hash.
     * The Hash is the key that is going to be used to retrieve the object from the repository.
     * @param hash Hash that is going to be used to retrieve the object.
     * @return T object type that is going to be decoded into.
     */
    abstract fun decode(hash: Hash): T

    /**
     * Generates a Hash key for the object.
     * The Hash is formed by the BLAKE3 hash of the compressed object and other metadata as the object type.
     */
    abstract fun generateKey(): Hash

    /**
     * Compresses the object into a ByteArray.
     *
     * The object is serialized into a ByteArray and then compressed using the Deflater class.
     * @return ByteArray with the compressed object.
     * @see Deflater
     */
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

    /**
     * Inverse operation of compressContent.
     * Decompresses the object from a ByteArray that can be cast to the object type.
     * @param compressedData ByteArray with the compressed object.
     */
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

    /**
     * Prints the object information.
     * @return String with the object information.
     */
    abstract fun printInfo(): String

    /**
     * Shows the differences between two objects of the same type.
     * @param newer Hash of the newer object.
     * @param oldest Hash of the oldest object.
     * @return String with the differences between the two objects.
     * @see Hash
     */
    abstract fun showDifferences(newer: Hash, oldest: Hash): String
}