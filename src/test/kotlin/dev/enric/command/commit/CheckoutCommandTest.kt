package dev.enric.command.commit

import dev.enric.command.CommandTest
import dev.enric.core.handler.management.users.UserCreationHandler
import dev.enric.core.handler.repo.commit.CheckoutHandler
import dev.enric.core.handler.repo.commit.CommitHandler
import dev.enric.core.handler.repo.init.InitHandler
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

class CheckoutCommandTest : CommandTest() {

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
        SystemConsoleInput.setInput("$USERNAME\n$MAIL\n$PHONE\n$PASSWORD\n$PASSWORD\n")
        InitHandler.init()
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

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())

        // Simulate a file modification
        file.writeText(FILE_TEXT_V2)

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())

        // When
        val checkoutHandler = CheckoutHandler(commit, arrayOf(USERNAME, PASSWORD))
        checkoutHandler.preCheckout()
        checkoutHandler.checkout()

        // Then
        assertEquals(FILE_TEXT_V1, file.readText())
        assertEquals(CommitIndex.getCurrentCommit()!!, commit)
    }

    @Test
    fun `Files are not modified to a previous state if user does not have read permissions`() {
        // Given
        val file = RepositoryFolderManager().getInitFolderPath().resolve(FILE)
        file.writeText(FILE_TEXT_V1)

        UserCreationHandler(
            USERNAME_2, PASSWORD_2, null, null, emptyArray(), arrayOf(
                USERNAME, PASSWORD
            )
        ).createUser()

        // When
        val commit = Commit(title = COMMIT_TITLE, message = COMMIT_MESSAGE)
        val commitHandler = CommitHandler(commit)

        commitHandler.initializeCommitProperties(arrayOf(USERNAME, PASSWORD), arrayOf(USERNAME, PASSWORD))
        commitHandler.preCommit(true)

        commitHandler.canDoCommit()

        commitHandler.processCommit()
        commitHandler.postCommit(listOf())


        // Simulate a file modification
        file.writeText(FILE_TEXT_V2)

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())

        // When
        val checkoutHandler = CheckoutHandler(commit, arrayOf(USERNAME_2, PASSWORD_2))
        assertFailsWith<InvalidPermissionException> { checkoutHandler.canDoCheckout() }

        // Then
        assertEquals(FILE_TEXT_V2, file.readText())
        assertEquals(CommitIndex.getCurrentCommit()!!, commit)
    }
}