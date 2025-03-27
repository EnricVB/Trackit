package dev.enric.util.common

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRow.Tag.*
import com.github.difflib.text.DiffRowGenerator
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
                INSERT -> diff.appendLine("+ ${ColorUtil.insertLine(it.newLine)}")
                DELETE -> diff.appendLine("- ${ColorUtil.deleteLine(it.oldLine)}")
                CHANGE -> diff.appendLine("""
                    - ${if(it.oldLine.isNotBlank()) ColorUtil.deleteLine(it.oldLine) else ""}
                    + ${if(it.newLine.isNotBlank()) ColorUtil.deleteLine(it.oldLine) else ""}"""
                    .trimIndent())
                else -> diff.appendLine("  ${it.oldLine}")
            }
        }

        return diff.toString()
    }
}