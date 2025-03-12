package dev.enric.core.administration

import dev.enric.domain.Commit
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import java.io.Console
import java.util.*

/**
 * Handles the logging functionality for displaying commit history.
 * Commits are displayed in a paginated format, allowing users to navigate
 * through the history interactively.
 */
class LogHandler {

    /**
     * Displays the commit log starting from the current branch head.
     * Commits are shown in a paginated manner, with an initial batch of 5.
     * Users can press the **spacebar** to show more commits or **q** to exit.
     *
     * @throws IllegalStateException if no commits are found in the repository.
     */
    fun showLog() {
        val branchHead = BranchIndex.getBranchHead()
            ?: throw IllegalStateException("No commits found in the repository")

        var commitShowSize = 1
        var commit: Commit? = branchHead

        var count = 0
        while (commit != null && count < commitShowSize) {
            printCommitData(commit)
            count++

            // Prompt the user to load more commits
            print(": ")
            if (count >= commitShowSize) {
                when (getKey()) {
                    'q' -> return
                    else -> commitShowSize += 1
                }
            }

            // Move to the previous commit
            commit = commit.previousCommit?.let { Commit().decode(it) }
        }
    }

    /**
     * Captures a single key press from the user.
     *
     * - **Space (' ')** → Load more commits.
     * - **'q'** → Exit the log.
     *
     * @return The character pressed by the user.
     */
    private fun getKey(): Char {
        val console: Console? = System.console()

        return console?.reader()?.read()?.toChar() ?: (Scanner(System.`in`).next().firstOrNull() ?: ' ')
    }

    /**
     * Prints commit details to the logger.
     *
     * @param commit The commit whose details are to be displayed.
     */
    fun printCommitData(commit: Commit) {
        Logger.log(commit.printInfo())
    }
}