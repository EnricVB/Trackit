package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.domain.objects.tag.Tag
import dev.enric.util.repository.RepositoryFolderManager

object TagIndex {
    /**
     * Checks if a tag exists in the repository.
     * A tag is considered to exist if it is present in the tag index with the exact same name.
     * This check is case sensitive.
     *
     * @param tagName The name of the tag to check for existence.
     * @return True if the tag exists, false otherwise.
     */
    fun existsTag(tagName: String): Boolean {
        return getTag(tagName) != null
    }

    /**
     * Gets the commit hash associated with a tag.
     * This method searches the tag index for the given tag name and returns the corresponding tag hash.
     * If the tag does not exist, it returns null.
     *
     * @param tagName The name of the tag to search for.
     * @return The commit hash associated with the tag, or null if the tag does not exist.
     */
    fun getTag(tagName: String): Hash? {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        // Iterate through all tags in the index
        tags.forEach {
            val tagHash = it.split(":").first()
            val isComplexTag = tagHash.startsWith(COMPLEX_TAG.hash.string)
            val isSimpleTag = tagHash.startsWith(SIMPLE_TAG.hash.string)

            // Create the corresponding tag object based on the type
            val tag: Tag = if (isComplexTag) {
                ComplexTag.newInstance(Hash(tagHash))
            } else if (isSimpleTag) {
                SimpleTag.newInstance(Hash(tagHash))
            } else {
                return@forEach
            }


            // Check if the tag name matches the requested tag name
            if (tag.name == tagName) {
                return Hash(tagHash)
            }
        }

        // Return null if the tag does not exist
        return null
    }

    /**
     * Adds a tag to the repository index.
     * This method updates the tag index by adding the specified tag hash to the list of tags.
     * If the tag already exists, it will not be added again.
     *
     * @param tagHash The hash of the tag to add to the index.
     */
    fun addTag(tagHash: Hash) {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines().toMutableList()

        // Check if the tag already exists in the index
        if (tags.any { it.startsWith("${tagHash.string}:") }) {
            return
        }

        // Add the new tag to the index
        tags.add("${tagHash.string}:")

        // Write the updated tag index back to the file
        tagIndex.writeText(tags.joinToString("\n"))
    }

    /**
     * Associates a commit with a tag.
     * This method updates the tag index by adding the commit hash to the list of commits associated with the given tag.
     * If the tag does not exist in the index, it creates a new entry for the tag with the given commit hash.
     *
     * @param tag The tag hash to associate with the commit.
     * @param commit The commit hash to associate with the tag.
     */
    fun addTagToCommit(tag: Hash, commit: Hash) {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines().toMutableList()

        // Find the line where the tag is already present
        val tagLineIndex = tags.indexOfFirst { it.startsWith("${tag.string}:") }

        if (tagLineIndex != -1) {
            // If the tag is already present, add the commit to the existing list of commits
            val currentLine = tags[tagLineIndex]
            val existingCommits = currentLine.split(":")[1].trim().split(",").toMutableList()

            // If the commit is already present, do not add it again
            if (existingCommits.contains(commit.string)) {
                return
            }

            existingCommits.add(commit.string)
            tags[tagLineIndex] = "${tag.string}:${existingCommits.joinToString(",")}"
        } else {
            // If the tag is not found, create a new entry for the tag with the commit
            tags.add("${tag.string}:${commit.string}")
        }

        // Write the updated tag index back to the file
        tagIndex.writeText(tags.joinToString("\n"))
    }

    /**
     * Removes a tag from the list of tags associated with a commit.
     * This method updates the tag index by removing the specified tag from the list of tags associated with the given commit.
     * If the tag is the only one associated with the commit, the line for the tag is removed from the index.
     *
     * @param tag The tag hash to remove from the commit's list of associated tags.
     * @param commit The commit hash from which to remove the tag.
     */
    fun removeTagFromCommit(tag: Hash, commit: Hash) {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines().toMutableList()

        // Find the line where the tag is present
        val tagLineIndex = tags.indexOfFirst { it.startsWith("${tag.string}:") }

        if (tagLineIndex != -1) {
            // If the tag is found, remove the commit from the list of commits
            val currentLine = tags[tagLineIndex]
            val existingCommits = currentLine.split(":")[1].trim().split(",").toMutableList()

            // Remove the commit hash from the list of commits if it exists
            existingCommits.remove(commit.string)

            // Update the line with the remaining commits
            tags[tagLineIndex] = "${tag.string}:${existingCommits.joinToString(",")}"
        }

        // Write the updated tag index back to the file
        tagIndex.writeText(tags.joinToString("\n"))
    }

    /**
     * Removes a tag from the repository index.
     * This method removes the specified tag from the tag index, effectively deleting the tag from the repository.
     * All associations between the tag and commits are removed.
     *
     * @param tag The tag hash to remove from the repository.
     */
    fun removeTag(tag: Hash) {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines().toMutableList()

        // Find the line where the tag is present
        val tagLineIndex = tags.indexOfFirst { it.startsWith("${tag.string}:") }

        if (tagLineIndex != -1) {
            // If the tag is found, remove the entire line
            tags.removeAt(tagLineIndex)
        }

        // Write the updated tag index back to the file
        tagIndex.writeText(tags.joinToString("\n"))
    }

    /**
     * Gets a list of commits associated with a tag.
     * This method searches the tag index for the given tag and returns a list of all commit hashes associated with it.
     * If the tag does not exist, an empty list is returned.
     *
     * @param tag The tag hash for which to retrieve the associated commits.
     * @return A list of commit hashes associated with the given tag.
     */
    fun getCommitsByTag(tag: Hash): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        // Filter lines that start with the given tag and extract the commits
        return tags.filter { it.startsWith("${tag.string}:") }
            .flatMap { line ->
                if (line.split(":")[1].isBlank()) {
                    return@flatMap emptyList<Hash>()
                }
                // Extract commit hashes after the colon
                line.split(":")[1].trim().split(",").map { Hash(it) }
            }
    }

    /**
     * Gets a list of commits associated with a tag.
     * This method searches the tag index for the given tag and returns a list of all commit hashes associated with it.
     * If the tag does not exist, an empty list is returned.
     *
     * @param tag The tag name for which to retrieve the associated commits.
     * @return A list of commit hashes associated with the given tag.
     */
    fun getCommitsByTag(tag: String): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()
        val tagHash = getTag(tag)

        // Filter lines that start with the given tag and extract the commits
        return tags.filter { it.startsWith("$tagHash:") }
            .flatMap { line ->
                if (line.split(":")[1].isBlank()) {
                    return@flatMap emptyList<Hash>()
                }
                // Extract commit hashes after the colon
                line.split(":")[1].trim().split(",").map { Hash(it) }
            }
    }

    /**
     * Gets a list of tags associated with a commit.
     * This method searches the tag index for the given commit and returns a list of all tag hashes associated with it.
     * If the commit does not have any associated tags, an empty list is returned.
     *
     * @param commit The commit hash for which to retrieve the associated tags.
     * @return A list of tag hashes associated with the given commit.
     */
    fun getTagsByCommit(commit: Hash): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        // Filter lines that contain the commit and extract the tags
        return tags.filter { it.contains(commit.string) }.map {
            Hash(it.split(":")[0])
        }
    }

    /**
     * Gets the hash of a tag by its name.
     * This method searches the tag index for the given tag name and returns its associated hash.
     * If the tag does not exist, it returns null.
     *
     * @param tagName The name of the tag to search for.
     * @return The hash of the tag if found, otherwise null.
     */
    fun getTagsByName(tagName: String): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        return tags.filter { tagName.isBlank() || it.contains(tagName) }.map {
            Hash(it.split(":")[0])
        }
    }

    /**
     * Gets a list of unused tags in the repository.
     * This method searches the tag index for tags that are not associated with any commits
     *
     * @return A list of unused tag hashes.
     */
    fun getUnusedTags(): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        return tags.filter { it.split(":")[1].isBlank() }.map { Hash(it.split(":")[0]) }
    }

    /**
     * Gets a list of all tags in the repository.
     * This method retrieves all tags from the tag index and returns them as a list.
     *
     * @return A list of all tag hashes in the repository.
     */
    fun getAllTags(): List<Hash> {
        val tagIndex = RepositoryFolderManager().getTagIndexPath().toFile()
        val tags = tagIndex.readLines()

        return tags.map { Hash(it.split(":")[0]) }
    }
}