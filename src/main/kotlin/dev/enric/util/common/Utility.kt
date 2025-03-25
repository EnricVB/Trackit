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
                INSERT -> diff.append("+ ${ColorUtil.insertLine(it.newLine)}\n")
                DELETE -> diff.append("- ${ColorUtil.deleteLine(it.oldLine)}\n")
                CHANGE -> diff.append("- ${it.oldLine}\n+ ${it.newLine}\n")
                else -> diff.append("  ${it.oldLine}\n")
            }
        }

        return diff.toString()
    }
}