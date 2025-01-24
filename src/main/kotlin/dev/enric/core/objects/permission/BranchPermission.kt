package dev.enric.core.objects.permission

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.TrackitObject
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class BranchPermission(
    val readPermission: Boolean = false,
    val writePermission: Boolean = false
) : TrackitObject<BranchPermission>(), Permission, Serializable {

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
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${toString().length};${readPermission};${writePermission}", 15)

        return BRANCH_PERMISSION.hash.plus(hashData)
    }

    override fun printInfo(): String {
        var message = "BranchPermission("
        message += if(readPermission) "r" else "-"
        message += if(writePermission) "w" else "-"
        message += ")"

        return message
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
