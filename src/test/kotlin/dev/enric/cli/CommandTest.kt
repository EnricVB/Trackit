package dev.enric.cli

import dev.enric.util.common.console.ConsoleInput.Companion.isTesting
import org.junit.After
import org.junit.Before
import java.nio.file.Files
import java.nio.file.Path

open class CommandTest {
    protected lateinit var tempDir: Path
    protected lateinit var originalDir: String

    @Before
    fun setUp() {
        originalDir = System.getProperty("user.dir")
        tempDir = Files.createTempDirectory("temp")
        isTesting = true

        System.setProperty("user.dir", tempDir.toAbsolutePath().toString())
    }

    @After
    fun tearDown() {
        isTesting = false
        System.setIn(System.`in`)

        System.setProperty("user.dir", originalDir)
        tempDir.toFile().deleteRecursively()
    }
}