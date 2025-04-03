package dev.enric.command.commit

import dev.enric.command.CommandTest
import dev.enric.core.handler.management.users.UserCreationHandler
import dev.enric.core.handler.repo.commit.CommitHandler
import dev.enric.core.handler.repo.init.InitHandler
import dev.enric.core.handler.repo.tag.TagCreationHandler
import dev.enric.core.handler.repo.tag.TagHandler
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.index.TagIndex
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class TagCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"

        const val USERNAME_2 = "test2"
        const val PASSWORD_2 = "password2"

        const val TAG_NAME = "test-tag"

        const val COMMIT_TITLE = "Test commit"
        const val COMMIT_MESSAGE = "This is a test commit"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$MAIL\n$PHONE\n$PASSWORD\n$PASSWORD\n")
        InitHandler.init()
    }

    @Test
    fun `User can create tag if has write permission`() {
        // Given
        val handler = TagCreationHandler(
            TAG_NAME,
            "",
            emptyList(),
            arrayOf(USERNAME, PASSWORD)
        )

        // When
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to create a tag")
        }

        handler.createTag()

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)

        assertNotNull(tagHash, "Tag should be created")
    }

    @Test
    fun `User cannot create tag if has no write permission`() {
        // Given
        UserCreationHandler(USERNAME_2, PASSWORD_2, null, null, emptyArray(), arrayOf(USERNAME, PASSWORD)).createUser()

        val handler = TagCreationHandler(
            TAG_NAME,
            "",
            emptyList(),
            arrayOf(USERNAME_2, PASSWORD_2)
        )

        // When
        assertFailsWith<InvalidPermissionException> {
            handler.checkCanModifyTags()
            handler.createTag()
        }

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)

        assertNull(tagHash, "Tag should not be created")
    }

    @Test
    fun `Can create tag with commit`() {
        // Given
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Given

        val handler = TagCreationHandler(
            TAG_NAME,
            "",
            listOf(commit.generateKey()),
            arrayOf(USERNAME, PASSWORD)
        )

        // When
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to create a tag")
        }

        handler.createTag()

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)
        val commitsToTagByName = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHash = TagIndex.getCommitsByTag(tagHash!!)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByName, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHash, "Tag should have commits associated with it")
    }

    @Test
    fun `If adding Tag already created with same name wont be added`() {
        // Given
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf(TAG_NAME))

        // Given
        val handler = TagCreationHandler(
            TAG_NAME,
            "",
            emptyList(),
            arrayOf(USERNAME, PASSWORD)
        )

        // When
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to create a tag")
        }

        handler.createTag()

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)
        val commitsToTagByName = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHash = TagIndex.getCommitsByTag(tagHash!!)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByName, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHash, "Tag should have commits associated with it")
        assert(commitsToTagByName.size == 1) { "Tag should have only one commit associated with it but found ${commitsToTagByName.size}" }
        assert(TagIndex.getTags().size == 1) { "Only one tag should have been created but found ${TagIndex.getTags().size}" }
    }

    @Test
    fun `User with permissions can assign a commit to a existing tag`() {
        // Given
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))

        commitHandler.preCommit(true)

        commitHandler.canDoCommit()
        commitHandler.processCommit()

        commitHandler.postCommit(listOf(TAG_NAME))

        // Given
        val handler = TagHandler(TAG_NAME, listOf(commit.generateKey()), arrayOf(USERNAME, PASSWORD))

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to assign a tag")
        }

        // Assign the tag to the commits
        handler.assignTag(commit)

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)
        val commitsToTagByName = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHash = TagIndex.getCommitsByTag(tagHash!!)
        val commitsByTag = TagIndex.getCommitsByTag(tagHash)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByName, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHash, "Tag should have commits associated with it")
        assert(commitsToTagByName.size == 1) { "Tag should have only one commit associated with it but found ${commitsToTagByName.size}" }
        assert(TagIndex.getTags().size == 1) { "Only one tag should have been created but found ${TagIndex.getTags().size}" }

        assert(commitsByTag.size == 1) { "Tag should have one commit associated with it but found ${commitsByTag.size}" }
        assert(commitsByTag.first() == commit.generateKey()) { "Tag should have commit ${commit.generateKey()} associated with it but found ${commitsByTag.first()}" }
    }

    @Test
    fun `User with permissions can remove tags from commits`() {
        // Given
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))

        commitHandler.preCommit(true)

        commitHandler.canDoCommit()
        commitHandler.processCommit()

        commitHandler.postCommit(listOf(TAG_NAME))

        // Given
        val handler = TagHandler(TAG_NAME, listOf(commit.generateKey()), arrayOf(USERNAME, PASSWORD))

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to assign a tag")
        }

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)
        val commitsToTagByName = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHash = TagIndex.getCommitsByTag(tagHash!!)
        val commitsByTag = TagIndex.getCommitsByTag(tagHash)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByName, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHash, "Tag should have commits associated with it")
        assert(commitsToTagByName.size == 1) { "Tag should have only one commit associated with it but found ${commitsToTagByName.size}" }
        assert(TagIndex.getTags().size == 1) { "Only one tag should have been created but found ${TagIndex.getTags().size}" }

        assert(commitsByTag.size == 1) { "Tag should have one commit associated with it but found ${commitsByTag.size}" }
        assert(commitsByTag.first() == commit.generateKey()) { "Tag should have commit ${commit.generateKey()} associated with it but found ${commitsByTag.first()}" }

        // Remove the tag to the commits
        handler.removeTagFromCommit(commit)

        // Then
        val tagHashAfterRemove = TagIndex.getTag(TAG_NAME)
        val commitsToTagByNameAfterRemove = TagIndex.getCommitsByTag(TAG_NAME)

        assertNotNull(tagHashAfterRemove, "Tag should be created")
        assert(commitsToTagByNameAfterRemove.isEmpty()) { "Tag should have no commit associated with it but found ${commitsToTagByNameAfterRemove.size}" }
    }

    @Test
    fun `User without permissions can remove tags from commits`() {
        // Given
        UserCreationHandler(USERNAME_2, PASSWORD_2, null, null, emptyArray(), arrayOf(USERNAME, PASSWORD)).createUser()

        // Given
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))

        commitHandler.preCommit(true)

        commitHandler.canDoCommit()
        commitHandler.processCommit()

        commitHandler.postCommit(listOf(TAG_NAME))

        // Given
        val handler = TagHandler(TAG_NAME, listOf(commit.generateKey()), arrayOf(USERNAME, PASSWORD))

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            fail("User should have permission to assign a tag")
        }

        // Then
        val tagHash = TagIndex.getTag(TAG_NAME)
        val commitsToTagByName = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHash = TagIndex.getCommitsByTag(tagHash!!)
        val commitsByTag = TagIndex.getCommitsByTag(tagHash)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByName, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHash, "Tag should have commits associated with it")
        assert(commitsToTagByName.size == 1) { "Tag should have only one commit associated with it but found ${commitsToTagByName.size}" }
        assert(TagIndex.getTags().size == 1) { "Only one tag should have been created but found ${TagIndex.getTags().size}" }

        assert(commitsByTag.size == 1) { "Tag should have one commit associated with it but found ${commitsByTag.size}" }
        assert(commitsByTag.first() == commit.generateKey()) { "Tag should have commit ${commit.generateKey()} associated with it but found ${commitsByTag.first()}" }

        // Remove the tag to the commits
        val handler2 = TagHandler(TAG_NAME, listOf(commit.generateKey()), arrayOf(USERNAME_2, PASSWORD_2))

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        assertFailsWith<InvalidPermissionException> {
            handler2.checkCanModifyTags()
            handler2.removeTagFromCommit(commit)
        }


        // Then
        val commitsToTagByNameAfterRemove = TagIndex.getCommitsByTag(TAG_NAME)
        val commitsToTagByHashAfterRemove = TagIndex.getCommitsByTag(tagHash)
        val commitsByTagAfterRemove = TagIndex.getCommitsByTag(tagHash)

        assertNotNull(tagHash, "Tag should be created")
        assertNotNull(commitsToTagByNameAfterRemove, "Tag should have commits associated with it")
        assertNotNull(commitsToTagByHashAfterRemove, "Tag should have commits associated with it")
        assert(commitsToTagByNameAfterRemove.size == 1) { "Tag should have only one commit associated with it but found ${commitsToTagByNameAfterRemove.size}" }
        assert(TagIndex.getTags().size == 1) { "Only one tag should have been created but found ${TagIndex.getTags().size}" }

        assert(commitsByTagAfterRemove.size == 1) { "Tag should have one commit associated with it but found ${commitsByTagAfterRemove.size}" }
        assert(commitsByTagAfterRemove.first() == commit.generateKey()) { "Tag should have commit ${commit.generateKey()} associated with it but found ${commitsByTagAfterRemove.first()}" }
    }
}