package dev.enric.command.repo.tag

import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.index.TagIndex

class TagHandler(val tagName : String) {

    /**
     * Assigns a tag to the commit.
     * If the tag already exists, it will be assigned to the commit.
     * Otherwise, a new SimpleTag will be created.
     *
     * @see SimpleTag
     * @see TagIndex
     */
    fun assignTag(commit: Commit) {
        if (tagName.isNotBlank()) {
            Logger.log("Adding tag $tagName for commit ${commit.generateKey()}")

            // Obtain the tag hash or create a new one if it does not exist
            val tag : Hash = if (TagIndex.existsTag(tagName)) {
                TagIndex.getTag(tagName)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(tagName).encode(true).first // Create a new tag hash
            }

            // Assign the tag to the commit in the TagIndex
            TagIndex.addTagToCommit(tag, commit.generateKey())
        }
    }

    fun removeTagFromCommit(commit: Commit) {
        if (tagName.isNotBlank()) {
            Logger.log("Removing tag $tagName from commit ${commit.generateKey()}")

            // Obtain the tag hash or create a new one if it does not exist
            val tag : Hash = if (TagIndex.existsTag(tagName)) {
                TagIndex.getTag(tagName)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(tagName).encode(true).first // Create a new tag hash
            }

            // Remove the tag from the commit in the TagIndex
            TagIndex.removeTagFromCommit(tag, commit.generateKey())
        }
    }

    fun removeTag() {
        if (tagName.isNotBlank()) {
            Logger.log("Removing tag $tagName")

            // Obtain the tag hash or create a new one if it does not exist
            val tag : Hash = if (TagIndex.existsTag(tagName)) {
                TagIndex.getTag(tagName)!!                       // Get the tag hash from the index
            } else {
                SimpleTag(tagName).encode(true).first // Create a new tag hash
            }

            // Remove the tag from all commits in the TagIndex
            TagIndex.removeTag(tag)
        }
    }
}