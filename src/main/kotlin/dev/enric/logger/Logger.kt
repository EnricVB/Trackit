package dev.enric.logger

import dev.enric.util.common.ColorUtil

object Logger {
    var logLevel: LogLevel = LogLevel.INFO

    fun setupLogger(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    fun log(message: String) {
        if(logLevel != LogLevel.INFO) return

        println(ColorUtil.message(message))
    }

    fun error(message: String) {
        if(logLevel == LogLevel.QUIET) return

        System.err.println(ColorUtil.error(message))
    }

    enum class LogLevel {
        INFO,
        ERRORS,
        QUIET
    }
}