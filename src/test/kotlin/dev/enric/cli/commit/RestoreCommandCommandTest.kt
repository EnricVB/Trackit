package dev.enric.cli.commit

import dev.enric.cli.CommandTest
import dev.enric.core.handler.management.UserCreationHandler
import dev.enric.core.handler.repo.CommitHandler
import dev.enric.core.handler.repo.RestoreHandler
import dev.enric.core.handler.repo.InitHandler
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.InvalidPermissionException
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.index.CommitIndex
import dev.enric.util.repository.RepositoryFolderManager
import org.junit.Before
import org.junit.Test
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RestoreCommandCommandTest : CommandTest() {

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

        const val FILE = "test.txt"
        const val FILE_TEXT_V1 = "This is a test file"
        const val FILE_TEXT_V2 = "This is a test file v2\nIt has been modified"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$PASSWORD\n$PASSWORD\n\n$MAIL\n$PHONE")
        InitHandler().init()
    }

    @Test
    fun `Files are modified to a previous state`() {
        Logger.info("Executing test: Files are modified to a previous state\n")

        // Given
        val file = RepositoryFolderManager().getInitFolderPath().resolve(FILE)
        file.writeText(FILE_TEXT_V1)

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(
            arrayOf(USERNAME, PASSWORD), arrayOf(
                USERNAME,
                PASSWORD
            )
        )
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Simulate a file modification
        file.writeText(FILE_TEXT_V2)

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())

        // When
        val restoreHandler = RestoreHandler(commit, file, arrayOf(USERNAME, PASSWORD))
        restoreHandler.canRestore()
        restoreHandler.restore()

        // Then
        assertEquals(FILE_TEXT_V1, file.readText())
        assertEquals(CommitIndex.getCurrentCommit()!!, commit)
    }

    @Test
    fun `Files are not modified if user has no read permissions`() {
        Logger.info("Executing test: Files are not modified if user has no read permissions\n")

        // Given
        val file = RepositoryFolderManager().getInitFolderPath().resolve(FILE)
        file.writeText(FILE_TEXT_V1)

        UserCreationHandler(
            USERNAME_2,
            PASSWORD_2,
            null,
            null,
            emptyArray(),
            arrayOf(
                USERNAME,
                PASSWORD
            )
        ).createUser()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(
            arrayOf(USERNAME, PASSWORD), arrayOf(
                USERNAME,
                PASSWORD
            )
        )
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Simulate a file modification
        file.writeText(FILE_TEXT_V2)

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())

        // When
        val restoreHandler = RestoreHandler(commit, file, arrayOf(USERNAME_2, PASSWORD_2))

        assertFailsWith<InvalidPermissionException> {
            restoreHandler.canRestore()
            restoreHandler.restore()
        }

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())
    }

    @Test
    fun `Files are not modified if they are not in the commit`() {
        Logger.info("Executing test: Files are not modified if they are not in the commit\n")

        // Given
        val file = RepositoryFolderManager().getInitFolderPath().resolve(FILE)
        file.writeText(FILE_TEXT_V1)

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(
            arrayOf(USERNAME, PASSWORD), arrayOf(
                USERNAME,
                PASSWORD
            )
        )
        commitHandler.preCommit(true)
        StagingHandler().unstage(file)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Then
        val restoreHandler = RestoreHandler(commit, file, arrayOf(USERNAME, PASSWORD))

        restoreHandler.canRestore()
        restoreHandler.restore()

        // Then
        assertEquals(FILE_TEXT_V1, file.readText())
    }
}