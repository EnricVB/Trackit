package dev.enric.util.common

import com.github.difflib.text.DiffRow.Tag.CHANGE
import com.github.difflib.text.DiffRow.Tag.EQUAL
import com.github.difflib.text.DiffRowGenerator
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Utility {

    fun getLogDateFormat(format: String): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }

    fun fileConflicts(text1: String, text2: String): String {
        val result = StringBuilder()
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .reportLinesUnchanged(true)
            .mergeOriginalRevised(false)
            .inlineDiffByWord(false)
            .replaceOriginalLinefeedInChangesWithSpaces(true)
            .build()

        val rows = generator.generateDiffRows(text1.split("\n"), text2.split("\n"))

        val rowTypes = rows
            .associate { row ->
                val rowIndex = rows.indexOf(row)
                val lineType = if(row.tag != EQUAL) CHANGE else EQUAL

                val data = Triple(lineType, row.oldLine, row.newLine)
                rowIndex to data
            }.toSortedMap()

        // Create a new StringBuilder to store the result
        var insideConflict = false
        val oldBlock = mutableListOf<String>()
        val newBlock = mutableListOf<String>()

        // Iterate through the sorted map and build the result
        // If we encounter a conflict, we store the lines in oldBlock and newBlock
        // If we encounter an equal line, we append the blocks to the result and reset the blocks
        rowTypes.forEach { (_, data) ->
            val (lineType, oldLine, newLine) = data

            if (lineType == EQUAL) {
                if (insideConflict) {
                    result.appendLine("<<<<<< OLD")
                    oldBlock.forEach { result.appendLine(it) }
                    result.appendLine("======")
                    newBlock.forEach { result.appendLine(it) }
                    result.appendLine(">>>>>> NEW")

                    oldBlock.clear()
                    newBlock.clear()
                }

                result.appendLine(newLine)
                insideConflict = false
            } else {
                oldBlock.add(oldLine)
                newBlock.add(newLine)
                insideConflict = true
            }
        }

        // If we finish with a conflict, we need to append the remaining blocks
        if (insideConflict) {
            result.appendLine("<<<<<< OLD")
            oldBlock.forEach { result.appendLine(it) }
            result.appendLine("======")
            newBlock.forEach { result.appendLine(it) }
            result.appendLine(">>>>>> NEW")
        }

        return result.toString()
    }

    fun fileConflict(text1: String, text2: String): String {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .reportLinesUnchanged(true)
            .mergeOriginalRevised(false)
            .inlineDiffByWord(false)
            .build()

        val rows: List<DiffRow> = generator.generateDiffRows(text1.split("\n"), text2.split("\n"))
        val diff = StringBuilder()

        rows.forEach {
            if(it.tag == CHANGE) {
                diff.appendLine("<<<<<<<< ORIGINAL")
                diff.appendLine(it.oldLine)

                diff.appendLine("========")

                diff.appendLine(it.newLine)
                diff.appendLine(">>>>>>>> REVISED")
            } else {
                diff.appendLine(it.oldLine)
            }
        }

        return diff.toString()
    }

    fun formatDateTime(dateTime: LocalDateTime, format: String): String {
        return dateTime.format(DateTimeFormatter.ofPattern(format))
    }

    fun formatDateTime(dateTime: Timestamp, format: String): String {
        return dateTime.toLocalDateTime().format(DateTimeFormatter.ofPattern(format))
    }
}