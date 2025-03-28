package dev.enric.command.repository

import dev.enric.command.CommandTest
import dev.enric.core.handler.repo.ignore.IgnoreHandler
import dev.enric.core.handler.repo.init.InitHandler
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.repository.RepositoryFolderManager
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IgnoreCommandTest : CommandTest() {

    companion object {
        // Given
        const val SPECIFIC_FILE_PATH = "index.html"

        const val FOLDER_PATH = "src"
        const val FILE_INSIDE_FOLDER = "file.txt"
        const val FILE_2_INSIDE_FOLDER = "file2.txt"

        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"
    }

    @Before
    fun createRepository() {
        SystemConsoleInput.setInput("$USERNAME\n$MAIL\n$PHONE\n$PASSWORD\n$PASSWORD\n")

        InitHandler.init()
    }

    @Test
    fun `Ignore command adds specific file to the ignore list`() {
        Logger.log("Executing test: Ignore command adds specific file to the ignore list\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(SPECIFIC_FILE_PATH)
        path.toFile().createNewFile()

        // When
        IgnoreHandler().ignore(path)

        // Then
        // Check the file is added to the ignore list
        assertEquals(2, IgnoreHandler().getIgnoredFiles().size)
        assertTrue(IgnoreHandler().isIgnored(path))
    }

    @Test
    fun `Ignore command does not adds specific file to the ignore list if does not exists`() {
        Logger.log("Executing test: Ignore command adds specific file to the ignore list\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(SPECIFIC_FILE_PATH)

        // When
        assertFailsWith<IllegalStateException> { IgnoreHandler().ignore(path) }

        // Then
        // Check the file is not added to the ignore list
        assertEquals(1, IgnoreHandler().getIgnoredFiles().size)
        assertFalse (IgnoreHandler().isIgnored(path))
    }

    @Test
    fun `Ignore commands adds all files inside folder to the ignore list`() {
        Logger.log("Executing test: Ignore commands adds all files inside folder to the ignore list\n")

        // Given
        val folderPath = RepositoryFolderManager().getInitFolderPath().resolve(FOLDER_PATH)
        val file1 = folderPath.resolve(FILE_INSIDE_FOLDER)
        val file2 = folderPath.resolve(FILE_2_INSIDE_FOLDER)

        folderPath.toFile().mkdir()
        file1.toFile().createNewFile()
        file2.toFile().createNewFile()

        // When
        IgnoreHandler().ignore(folderPath)

        // Then
        // Check the files are added to the ignore list
        IgnoreHandler().getIgnoredFiles().forEach(System.out::println)
        assertEquals(2, IgnoreHandler().getIgnoredFiles().size)

        assertTrue(IgnoreHandler().isIgnored(file1))
        assertTrue(IgnoreHandler().isIgnored(file2))
    }
}