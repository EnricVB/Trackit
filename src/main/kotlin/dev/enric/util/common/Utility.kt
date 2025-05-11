package dev.enric.util.common

import dev.enric.logger.Logger
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Utility {

    fun getLogDateFormat(format: String): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }

    fun formatDateTime(dateTime: LocalDateTime, format: String): String {
        return dateTime.format(DateTimeFormatter.ofPattern(format))
    }

    fun formatDateTime(dateTime: Timestamp, format: String): String {
        return dateTime.toLocalDateTime().format(DateTimeFormatter.ofPattern(format))
    }

    fun printWaitingBar() {
        val progressThread = Thread {
            var dots = 0
            println()

            while (!Thread.currentThread().isInterrupted) {
                val dotString = ".".repeat(dots + 1)
                Logger.updateLine("$dotString   ")
                Thread.sleep(1000)
                dots = (dots + 1) % 3
            }

            Logger.updateLine("\n")
        }
        progressThread.start()
    }
}