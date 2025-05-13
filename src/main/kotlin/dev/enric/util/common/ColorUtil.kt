package dev.enric.util.common

import picocli.CommandLine.Help.Ansi
import kotlin.math.absoluteValue

/**
 * Utility class to colorize text in the console
 */
object ColorUtil {
    private val colorPool = listOf(
        "bold,blue", "bold,green", "bold,magenta", "bold,cyan",
        "bold,yellow", "bold,white", "bold,red"
    )

    private val hashColorMap = mutableMapOf<String, String>()

    fun hashColor(hash: String): String {
        return hashColorMap.getOrPut(hash) {
            val index = (hash.hashCode().absoluteValue) % colorPool.size
            "@|${colorPool[index]} $hash|@"
        }.let { Ansi.ON.string(it) }
    }

    fun title(title: String): String {
        return Ansi.ON.string("@|cyan ${title}|@")
    }

    fun label(label: String): String {
        return Ansi.ON.string("@|bold,cyan ${label}|@")
    }

    fun text(text: String): String {
        return Ansi.ON.string("@|white ${text}|@")
    }

    fun message(message: String): String {
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