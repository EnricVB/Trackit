package dev.enric.domain.objects.permission

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH_PERMISSION
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.Branch
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

data class BranchPermission(
    val readPermission: Boolean = false,
    val writePermission: Boolean = false,
    val branch: Hash = Hash("0".repeat(40))
) : TrackitObject<BranchPermission>(), Permission, Serializable {

    constructor(permissions: String, branch: Hash) : this(
        readPermission = when (permissions.getOrNull(0)) {
            'r' -> true
            '-' -> false
            else -> throw IllegalArgumentException("Invalid read permission: ${permissions.getOrNull(0)}")
        },
        writePermission = when (permissions.getOrNull(1)) {
            'w' -> true
            '-' -> false
            else -> throw IllegalArgumentException("Invalid write permission: ${permissions.getOrNull(1)}")
        },
        branch = branch
    )

    override val permission: Int
        get() = if (writePermission) WRITE else if (readPermission) READ else READ

    override fun decode(hash: Hash): BranchPermission {
        if (!hash.string.startsWith(BRANCH_PERMISSION.hash.string)) throw IllegalHashException("Hash $hash is not a Branch Permission hash")

        val branchPermissionFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = branchPermissionFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Permission with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return BranchPermission() // If the file is empty, return an empty Permission

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as BranchPermission
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText(branch.string, 15)

        return BRANCH_PERMISSION.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Branch Permission Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Branch: "))
            appendLine(ColorUtil.text(Branch.newInstance(branch).name))

            append(ColorUtil.label("  Permissions: "))
            appendLine(
                ColorUtil.text(
                    (if (readPermission) "r" else "-") + (if (writePermission) "w" else "-")
                )
            )
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        const val READ = 1 shl 0
        const val WRITE = 1 shl 1

        @JvmStatic
        fun newInstance(hash: Hash): BranchPermission {
            return BranchPermission().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(BRANCH_PERMISSION.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = BranchPermission().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as BranchPermission).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for BranchPermission: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for BranchPermission: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}
