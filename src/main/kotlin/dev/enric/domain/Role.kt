package dev.enric.domain

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.*
import dev.enric.core.TrackitObject
import dev.enric.domain.permission.BranchPermission
import dev.enric.domain.permission.RolePermission
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

data class Role(
    val name: String = "",
    val permissionLevel: Int = -1,
    val permissions: MutableList<Hash> = mutableListOf()
) : TrackitObject<Role>(),
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

    fun getRolePermissions(): List<RolePermission> {
        return permissions
            .filter { it.string.substring(2) == ROLE_PERMISSION.hash.string }
            .map { RolePermission.newInstance(it) }
    }

    fun getBranchPermissions(): List<BranchPermission> {
        return permissions
            .filter { it.string.substring(2) == BRANCH_PERMISSION.hash.string }
            .map { BranchPermission.newInstance(it) }
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

            append(ColorUtil.label("  Role Permissions: "))
            getRolePermissions().forEach(RolePermission::printInfo)

            append(ColorUtil.label("  Branch Permissions: "))
            getBranchPermissions().forEach(BranchPermission::printInfo)
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
