package dev.enric.core.objects

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContentTest {

    @Test
    fun `Generate key with same content should return different hash starting with cd`() {
        val content = Content("This is a test")

        val hash1 = content.generateKey()
        val hash2 = content.generateKey()

        assertNotEquals(hash1, hash2, "Hashes should not be the same because they were generated at different times")
        assertTrue(
            hash1.toString().startsWith("cd") && hash2.toString().startsWith("cd"),
            "Both hashes should start with 'cd' as they are Content hashes"
        )
        assertTrue(
            hash1.toString().length == 32 && hash2.toString().length == 32,
            "Both hashes should have a length of 32 characters, or 16 bytes"
        )
    }

    @Test
    fun `Compress and decompress same Content should return the initial message`() {
        val initialMessage = "This is a test"
        val content = Content(initialMessage)

        val compressedContent = content.compressContent()
        val decompressedContent = content.decompressContent(compressedContent)

        assertEquals(
            initialMessage,
            String(decompressedContent!!),
            "Initial message should be the same as the decompressed message"
        )
    }

    @Test
    fun `Compress and decompress different Content should return different messages`() {
        val initialMessage = "This is a test"
        val content = Content(initialMessage)

        val compressedContent = content.compressContent()
        val decompressedContent = content.decompressContent(compressedContent)

        val differentMessage = "This is a different test"
        val differentContent = Content(differentMessage)

        val differentCompressedContent = differentContent.compressContent()
        val differentDecompressedContent = differentContent.decompressContent(differentCompressedContent)

        assertNotEquals(
            decompressedContent,
            differentDecompressedContent,
            "Initial message should be different from the different message"
        )
    }
}