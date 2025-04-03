package dev.enric.core.handler.repo.tag

import dev.enric.domain.Hash
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.index.TagIndex
import java.sql.Timestamp
import java.time.Instant

/**
 * Handles the creation of tags in the repository.
 *
 * This class is responsible for verifying permissions, ensuring commits exist, and creating either
 * simple or complex tags based on the provided information.
 *
 * @property name The name of the tag to be created.
 * @property message The message associated with a complex tag. If empty, a simple tag is created.
 * @property commits The list of commit hashes to which the tag should be assigned.
 * @property sudoArgs Optional sudo arguments to verify administrative privileges.
 */
class TagCreationHandler(
    name: String,
    val message: String = "",
    commits: List<Hash> = emptyList(),
    sudoArgs: Array<String>? = null
) : TagHandler(name, commits, sudoArgs) {

    /**
     * Initiates the creation of a tag.
     *
     * Determines whether a simple or complex tag should be created based on the presence of a message.
     * Once created, the tag is assigned to the specified commits.
     */
    fun createTag() {
        val isComplexTag = message.isNotEmpty()
        val tagHash = if (isComplexTag) createComplexTag() else createSimpleTag()
        assignCommitsToTag(tagHash)
    }

    /**
     * Creates a complex tag with an associated message.
     *
     * @return The hash of the newly created complex tag.
     */
    private fun createComplexTag(): Hash {
        val complexTag = ComplexTag(
            name,
            message,
            isValidSudoUser(sudoArgs).generateKey(),
            Timestamp.from(Instant.now())
        )

        Logger.log("Creating complex tag $name")
        return complexTag.encode(true).first
    }

    /**
     * Creates a simple tag without an associated message.
     *
     * @return The hash of the newly created simple tag.
     */
    private fun createSimpleTag(): Hash {
        val simpleTag = SimpleTag(name)

        Logger.log("Creating simple tag $name")
        return simpleTag.encode(true).first
    }

    /**
     * Assigns the created tag to the specified commits.
     *
     * @param tagHash The hash of the tag to be assigned.
     */
    private fun assignCommitsToTag(tagHash: Hash) {
        TagIndex.addTag(tagHash)

        commits.forEach {
            TagIndex.addTagToCommit(tagHash, it)
        }
    }
}