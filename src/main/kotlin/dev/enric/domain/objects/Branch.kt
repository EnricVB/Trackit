package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.index.BranchPermissionIndex
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

class Branch(val name: String = "", val creationDate: Timestamp = Timestamp.from(Instant.now())) : TrackitObject<Branch>(), Serializable {

    override fun decode(hash: Hash): Branch {
        if (!hash.string.startsWith(BRANCH.hash.string)) throw IllegalHashException("Hash $hash is not a Branch hash")

        val branchFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = branchFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Branch with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Branch() // If the file is empty, return an empty Branch

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Branch
    }

    override fun generateKey(): Hash {
        return BRANCH.hash.plus(Hash.parseText(name, 15))
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Branch Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Branch Name: "))
            appendLine(
                if (name.isNotEmpty()) ColorUtil.text(name)
                else ColorUtil.message("No branch name assigned")
            )

            appendLine(ColorUtil.title("  Branch Permissions:  "))
            getPermissions().forEach {
                appendLine(it.printInfo())
            }
        }
    }

    fun getPermissions(): List<BranchPermission> {
        return BranchPermissionIndex.getBranchPermissionsByBranch(name).map { BranchPermission.newInstance(it) }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash: Hash): Branch {
            return Branch().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(BRANCH.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = Branch().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as Branch).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for Branch: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for Branch: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}