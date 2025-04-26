package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.BRANCH
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
    fun exists(branchName: String): Boolean {
        return getAllBranches().any { Branch.newInstance(it).name == branchName }
    }

    /**
     * Saves the Branch Head Pointer to the given commit.
     *
     * @param branchHash The hash of the branch.
     * @param commitHash The hash of the commit to set as the head of the branch.
     * @return `true` if the branch head was updated, `false` otherwise.
     */
    fun setBranchHead(branchHash: Hash, commitHash: Hash) {
        val branchHeadPath = RepositoryFolderManager().getBranchHeadPath()
        val lines = Files.readAllLines(branchHeadPath).toMutableList()

        var updated = false
        for (i in lines.indices) {
            if (lines[i].startsWith(branchHash.string)) {
                lines[i] = "${branchHash.string}:${commitHash.string}"

                updated = true
                break
            }
        }

        if (!updated) {
            lines.add("${branchHash.string}:${commitHash.string}")
        }

        Files.write(branchHeadPath, lines)
    }

    /**
     * Retrieves the Branch Head Pointer for a specific branch hash.
     *
     * @param branchHash The hash of the branch.
     * @return A [Commit] representing the head of the branch, or null if not found.
     */
    fun getBranchHead(branchHash: Hash): Commit {
        val branchHeadPath = RepositoryFolderManager().getBranchHeadPath()
        val lines = Files.readAllLines(branchHeadPath).toMutableList()

        val targetLine = lines.find { it.startsWith(branchHash.string) } ?: throw throw IllegalStateException("Commit HEAD not found for branch $branchHash. Try commiting a change first.")
        val split = targetLine.trim().split(":").takeIf { it.size == 2 } ?: throw IllegalStateException("Branch head file is corrupted: expected ':' separator.")

        return Commit.newInstance(Hash(split[1]))
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
        return if(CommitIndex.getCurrentCommit()?.branch != null) Branch.newInstance(CommitIndex.getCurrentCommit()?.branch!!) else Branch("main")
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
            Hash(it.toString().substringAfterLast(File.separator + BRANCH.hash + File.separator))
        }.toList()
    }
}