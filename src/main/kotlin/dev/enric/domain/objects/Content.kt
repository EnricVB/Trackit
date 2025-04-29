package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.CONTENT
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.nio.file.Files
import java.util.zip.Deflater
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

class Content(val content: ByteArray = ByteArray(0)) : TrackitObject<Content>(), Serializable {

    override fun decode(hash: Hash): Content {
        if (!hash.string.startsWith(CONTENT.hash.string)) throw IllegalHashException("Hash $hash is not a Content hash")

        val contentFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = contentFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Content with hash $hash not found")
        val decompressedStringData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Content() // If the file is empty, return an empty content

        return Content(decompressedStringData)
    }

    override fun generateKey(): Hash {
        return CONTENT.hash.plus(Hash.parse(content, 15))
    }

    override fun compressContent(): ByteArray {
        val deflater = Deflater()
        deflater.setInput(content)
        deflater.finish() // Indicates that all data has been sent

        val outputStream =
            ByteArrayOutputStream(content.size) // Stream to which is going to be sent the compressed data
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
        return buildString {
            appendLine(ColorUtil.title("Content Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            appendLine(ColorUtil.text(String(content)))
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash: Hash): Content {
            return Content().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val contentFolder = objectsFolder.resolve(CONTENT.hash.string)
            val objectFile = contentFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedStringData = Content().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val supposedContentHash = Content(decompressedStringData).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for Content: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for Content: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}