package dev.enric.util.common

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRow.Tag.*
import com.github.difflib.text.DiffRowGenerator
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.Content
import dev.enric.domain.objects.Tree
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object Utility {

    fun getLogDateFormat(format: String): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }

    fun fileDiff(text1: String, text2: String): String {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .ignoreWhiteSpaces(true)
            .reportLinesUnchanged(true)
            .mergeOriginalRevised(false)
            .inlineDiffByWord(false)
            .replaceOriginalLinefeedInChangesWithSpaces(true)
            .build()

        val rows: List<DiffRow> = generator.generateDiffRows(text1.split("\n"), text2.split("\n"))
        val diff = StringBuilder()

        rows.forEach {
            when (it.tag ?: EQUAL) {
                INSERT -> diff.append("+ ${ColorUtil.insertLine(it.newLine)}\n")
                DELETE -> diff.append("- ${ColorUtil.deleteLine(it.oldLine)}\n")
                CHANGE -> diff.append("- ${it.oldLine}\n+ ${it.newLine}\n")
                else -> diff.append("  ${it.oldLine}\n")
            }
        }

        return diff.toString()
    }

    fun blame(file: File, commit: Commit): String {
        val originalString = Files.readAllLines(file.toPath())
        val blameString = StringBuilder()

        var currentCommit: Commit? = commit

        while (currentCommit != null) {
            val previousCommit = currentCommit.previousCommit
            val commitString = currentCommit.tree.find { treeHash ->
                val tree = treeHash.let { Tree.newInstance(it) }

                tree.serializablePath.toPath() == file.toPath()
            }.let { treeHash ->
                val tree = treeHash?.let { Tree.newInstance(treeHash) }
                val content = tree?.content?.let { Content.newInstance(it) } ?: return@let "".lines()

                return@let String(content.content).lines()
            }

            for (currentLineIndex in 0..originalString.size) {
                if (originalString[currentLineIndex] == commitString[currentLineIndex]) {
                    blameString.appendLine("${currentCommit.author} ${currentCommit.date} ${commitString[currentLineIndex]}")
                }
            }

            currentCommit = previousCommit?.let { Commit.newInstance(it) }
        }

        return blameString.toString()
    }
}