package dev.enric.util.common

import picocli.CommandLine.Help.Ansi

/**
 * Utility class to colorize text in the console
 */
object ColorUtil {

    fun title(title : String): String {
        return Ansi.ON.string("@|cyan ${title}|@")
    }

    fun label(label : String): String {
        return Ansi.ON.string("@|bold,cyan ${label}|@")
    }

    fun text(text : String): String {
        return Ansi.ON.string("@|white ${text}|@")
    }

    fun message(message : String): String {
        return Ansi.ON.string("@|italic,white ${message}|@")
    }

    fun error(message: String): String {
        return Ansi.ON.string("@|bold,red ${message}|@")
    }

    fun warning(message: String): Any {
        return Ansi.ON.string("@|yellow $message|@")
    }

    fun insertLine(text: String): String {
        return Ansi.ON.string("@|bold,green $text|@")
    }

    fun deleteLine(text: String): String {
        return Ansi.ON.string("@|bold,red $text|@")
    }
}