package dev.enric.domain.objects.tag

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.ObjectInputStream
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

data class ComplexTag(
    override val name: String = "",
    val message: String = "",
    val user: Hash = Hash.empty32(),
    val date: Timestamp = Timestamp.from(Instant.now())
) : Tag, TrackitObject<ComplexTag>(), Serializable {

    override fun decode(hash: Hash): ComplexTag {
        if (!hash.string.startsWith(COMPLEX_TAG.hash.string)) throw IllegalHashException("Hash $hash is not a ComplexTag hash")

        val complexTagFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = complexTagFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("ComplexTag with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return ComplexTag() // If the file is empty, return an empty ComplexTag

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as ComplexTag
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${this.toString().length};$name$date$user", 15)

        return COMPLEX_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Complex Tag Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Name: "))
            appendLine(if (name.isNotEmpty()) ColorUtil.text(name) else ColorUtil.message("No name assigned"))

            append(ColorUtil.label("  User: "))
            appendLine(ColorUtil.text(user.toString()))

            append(ColorUtil.label("  Date: "))
            appendLine(ColorUtil.text(date.toString()))

            append(ColorUtil.label("  Message: "))
            appendLine(if (message.isNotEmpty()) ColorUtil.text(message) else ColorUtil.message("No message assigned"))
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash : Hash) : ComplexTag {
            return ComplexTag().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(COMPLEX_TAG.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = ComplexTag().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as ComplexTag).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for ComplexTag: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for ComplexTag: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}
