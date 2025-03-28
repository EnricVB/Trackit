package dev.enric.util.common.console

open class ConsoleInput {
    companion object {
        var isTesting: Boolean = false
    }

    open fun readLine(prompt: String): String = ""
    open fun readPassword(prompt: String): CharArray = charArrayOf()
}