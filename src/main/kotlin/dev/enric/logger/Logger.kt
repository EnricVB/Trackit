package dev.enric.logger

object Logger {
    var logLevel: LogLevel = LogLevel.INFO

    fun setupLogger(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    fun log(message: String) {
        if(logLevel != LogLevel.INFO) return

        println(message)
    }

    fun error(message: String) {
        if(logLevel == LogLevel.QUIET) return

        System.err.println(message)
    }


    enum class LogLevel {
        INFO,
        ERRORS,
        QUIET
    }
}