package dev.enric.domain.objects.remote

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.REMOTE
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

data class Remote(
    val protocol: DataProtocol? = null,
    val type: RemoteType? = null
) : TrackitObject<Remote>(), Serializable {

    override fun decode(hash: Hash): Remote {
        if (!hash.string.startsWith(REMOTE.hash.string)) throw IllegalHashException("Hash $hash is not a Remote hash")

        val remoteFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = remoteFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Remote with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Remote() // If the file is empty, return an empty Remote

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Remote
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${toString().length};${protocol.toString()};${type.toString()}", 15)

        return REMOTE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Remote Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Protocol: "))
            appendLine(protocol?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No protocol assigned"))

            append(ColorUtil.label("  Type: "))
            appendLine(type?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No type assigned"))
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash : Hash) : Remote {
            return Remote().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(REMOTE.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = Remote().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as Remote).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for Remote: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for Remote: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}