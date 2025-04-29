package dev.enric.domain.objects.tag

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.ObjectInputStream
import java.io.Serializable
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

data class SimpleTag(
    override val name: String = "",
) : Tag, TrackitObject<SimpleTag>(), Serializable {

    override fun decode(hash: Hash): SimpleTag {
        if (!hash.string.startsWith(SIMPLE_TAG.hash.string)) throw IllegalHashException("Hash $hash is not a SimpleTag hash")

        val simpleTagFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = simpleTagFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("SimpleTag with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return SimpleTag() // If the file is empty, return an empty SimpleTag

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as SimpleTag
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${this.toString().length};$name", 15)

        return SIMPLE_TAG.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Simple Tag Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Name: "))
            appendLine(if (name.isNotEmpty()) ColorUtil.text(name) else ColorUtil.message("No name assigned"))
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash : Hash) : SimpleTag {
            return SimpleTag().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(SIMPLE_TAG.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = SimpleTag().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as SimpleTag).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for SimpleTag: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for SimpleTag: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}
