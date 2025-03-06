package dev.enric.domain.permission

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.TrackitObject
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class BranchPermission(
    val readPermission: Boolean = false,
    val writePermission: Boolean = false
) : TrackitObject<BranchPermission>(), Permission, Serializable {

    constructor(permissions: String) : this(
        readPermission = when (permissions.getOrNull(0)) {
            'r' -> true
            '-' -> false
            else -> throw IllegalArgumentException("Invalid read permission: ${permissions.getOrNull(0)}")
        },
        writePermission = when (permissions.getOrNull(1)) {
            'w' -> true
            '-' -> false
            else -> throw IllegalArgumentException("Invalid write permission: ${permissions.getOrNull(1)}")
        }
    )

    override val permission: Int
        get() = if (writePermission) WRITE else if (readPermission) READ else READ

    override fun decode(hash: Hash): BranchPermission {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return BranchPermission() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as BranchPermission
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText("${toString().length};${readPermission};${writePermission}", 15)

        return BRANCH_PERMISSION.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Branch Permission Details"))

            append(ColorUtil.label("  Permissions: "))
            appendLine(
                ColorUtil.text(
                    (if (readPermission) "r" else "-") + (if (writePermission) "w" else "-")
                )
            )
        }
    }


    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        const val READ = 1 shl 0
        const val WRITE = 1 shl 1

        @JvmStatic
        fun newInstance(hash: Hash): BranchPermission {
            return BranchPermission().decode(hash)
        }
    }
}
