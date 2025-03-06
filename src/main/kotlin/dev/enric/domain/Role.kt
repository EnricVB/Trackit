package dev.enric.domain

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.ROLE
import dev.enric.core.TrackitObject
import dev.enric.domain.permission.RolePermission
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class Role(val name: String = "", val permissionLevel: Int = -1, val permissions: Hash = Hash("0".repeat(32))) : TrackitObject<Role>(),
    Serializable {

    override fun decode(hash: Hash): Role {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Role() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Role
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText(name, 15)

        return ROLE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Role Details"))

            append(ColorUtil.label("  Name: "))
            appendLine(
                if (name.isNotEmpty()) ColorUtil.text(name)
                else ColorUtil.message("No name assigned")
            )

            append(ColorUtil.label("  Permission Level: "))
            appendLine(
                if (permissionLevel >= 0) ColorUtil.text(permissionLevel.toString())
                else ColorUtil.message("No permission level assigned")
            )

            append(ColorUtil.label("  Permissions: "))
            val permissionsInfo = RolePermission.newInstance(permissions).printInfo()
            appendLine(
                if (permissionsInfo.isNotEmpty()) ColorUtil.text(permissionsInfo)
                else ColorUtil.message("No permissions assigned")
            )
        }
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {

        @JvmStatic
        fun newInstance(hash: Hash): Role {
            return Role().decode(hash)
        }
    }
}
