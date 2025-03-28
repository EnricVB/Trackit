package dev.enric.command.commit

import dev.enric.command.CommandTest
import dev.enric.core.handler.repo.commit.CommitHandler
import dev.enric.core.handler.repo.init.InitHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommitCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"

        const val FILE_PATH = "file.txt"

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
        Logger.log("Executing test: Commit with no staged files fails\n")

        // Given
        StagingHandler.clearStagingArea()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(false)

        assertFailsWith<IllegalStateException> { commitHandler.canDoCommit() }
    }

    @Test
    fun `Clears staging area after commit`() {
        Logger.log("Executing test: Clears staging area after commit\n")

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(false)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit("")

        // Then
        assertTrue { StagingHandler.getStagedFiles().isEmpty() }
    }
}