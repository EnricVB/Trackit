package dev.enric.domain.objects

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.exceptions.IllegalHashException
import dev.enric.util.common.ColorUtil
import dev.enric.util.index.BranchPermissionIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

class Branch(val name: String = "") : TrackitObject<Branch>(), Serializable {

    override fun decode(hash: Hash): Branch {
        if (!hash.string.startsWith(BRANCH.hash.string)) throw IllegalHashException("Hash $hash is not a Branch hash")

        val branchFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = branchFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("Branch with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Branch() // If the file is empty, return an empty Branch

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Branch
    }

    override fun generateKey(): Hash {
        return BRANCH.hash.plus(Hash.parseText(name, 15))
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Branch Details"))

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

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        val newerContent = decode(newer)
        val oldestContent = decode(oldest)

        return "Newer content: ${newerContent.name}\nOldest content: ${oldestContent.name}" // TODO: Implement a better way to show differences
    }

    fun getPermissions(): List<BranchPermission> {
        return BranchPermissionIndex.getBranchPermissionsByBranch(name).map { BranchPermission.newInstance(it) }
    }

    companion object {
        @JvmStatic
        fun newInstance(hash: Hash): Branch {
            return Branch().decode(hash)
        }
    }
}