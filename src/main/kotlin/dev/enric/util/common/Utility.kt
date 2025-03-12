package dev.enric.util.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Utility {

    fun getLogDateFormat(format: String) : String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }
}