package dev.enric.util.common.console

import dev.enric.logger.Logger
import java.io.Console
import java.util.Scanner

class SystemConsoleInput : ConsoleInput() {
    companion object {
        val instance = SystemConsoleInput()
        val console: Console? = System.console()
        val scanner: Scanner = Scanner(System.`in`)

        fun getInstance(): ConsoleInput {
            return instance
        }
    }

    override fun readLine(prompt: String): String {
        if (console == null || isTesting) {
            val line = scanner.nextLine()
            Logger.log(prompt)

            if (isTesting) println(line)
            return line
        }

        return console.readLine(prompt)
    }

    override fun readPassword(prompt: String): CharArray {
        if (console == null || isTesting) {
            val line = scanner.nextLine()
            Logger.log(prompt)

            if (isTesting) println(line)
            return line.toCharArray()
        }

        return console.readPassword(prompt)
    }
}
