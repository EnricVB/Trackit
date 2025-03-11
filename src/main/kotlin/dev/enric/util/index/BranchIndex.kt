package dev.enric.util.index

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.BRANCH
import dev.enric.core.Hash.HashType.USER
import dev.enric.domain.Branch
import dev.enric.domain.Commit
import dev.enric.util.index.CommitIndex.repositoryFolderManager
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
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
     * Saves the Branch Head Pointer to the given commit.
     *
     * @param commit A [Hash] object representing the commit to set as the branch head.
     */
    fun setBranchHead(commit: Hash) {
        Files.writeString(repositoryFolderManager.getBranchHeadPath(), commit.string)
    }

    /**
     * Retrieves the Branch Head Pointer.
     * This is the commit the branch is pointing to.
     *
     * @return A [Commit] object representing the branch head.
     */
    fun getBranchHead() : Commit? {
        val hashString = Files.readString(repositoryFolderManager.getBranchHeadPath())

        return if(hashString.isNullOrEmpty()) null else Commit.newInstance(Hash(hashString))
    }

    /**
     * Retrieves the current branch.
     *
     * This is the branch the Working Directory is currently on.
     * In case this is a new repository and no branch has been created yet, it will return 'main' branch.
     *
     * @return A [Branch] object representing the current branch.
     */
    fun getCurrentBranch() : Branch {
        return if(getBranchHead()?.branch != null) Branch.newInstance(getBranchHead()?.branch!!) else Branch("main")
    }

    /**
     * Retrieves all the branches.
     *
     * @return A list of [Hash] objects representing the branches.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getAllBranches(): List<Hash> {
        val branchFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(BRANCH.hash.toString())

        return branchFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + USER.hash + "\\"))
        }.toList()
    }
}