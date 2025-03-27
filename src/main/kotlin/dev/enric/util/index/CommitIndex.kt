package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMMIT
import dev.enric.domain.Hash.HashType.USER
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.util.repository.RepositoryFolderManager
import java.io.File
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * This object is responsible for managing the commit index.
 * It allows to get all the commits, current commit, or filtered by branch.
 * Also allows to get a specific commit based on his hash abreviation.
 */
object CommitIndex {
    val repositoryFolderManager = RepositoryFolderManager()

    /**
     * Retrieves the current commit the Working Direcotry has.
     *
     * @return The current commit.
     */
    fun getCurrentCommit() : Commit? {
        val hashString = Files.readString(repositoryFolderManager.getCurrentCommitPath())

        return if(hashString.isNullOrEmpty()) null else Commit.newInstance(Hash(hashString))
    }

    /**
     * Updates the current commit index file to the specified commit.
     *
     * @param commit A [Hash] object representing the new current commit.
     */
    fun setCurrentCommit(commit: Hash) {
        Files.writeString(repositoryFolderManager.getCurrentCommitPath(), commit.string)
    }

    /**
     * Retrieves a commit based on the first X letters of the hash.
     * In case multiple commits start with the same pattern, returns all.
     *
     * @param abbreviatedHash An abbreviated string hash of the commit searched.
     * @return A [Hash] or list of [Hash] objects representing the found commit.
     */
    fun getAbbreviatedCommit(abbreviatedHash: String) : List<Hash> {
        return getAllCommit().filter { it.string.startsWith(abbreviatedHash.trim()) }
    }

    /**
     * Checks if a commit exists in the index.
     *
     * @param hash The hash of the commit to be checked.
     * @return True if the commit exists, false otherwise.
     */
    fun commitExists(hash: Hash): Boolean {
        return Files.exists(repositoryFolderManager.getObjectsFolderPath().resolve(COMMIT.hash.toString()).resolve(hash.string))
    }

    /**
     * Retrieves all the commits filtered by his branch name.
     *
     * @return A list of [Hash] objects representing the branch commits.
     */
    fun getCommitByBranch(branchName: String): List<Hash> {
        return getAllCommit()
            .map { Commit.newInstance(it) }
            .filter { Branch.newInstance(it.branch).name == branchName }
            .map { it.generateKey() }
    }

    /**
     * Retrieves all the commits.
     *
     * @return A list of [Hash] objects representing the commits.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getAllCommit(): List<Hash> {
        val commitFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(COMMIT.hash.toString())

        return commitFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast(File.separator + USER.hash + File.separator))
        }.toList()
    }
}