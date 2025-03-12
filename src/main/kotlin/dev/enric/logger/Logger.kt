package dev.enric.logger

import dev.enric.util.common.ColorUtil
import dev.enric.util.common.Utility
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Logger class to log messages to the console
 * @property logLevel The log level to use
 *
 * @constructor Creates a Logger with the given log level
 */
object Logger {
    val repositoryFolderManager : RepositoryFolderManager = RepositoryFolderManager()
    val commandLogFile = repositoryFolderManager.getCommandLogsFilePath()

    var logLevel: LogLevel = LogLevel.INFO
    var clazz: String = ""

    /**
     * Sets up the logger with the given log level
     * @param logLevel The log level to use
     */
    fun setupLogger(logLevel: LogLevel, clazz: String) {
        this.logLevel = logLevel
        this.clazz = clazz
    }

    /**
     * Logs a message to the console.
     * In case the log level is set to QUIET or ERRORS, the message will not be printed.
     *
     * @param message The message to log
     */
    fun log(message: String) {
        saveLog("[${getDateTime()}] [INFO] [$clazz] $message")
        if(logLevel != LogLevel.INFO) return

        println(ColorUtil.message(message))
    }

    /**
     * Logs an error message to the console.
     * In case the log level is set to QUIET, the message will not be printed.
     *
     * @param message The error message to log
     */
    fun error(message: String) {
        saveLog("[${getDateTime()}] [ERROR] [$clazz] $message")
        if(logLevel == LogLevel.QUIET) return

        System.err.println(ColorUtil.error(message))
    }


    fun trace(message: String) {
        saveLog("[${getDateTime()}] [TRACE] [$clazz] $message")
    }

    /**
     * Saves a log message to a file to store all steps of the application.
     *
     * In case the file does not exist, it will be created. This may happen if it is a new day, as the logs are stored daily.
     *
     * @param message The message to save
     */
    fun saveLog(message: String) {
        if (!commandLogFile.toFile().exists()) {
            commandLogFile.parent.toFile().mkdirs()
            Files.createFile(commandLogFile)
        }

        Files.writeString(commandLogFile, message + "\n", Charsets.UTF_8, StandardOpenOption.APPEND)
    }

    fun getDateTime(): String {
        return Utility.getLogDateFormat("yyyy-MM-dd HH:mm:ss")
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