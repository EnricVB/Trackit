package dev.enric.core.handler.repo.tag

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Role
import dev.enric.domain.objects.User
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.exceptions.CommitNotFoundException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.index.CommitIndex
import dev.enric.util.index.TagIndex

open class TagHandler(
    val name: String,
    val commits: List<String> = emptyList(),
    val sudoArgs: Array<String>? = null
) : CommandHandler() {

    /**
     * Checks if the user has the necessary permissions to modify the branch.
     *
     * This method ensures that the commits exist and verifies that the user has the required
     * permissions to modify the branch associated with those commits.
     *
     * @return True if the user has the required permissions, otherwise an exception is thrown.
     * @throws CommitNotFoundException If any of the provided commits do not exist.
     * @throws InvalidPermissionException If the user lacks the necessary permissions.
     */
    fun checkCanAssignTags(): Boolean {
        commitsExists()
        hasModifyRolePermissions(isValidSudoUser(sudoArgs))
        return true
    }

    /**
     * Validates that all provided commit hashes exist in the repository.
     *
     * @throws CommitNotFoundException If any commit does not exist.
     */
    private fun commitsExists() {
        commits.forEach {
            val commitHash = Hash(it)
            if (!CommitIndex.commitExists(commitHash)) {
                throw CommitNotFoundException("Commit $commitHash does not exist. Create the commit first.")
            }
        }
    }

    /**
     * Verifies whether the user has the necessary permissions to modify the branches of the provided commits.
     *
     * @param user The user whose permissions need to be checked.
     * @return True if the user has the required permissions, otherwise an exception is thrown.
     * @throws InvalidPermissionException If the user lacks the necessary permissions.
     */
    private fun hasModifyRolePermissions(user: User): Boolean {
        val branchPermissions = user.roles
            .flatMap { Role.newInstance(it).getBranchPermissions() }
            .filter { it.writePermission }
            .map { it.branch }
            .toSet()

        if (!commits.map { Commit.newInstance(Hash(it)) }.all { it.branch in branchPermissions }) {
            throw InvalidPermissionException("You don't have permission to modify the commits' branch.")
        }

        return true
    }

    /**
     * Assigns a tag to the commit.
     * If the tag already exists, it will be assigned to the commit.
     * Otherwise, a new SimpleTag will be created.
     *
     * @see SimpleTag
     * @see TagIndex
     */
    fun assignTag(commit: Commit) {
        if (name.isNotBlank()) {
            Logger.log("Adding tag $name for commit ${commit.generateKey()}")

            // Obtain the tag hash or create a new one if it does not exist
            val tag: Hash = if (TagIndex.existsTag(name)) {
                TagIndex.getTag(name)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(name).encode(true).first // Create a new tag hash
            }

            // Assign the tag to the commit in the TagIndex
            TagIndex.addTagToCommit(tag, commit.generateKey())
        }
    }

    fun removeTagFromCommit(commit: Commit) {
        if (name.isNotBlank()) {
            Logger.log("Removing tag $name from commit ${commit.generateKey()}")

            // Obtain the tag hash or create a new one if it does not exist
            val tag: Hash = if (TagIndex.existsTag(name)) {
                TagIndex.getTag(name)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(name).encode(true).first // Create a new tag hash
            }

            // Remove the tag from the commit in the TagIndex
            TagIndex.removeTagFromCommit(tag, commit.generateKey())
        }
    }

    fun removeTag() {
        if (name.isNotBlank()) {
            Logger.log("Removing tag $name")

            // Obtain the tag hash or create a new one if it does not exist
            val tag: Hash = if (TagIndex.existsTag(name)) {
                TagIndex.getTag(name)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(name).encode(true).first // Create a new tag hash
            }

            // Remove the tag from all commits in the TagIndex
            TagIndex.removeTag(tag)
        }
    }
}