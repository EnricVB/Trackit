package dev.enric.command.stage

import dev.enric.command.CommandTest
import dev.enric.command.repo.commit.Commit
import dev.enric.core.handler.repo.ignore.IgnoreHandler
import dev.enric.core.handler.repo.init.InitHandler
import dev.enric.core.handler.repo.staging.StagingHandler
import dev.enric.core.handler.repo.staging.StatusHandler
import dev.enric.util.common.FileStatus
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.repository.RepositoryFolderManager
import org.junit.Before
import org.junit.Test
import kotlin.io.path.createFile
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StatusCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"

        const val FILE_PATH = "file.txt"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$MAIL\n$PHONE\n$PASSWORD\n$PASSWORD\n")
        InitHandler.init()
    }

    private fun createAndWriteFile(content: String = "Hello world!") =
        RepositoryFolderManager().getInitFolderPath().resolve(FILE_PATH).createFile().toFile().apply {
            writeText(content)
        }

    private fun commitFile() {
        Commit().apply {
            title = "Initial commit"
            message = "Initial commit message"
            stageAllFiles = true
            sudoArgs = arrayOf(USERNAME, PASSWORD)
            confirmerArgs = arrayOf(USERNAME, PASSWORD)
        }.call()
    }

    @Test
    fun `File status must not include trackit directory`() {
        // When
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertFalse(files.any { it.path.toString().contains(".trackit") })
        assertEquals(2, files.size)
    }

    @Test
    fun `File non added to repository must be UNTRACKED`() {
        // Given
        val file = createAndWriteFile()

        // When
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.UNTRACKED, FileStatus.getStatus(file))
    }

    @Test
    fun `File added to repository on previous commit and without changes must be UNMODIFIED`() {
        // Given
        val file = createAndWriteFile()

        // When
        // Create a commit with the staged file
        commitFile()

        // Obtain the status of the files
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.UNMODIFIED, FileStatus.getStatus(file))
    }

    @Test
    fun `File added to repository on previous commit and with changes must be MODIFIED`() {
        // Given
        val file = createAndWriteFile()

        // When
        // Create a commit with the staged file
        commitFile()

        // Modify the file
        file.writeText("Hello world! Modified")

        // Obtain the status of the files
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.MODIFIED, FileStatus.getStatus(file))
    }

    @Test
    fun `File added to staging area, no matter if committed, must be STAGED`() {
        // Given
        val file = createAndWriteFile()

        // When
        StagingHandler().stage(file.toPath())

        // Obtain the status of the files
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.STAGED, FileStatus.getStatus(file))
    }

    @Test
    fun `File added to repository on previous commit and deleted must be DELETED`() {
        // Given
        val file = createAndWriteFile()

        // When
        // Create a commit with the staged file
        commitFile()

        // Delete the file
        file.delete()

        // Obtain the status of the files
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.DELETE, FileStatus.getStatus(file))
    }

    @Test
    fun `File added to ignore must be IGNORED`() {
        // Given
        val file = createAndWriteFile()

        // When
        IgnoreHandler().ignore(file.toPath())

        // Obtain the status of the files
        val files = StatusHandler.getFilesStatus().flatMap { it.value }

        // Then
        assertContains(files, file)
        assertEquals(FileStatus.IGNORED, FileStatus.getStatus(file))
    }
}