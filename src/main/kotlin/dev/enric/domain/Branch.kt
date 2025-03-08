package dev.enric.domain

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH
import dev.enric.core.TrackitObject
import dev.enric.domain.permission.BranchPermission
import dev.enric.util.common.ColorUtil
import dev.enric.util.index.BranchPermissionIndex
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files

class Branch(val branchName: String = "") : TrackitObject<Branch>(), Serializable {

    override fun decode(hash: Hash): Branch {
        val branchFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = branchFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return Branch() // If the file is empty, return an empty Branch

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Branch
    }

    override fun generateKey(): Hash {
        return BRANCH.hash.plus(Hash.parseText(branchName, 15))
    }

    override fun printInfo(): String {
        return buildString {
            appendLine(ColorUtil.title("Branch Details"))

            append(ColorUtil.label("  Branch Name: "))
            appendLine(
                if (branchName.isNotEmpty()) ColorUtil.text(branchName)
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

        return "Newer content: ${newerContent.branchName}\nOldest content: ${oldestContent.branchName}" // TODO: Implement a better way to show differences
    }

    fun getPermissions(): List<BranchPermission> {
        return BranchPermissionIndex.getBranchPermissionsByBranch(branchName).map { BranchPermission.newInstance(it) }
    }

    companion object {
        @JvmStatic
        fun newInstance(hash: Hash): Branch {
            return Branch().decode(hash)
        }
    }
}