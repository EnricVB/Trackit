package dev.enric.domain.objects.permission

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
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
        get() = when {
            createRolePermission -> CREATE_ROLE
            modifyRolePermission -> MODIFY_ROLE
            assignRolePermission -> ASSIGN_ROLE
            userOperationPermission -> USER_OPERATION
            else -> 0
        }

    override fun decode(hash: Hash): RolePermission {
        if (!hash.string.startsWith(ROLE_PERMISSION.hash.string)) throw IllegalHashException("Hash $hash is not a Role Permission hash")

        val branchPermissionFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = branchPermissionFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Permission with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return RolePermission() // If the file is empty, return an empty Permission

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

        return ROLE_PERMISSION.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Role Permission Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Permissions: "))
            appendLine(
                ColorUtil.text(
                    (if (modifyRolePermission) "m" else "-") +
                            (if (userOperationPermission) "u" else "-") +
                            (if (assignRolePermission) "s" else "-") +
                            (if (createRolePermission) "a" else "-")
                )
            )
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

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
