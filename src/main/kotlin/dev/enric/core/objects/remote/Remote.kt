package dev.enric.core.objects.remote

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.REMOTE
import dev.enric.core.TrackitObject
import dev.enric.util.ColorUtil
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class Remote(
    val protocol: DataProtocol? = null,
    val type: RemoteType? = null
) : TrackitObject<Remote>(), Serializable {

    override fun decode(hash: Hash): Remote {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Remote() // If the file is empty, return an empty commit

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