package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH
import dev.enric.core.Hash.HashType.USER
import dev.enric.domain.Branch
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object BranchIndex {
    fun getBranch(branchName: String): Branch? {
        getAllBranches().forEach {
            val branch = Branch.newInstance(it)

            if(branch.name == branchName) {
                return branch
            }
        }

        return null
    }

    fun branchAlreadyExists(branchName: String): Boolean {
        return getAllBranches().any { Branch.newInstance(it).name == branchName }
    }

    @OptIn(ExperimentalPathApi::class)
    fun getAllBranches(): List<Hash> {
        val usersFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(BRANCH.hash.toString())

        return usersFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + USER.hash + "\\"))
        }.toList()
    }
}