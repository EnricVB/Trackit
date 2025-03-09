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

/**
 * This object is responsible for managing the branches index.
 * It allows to get all the branches, check if a branch already exists and get a branch by its name.
 */
object BranchIndex {

    /**
     * Retrieves a branch by its name.
     *
     * @param branchName The name of the branch to retrieve.
     * @return A [Branch] object if found, or `null` if no branch with the given name exists.
     */
    fun getBranch(branchName: String): Branch? {
        getAllBranches().forEach {
            val branch = Branch.newInstance(it)

            if(branch.name == branchName) {
                return branch
            }
        }

        return null
    }

    /**
     * Checks if a branch with the given name already exists.
     *
     * @param branchName The name of the branch to check.
     * @return `true` if a branch with the given name exists, `false` otherwise.
     */
    fun branchAlreadyExists(branchName: String): Boolean {
        return getAllBranches().any { Branch.newInstance(it).name == branchName }
    }

    /**
     * Retrieves all the branches.
     *
     * @return A list of [Hash] objects representing the branches.
     */
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