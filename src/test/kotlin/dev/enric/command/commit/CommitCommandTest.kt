package dev.enric.command.commit

import dev.enric.command.CommandTest
import dev.enric.core.handler.management.users.UserCreationHandler
import dev.enric.core.handler.repo.commit.CommitHandler
import dev.enric.core.handler.repo.init.InitHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.User
import dev.enric.exceptions.IllegalStateException
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.index.CommitIndex
import dev.enric.util.index.TagIndex
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class CommitCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"

        const val USERNAME_2 = "test2"
        const val PASSWORD_2 = "password2"

        const val COMMIT_TITLE = "Test commit"
        const val COMMIT_MESSAGE = "This is a test commit"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$MAIL\n$PHONE\n$PASSWORD\n$PASSWORD\n")
        InitHandler.init()
    }

    @Test
    fun `Commit with no staged files fails`() {
        Logger.info("Executing test: Commit with no staged files fails\n")

        // Given
        StagingHandler.clearStagingArea()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(false)

        // Then
        assertFailsWith<IllegalStateException> { commitHandler.canDoCommit() }
        assertNull(CommitIndex.getCurrentCommit())
    }

    @Test
    fun `Clears staging area after commit`() {
        Logger.info("Executing test: Clears staging area after commit\n")

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(false)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Then
        val indexCommit = CommitIndex.getCurrentCommit()!!

        assertTrue { StagingHandler.getStagedFiles().isEmpty() }

        // Assert that the commit is not null and has the expected properties
        assertNotNull(indexCommit)
        assertNull(indexCommit.previousCommit)
        assertEquals(COMMIT_TITLE, indexCommit.title)
        assertEquals(COMMIT_MESSAGE, indexCommit.message)

        // Assert that the commit author and confirmer are the same
        assertEquals(USERNAME, User.newInstance(indexCommit.author).name)
        assertEquals(MAIL, User.newInstance(indexCommit.author).mail)
        assertEquals(PHONE, User.newInstance(indexCommit.author).phone)

        assertEquals(USERNAME, User.newInstance(indexCommit.confirmer).name)
        assertEquals(MAIL, User.newInstance(indexCommit.confirmer).mail)
        assertEquals(PHONE, User.newInstance(indexCommit.confirmer).phone)
    }

    @Test
    fun `User with no branch write permissions cannot commit`() {
        Logger.info("Executing test: User with no branch write permissions cannot commit\n")

        // Given
        StagingHandler.clearStagingArea()
        UserCreationHandler(USERNAME_2, PASSWORD_2, null, null, emptyArray(), arrayOf(USERNAME, PASSWORD)).createUser()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME_2, PASSWORD_2), arrayOf(USERNAME_2, PASSWORD_2))
        commitHandler.preCommit(true)

        // Then
        assertFailsWith<InvalidPermissionException> { commitHandler.canDoCommit() }
        assertFalse { StagingHandler.getStagedFiles().isEmpty() }
        assertNull(CommitIndex.getCurrentCommit())
    }

    @Test
    fun `User with no branch write permissions but different confirmer can commit`() {
        Logger.info("Executing test: User with no branch write permissions but different confirmer can commit\n")

        // Given
        StagingHandler.clearStagingArea()
        UserCreationHandler(USERNAME_2, PASSWORD_2, null, null, emptyArray(), arrayOf(USERNAME, PASSWORD)).createUser()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME_2, PASSWORD_2), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Then
        val indexCommit = CommitIndex.getCurrentCommit()!!

        assertTrue { StagingHandler.getStagedFiles().isEmpty() }

        // Assert that the commit is not null and has the expected properties
        assertNotNull(indexCommit)
        assertNull(indexCommit.previousCommit)
        assertEquals(COMMIT_TITLE, indexCommit.title)
        assertEquals(COMMIT_MESSAGE, indexCommit.message)

        // Assert that user 2 is the author and user 1 is the confirmer
        assertEquals(USERNAME_2, User.newInstance(indexCommit.author).name)
        assertEquals(USERNAME, User.newInstance(indexCommit.confirmer).name)
    }

    @Test
    fun `Adding tags to commits`() {
        Logger.info("Executing test: Adding tags to commits\n")

        // Given
        StagingHandler.clearStagingArea()
        val tag = "v1.0"
        val tag2 = "Beta"

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf(tag, tag2))

        // Then
        val indexCommit = CommitIndex.getCurrentCommit()!!

        assertTrue { StagingHandler.getStagedFiles().isEmpty() }

        // Assert that the commit is not null and has the expected properties
        assertNotNull(indexCommit)
        assertNull(indexCommit.previousCommit)
        assertEquals(COMMIT_TITLE, indexCommit.title)
        assertEquals(COMMIT_MESSAGE, indexCommit.message)

        // Assert that user 1 is the author and confirmer
        assertEquals(USERNAME, User.newInstance(indexCommit.author).name)
        assertEquals(USERNAME, User.newInstance(indexCommit.confirmer).name)

        // Assert that the commit has the expected tag
        assertContains(TagIndex.getCommitsByTag(tag), CommitIndex.getCurrentCommit()?.generateKey())
        assertContains(TagIndex.getCommitsByTag(tag2), CommitIndex.getCurrentCommit()?.generateKey())
    }
}