package dev.enric.core.objects.permission

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH_PERMISSION
import dev.enric.core.TrackitObject
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files


data class RolePermission(
    var createRolePermission: Boolean = false, // Create roles if role level is lower
    var modifyRolePermission: Boolean = false, // Modify role permissions if role level is lower
    var assignRolePermission: Boolean = false, // Assign roles to other users if role level is lower
    var userOperationPermission: Boolean = false // Create, modify and delete users if role level is lower
) : TrackitObject<RolePermission>(), Permission, Serializable {

    constructor(permissions: String) : this() {
        require(permissions.length >= 4) { "The permissions string must be at least 4 characters long." }

        val validChars = listOf('m', 'u', 's', 'a')
        val properties = listOf(::createRolePermission, ::modifyRolePermission, ::assignRolePermission, ::userOperationPermission)

        permissions.take(4).forEachIndexed { index, char ->
            properties[index].set(
                when (char) {
                    validChars[index] -> true
                    '-' -> false
                    else -> throw IllegalArgumentException("Invalid permission at position ${index + 1}: $char")
                }
            )
        }
    }

    override val permission: Int
        get() = if (createRolePermission) CREATE_ROLE else if (modifyRolePermission) MODIFY_ROLE else if (assignRolePermission) ASSIGN_ROLE else if (userOperationPermission) USER_OPERATION else 0

    override fun decode(hash: Hash): RolePermission {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return RolePermission() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as RolePermission
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText(
            """
            ${toString().length};
            $createRolePermission;
            $modifyRolePermission;
            $assignRolePermission;
            $userOperationPermission
            """.trimIndent(),
            15
        )

        return BRANCH_PERMISSION.hash.plus(hashData)
    }

    override fun printInfo(): String {
        var message = "RolePermission("
        message += if (modifyRolePermission) "m" else "-"
        message += if (userOperationPermission) "u" else "-"
        message += if (assignRolePermission) "s" else "-"
        message += if (createRolePermission) "a" else "-"
        message += ")"

        return message
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        const val CREATE_ROLE = 1 shl 0
        const val MODIFY_ROLE = 1 shl 1
        const val ASSIGN_ROLE = 1 shl 2
        const val USER_OPERATION = 1 shl 3

        @JvmStatic
        fun newInstance(hash: Hash): RolePermission {
            return RolePermission().decode(hash)
        }
    }
}
