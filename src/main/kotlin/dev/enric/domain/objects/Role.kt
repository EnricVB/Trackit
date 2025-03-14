package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.exceptions.IllegalHashException
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
        if (!hash.string.startsWith(ROLE.hash.string)) throw IllegalHashException("Hash $hash is not a Role hash")

        val roleFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = roleFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Role with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Role() // If the file is empty, return an empty role

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
            .filter { it.string.startsWith(ROLE_PERMISSION.hash.string) }
            .map { RolePermission.newInstance(it) }
    }

    fun getBranchPermissions(): List<BranchPermission> {
        return permissions
            .filter { it.string.startsWith(BRANCH_PERMISSION.hash.string) }
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

            getRolePermissions().forEach { appendLine(it.printInfo()) }
            getBranchPermissions().forEach { appendLine(it.printInfo()) }
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
