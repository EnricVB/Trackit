package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
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
        val objectIStream = ObjectInputStream(byteArrayInputStream)

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

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

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

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newInstance(hash: Hash): Role {
            return Role().decode(hash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(ROLE.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = Role().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as Role).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for Role: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for Role: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}
