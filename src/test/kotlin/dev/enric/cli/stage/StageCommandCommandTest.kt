package dev.enric.cli.stage

import dev.enric.cli.CommandTest
import dev.enric.cli.repo.StageCommand
import dev.enric.cli.repo.UnstageCommand
import dev.enric.core.handler.repo.InitHandler
import dev.enric.core.handler.repo.StagingHandler
import dev.enric.domain.objects.Content
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.repository.RepositoryFolderManager
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageCommandCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"

        const val FILE_PATH = "file.txt"
        const val FILE_2_PATH = "file2.txt"

        const val FOLDER = "folder"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$PASSWORD\n$PASSWORD\n\n$MAIL\n$PHONE")
        InitHandler().init()
    }

    @Test
    fun `Stage command adds file to the stage`() {
        Logger.info("Executing test: Stage command adds file to the stage\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(FILE_PATH)
        path.toFile().createNewFile()
        Files.writeString(path, "content")

        // When
        StageCommand().stageFile(path)

        // Then
        val relativizedPath = RepositoryFolderManager().getInitFolderPath().relativize(path)

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath } != null }
        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath }!!.first == Content(Files.readAllBytes(path)).generateKey() }
        assertFalse { StagingHandler.getStagedFiles().find { it.second == relativizedPath }!!.first == Content("".toByteArray()).generateKey() }
        assertEquals(2, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Stage command adds files from folder to the stage`() {
        Logger.info("Executing test: Stage command adds files from folder to the stage\n")

        // Given
        val folder = RepositoryFolderManager().getInitFolderPath().resolve(FOLDER)
        val file1 = folder.resolve(FILE_PATH)
        val file2 = folder.resolve(FILE_2_PATH)

        folder.toFile().mkdir()
        file1.toFile().createNewFile()
        file2.toFile().createNewFile()

        Files.writeString(file1, "content1")
        Files.writeString(file2, "content2")

        // When
        StageCommand().stageFolder(folder)

        // Then
        val relativizedPath1 = RepositoryFolderManager().getInitFolderPath().relativize(file1)
        val relativizedPath2 = RepositoryFolderManager().getInitFolderPath().relativize(file2)

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath1 } != null }
        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath1 }!!.first == Content(Files.readAllBytes(file1)).generateKey() }
        assertFalse { StagingHandler.getStagedFiles().find { it.second == relativizedPath1 }!!.first == Content("A".toByteArray()).generateKey() }

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath2 } != null }
        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath2 }!!.first == Content(Files.readAllBytes(file2)).generateKey() }
        assertFalse { StagingHandler.getStagedFiles().find { it.second == relativizedPath2 }!!.first == Content("A".toByteArray()).generateKey() }

        assertEquals(3, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Stage command does not adds file to the stage that does not exists`() {
        Logger.info("Executing test: Stage command does not adds file to the stage that does not exists\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(FILE_PATH)

        // When
        assertFailsWith<IllegalStateException> { StageCommand().stageFile(path) }

        // Then
        val relativizedPath = RepositoryFolderManager().getInitFolderPath().relativize(path)

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath } == null }
        assertEquals(1, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Stage command does not stage ignored files`() {
        Logger.info("Executing test: Stage command does not stage ignored files\n")

        // Given
        val path = RepositoryFolderManager().getSecretKeyPath()

        // When
        StageCommand().stageFile(path)

        // Then
        assertEquals(1, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Stage command does stage ignored files if forced`() {
        Logger.info("Executing test: Stage command does stage ignored files if forced\n")

        // Given
        val path = RepositoryFolderManager().getSecretKeyPath()

        // When
        val stage = StageCommand()
        stage.force = true

        stage.stageFile(path)

        // Then
        assertEquals(2, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Unstage command does only unstage specified file`() {
        Logger.info("Executing test: Unstage command does only unstage specified file\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(FILE_PATH)
        path.toFile().createNewFile()
        Files.writeString(path, "content")

        StageCommand().stageFile(path)

        // When
        UnstageCommand().unstageFile(path)

        // Then
        val relativizedPath = RepositoryFolderManager().getInitFolderPath().relativize(path)

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath } == null }
        assertEquals(1, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Unstage command does unstage everything under folder`() {
        Logger.info("Executing test: Unstage command does unstage everything under folder\n")

        // Given
        val folder = RepositoryFolderManager().getInitFolderPath().resolve(FOLDER)
        val file1 = folder.resolve(FILE_PATH)
        val file2 = folder.resolve(FILE_2_PATH)

        folder.toFile().mkdir()
        file1.toFile().createNewFile()
        file2.toFile().createNewFile()

        Files.writeString(file1, "content1")
        Files.writeString(file2, "content2")

        StageCommand().stageFolder(folder)

        // When
        UnstageCommand().unstageFolder(folder)

        // Then
        val relativizedPath1 = RepositoryFolderManager().getInitFolderPath().relativize(file1)
        val relativizedPath2 = RepositoryFolderManager().getInitFolderPath().relativize(file2)

        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath1 } == null }
        assertTrue { StagingHandler.getStagedFiles().find { it.second == relativizedPath2 } == null }
        assertEquals(1, StagingHandler.getStagedFiles().size)
    }

    @Test
    fun `Unstage command does nothing if file does not exist`() {
        Logger.info("Executing test: Unstage command does nothing if file does not exist\n")

        // Given
        val path = RepositoryFolderManager().getInitFolderPath().resolve(FILE_PATH)

        // When
        UnstageCommand().unstageFile(path)

        // Then
        assertEquals(1, StagingHandler.getStagedFiles().size) //
    }
}