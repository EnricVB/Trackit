package dev.enric.util.common.console

import dev.enric.logger.Logger
import java.io.Console
import java.util.Scanner

class SystemConsoleInput : ConsoleInput() {
    companion object {
        val instance = SystemConsoleInput()
        val console: Console? = System.console()
        var scanner: Scanner = Scanner(System.`in`)

        fun getInstance(): ConsoleInput {
            return instance
        }

        /**
         * Sets up the [Scanner] for testing.
         * This method simulates user input by providing a predefined string that the [Scanner] will read.
         * It should be called in test environments to provide controlled input.
         *
         * @param input A string representing the input that should be used for testing.
         */
        fun setInput(input: String) {
            scanner = Scanner(input)
        }
    }

    /**
     * Reads a line of input from the system console or [Scanner].
     * If the console is available, it uses the [Console.readLine] method; otherwise, it falls back to the [Scanner].
     * The prompt is logged using [Logger.info] before reading the input.
     *
     * @param prompt The prompt to display to the user.
     * @return The input entered by the user as a string.
     */
    override fun readLine(prompt: String): String {
        if (console == null || isTesting) {
            val line = scanner.nextLine()
            Logger.info(prompt)

            if (isTesting) println(line)
            return line
        }

        return console.readLine(prompt)
    }

    /**
     * Reads a password from the system console or [Scanner].
     * If the console is available, it uses the [Console.readPassword] method; otherwise, it falls back to the [Scanner].
     * The prompt is logged using [Logger.info] before reading the input.
     *
     * @param prompt The prompt to display to the user.
     * @return A [CharArray] containing the password entered by the user.
     */
    override fun readPassword(prompt: String): CharArray {
        if (console == null || isTesting) {
            val line = scanner.nextLine()
            Logger.info(prompt)

            if (isTesting) println(line)
            return line.toCharArray()
        }

        return console.readPassword(prompt)
    }
}