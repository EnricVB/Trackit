package dev.enric.util.common

import picocli.CommandLine.Help.Ansi

object ColorUtil {

    fun title(title : String): String {
        return Ansi.AUTO.string("@|cyan ${title}|@")
    }

    fun label(label : String): String {
        return Ansi.AUTO.string("@|bold,cyan ${label}|@")
    }

    fun text(text : String): String {
        return Ansi.AUTO.string("@|white ${text}|@")
    }

    fun message(message : String): String {
        return Ansi.AUTO.string("@|italic,white ${message}|@")
    }

    fun error(message: String): String {
        return Ansi.AUTO.string("@|bold,red ${message}|@")
    }
}