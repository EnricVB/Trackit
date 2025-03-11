package dev.enric.domain.remote

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.REMOTE
import dev.enric.core.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

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

            append(ColorUtil.label("  Protocol: "))
            appendLine(protocol?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No protocol assigned"))

            append(ColorUtil.label("  Type: "))
            appendLine(type?.let { ColorUtil.text(it.toString()) } ?: ColorUtil.message("No type assigned"))
        }
    }


    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : Remote {
            return Remote().decode(hash)
        }
    }
}