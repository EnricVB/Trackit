package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.TREE
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import dev.enric.util.common.SerializablePath
import java.io.ObjectInputStream
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

data class Tree(
    val serializablePath: SerializablePath = SerializablePath("."),
    val content: Hash = Hash.empty32()
) : TrackitObject<Tree>(), Serializable {

    constructor(path: Path, hash: Hash) : this(SerializablePath.of(path), hash)

    override fun decode(hash: Hash): Tree {
        if (!hash.string.startsWith(TREE.hash.string)) throw IllegalHashException("Hash $hash is not a Tree hash")

        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Tree with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Tree() // If the file is empty, return an empty tree

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Tree
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("$serializablePath;${content}", 15)

        return TREE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Tree Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Path: "))
            appendLine(
                if (serializablePath.toString().isNotEmpty()) ColorUtil.text(serializablePath.toString())
                else ColorUtil.message("No path assigned")
            )

            append(ColorUtil.label("  Content: "))
            appendLine(
                if (content.toString().isNotEmpty()) ColorUtil.text(content.toString())
                else ColorUtil.message("No content assigned")
            )
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash : Hash) : Tree {
            return Tree().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(TREE.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = Tree().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as Tree).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for Tree: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for Tree: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}