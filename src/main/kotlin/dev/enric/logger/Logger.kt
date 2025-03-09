package dev.enric.logger

import dev.enric.util.common.ColorUtil

/**
 * Logger class to log messages to the console
 * @property logLevel The log level to use
 *
 * @constructor Creates a Logger with the given log level
 */
object Logger {
    var logLevel: LogLevel = LogLevel.INFO

    /**
     * Sets up the logger with the given log level
     * @param logLevel The log level to use
     */
    fun setupLogger(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    /**
     * Logs a message to the console.
     * In case the log level is set to QUIET or ERRORS, the message will not be printed.
     */
    fun log(message: String) {
        if(logLevel != LogLevel.INFO) return

        println(ColorUtil.message(message))
    }

    /**
     * Logs an error message to the console.
     * In case the log level is set to QUIET, the message will not be printed.
     */
    fun error(message: String) {
        if(logLevel == LogLevel.QUIET) return

        System.err.println(ColorUtil.error(message))
    }

    /**
     * The log levels available
     */
    enum class LogLevel {
        INFO,
        ERRORS,
        QUIET
    }
}