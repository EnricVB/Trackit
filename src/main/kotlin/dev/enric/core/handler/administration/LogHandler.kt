package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.User
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import java.io.Console
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Handles the logging functionality for displaying commit history.
 * Commits are displayed in a paginated format, with support for filters.
 */
class LogHandler(val format: String?) : CommandHandler() {
    /**
     * Displays the commit log starting from the current branch head.
     * Commits can be filtered by author and date, and limited in number.
     *
     * @param limit The maximum number of commits to show. If null, uses pagination.
     * @param authorFilter The author name to filter commits by.
     * @param since The minimum date (inclusive) to filter commits. Format: yyyy-MM-ddTHH:mm
     * @param until The maximum date (inclusive) to filter commits. Format: yyyy-MM-ddTHH:mm
     * @param message The message to filter commits by.
     *
     * @throws IllegalStateException if no commits are found in the repository.
     */
    fun showLog(
        limit: Int,
        authorFilter: String?,
        since: LocalDateTime?,
        until: LocalDateTime?,
        message: String
    ) {
        val branchHead = BranchIndex.getBranchHead(BranchIndex.getCurrentBranch().generateKey())
        var commit: Commit? = branchHead
        var shownCount = 0

        // Determine if we use interactive mode
        val interactive = (limit == 0)

        // Initial size for interactive pagination
        var commitShowSize = 1

        while (commit != null) {
            // Filter commits
            if (!isFromAuthor(commit, authorFilter)
                || !isAfterSince(commit, since)
                || !isBeforeUntil(commit, until)
                || !hasMessage(commit, message)) {
                commit = decodePrevious(commit)
                continue
            }

            // Show the commit
            printCommitData(commit)
            shownCount++

            // Exit if limit reached (non-interactive mode)
            if (!interactive && shownCount >= limit) break

            // Pagination prompt if needed
            if (interactive && shownCount >= commitShowSize) {
                print(":")
                when (getKey()) {
                    'q' -> break
                    else -> commitShowSize += 1
                }
            }

            commit = decodePrevious(commit)
        }

        if (shownCount == 0) {
            Logger.warning("No commits found with the specified filters.")
        }
    }

    /**
     * Checks if a commit was made by a specific author.
     *
     * @param commit The commit being evaluated.
     * @param authorFilter The name of the author to filter by.
     *                     If `null`, all authors are accepted.
     * @return `true` if the commit was made by the specified author
     *         or if no author filter is provided; `false` otherwise.
     */
    private fun isFromAuthor(commit: Commit, authorFilter: String?): Boolean {
        return authorFilter == null || User.newInstance(commit.author).name.equals(authorFilter, ignoreCase = true)
    }

    /**
     * Checks if a commit was made after (or at) the specified date.
     *
     * @param commit The commit being evaluated.
     * @param since The minimum date (inclusive) to accept commits from.
     *              If `null`, all dates are accepted.
     * @return `true` if the commit date is after the specified date
     *         or if no date filter is provided; `false` otherwise.
     */
    private fun isAfterSince(commit: Commit, since: LocalDateTime?): Boolean {
        return since == null || commit.date.isAfter(since)
    }

    /**
     * Checks if a commit was made before (or at) the specified date.
     *
     * @param commit The commit being evaluated.
     * @param until The maximum date (inclusive) to accept commits up to.
     *              If `null`, all dates are accepted.
     * @return `true` if the commit date is before the specified date
     *         or if no date filter is provided; `false` otherwise.
     */
    private fun isBeforeUntil(commit: Commit, until: LocalDateTime?): Boolean {
        return until == null || commit.date.isBefore(until)
    }

    /**
     * Checks if the commit message contains a given substring.
     *
     * @param commit The commit being evaluated.
     * @param message The message filter to apply. If empty, all commit messages are accepted.
     * @return `true` if the commit message contains the specified substring
     *         or if no message filter is provided; `false` otherwise.
     */
    private fun hasMessage(commit: Commit, message: String): Boolean {
        return message.isEmpty() || commit.message.contains(message) || commit.title.contains(message)
    }

    /**
     * Helper to decode the previous commit.
     */
    private fun decodePrevious(commit: Commit): Commit? {
        return commit.previousCommit?.let { Commit().decode(it) }
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
     * The output format can be customized using the provided format string.
     *
     * @param commit The commit whose details are to be displayed.
     */
    fun printCommitData(commit: Commit) {
        if (format == null) {
            Logger.log("\n${commit.printInfo()}")
            return
        }

        val commitHash = commit.generateKey().toString()
        val commitDate = commit.date.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        val author = User.newInstance(commit.author)
        val confirmer = User.newInstance(commit.confirmer)
        val tags = TagIndex.getTagsByCommit(commit.generateKey())
        val tagNames = tags.map {
            val isComplexTag = it.string.startsWith(COMPLEX_TAG.hash.string)
            val isSimpleTag = it.string.startsWith(SIMPLE_TAG.hash.string)

            return@map when {
                isComplexTag -> ComplexTag.newInstance(it).name
                isSimpleTag -> SimpleTag.newInstance(it).name
                else -> throw IllegalStateException("Invalid tag type")
            }
        }.joinToString(", ")

        Logger.log(format
            .replace("{ch}", commitHash)
            .replace("{chS}", commitHash.substring(0, 5).plus("^"))
            .replace("{date}", commitDate.toString())
            .replace("{title}", commit.title)
            .replace("{message}", commit.message)
            .replace("{ah}", author.generateKey().toString())
            .replace("{ahS}", author.generateKey().toString().substring(0, 5).plus("^"))
            .replace("{an}", author.name)
            .replace("{am}", author.mail)
            .replace("{ap}", author.phone)
            .replace("{Ch}", confirmer.generateKey().toString())
            .replace("{ChS}", confirmer.generateKey().toString().substring(0, 5).plus("^"))
            .replace("{Cn}", confirmer.name)
            .replace("{Cm}", confirmer.mail)
            .replace("{Cp}", confirmer.phone)
            .replace("{Th}", tags.joinToString(", ") { it.string })
            .replace("{ThS}", tags.joinToString(", ") { it.string.substring(0, 5).plus("^") })
            .replace("{Tn}", tagNames))
    }

    companion object {
        /**
         * Parses a date string in the format yyyy-MM-ddTHH:mm
         * Example: "2025-03-16T14:30" | "2025-03-16"
         */
        fun parseDate(dateStr: String): LocalDateTime {
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            return try {
                LocalDateTime.parse(dateStr, dateTimeFormatter)
            } catch (e: Exception) {
                try {
                    val date = LocalDate.parse(dateStr, dateFormatter)
                    date.atStartOfDay()
                } catch (e: Exception) {
                    throw IllegalArgumentValueException("Invalid date format. Use yyyy-MM-dd'T'HH:mm or yyyy-MM-dd")
                }
            }
        }
    }
}

/**
 * Checks if a commit was made after (or at) the specified date.
 *
 * @param since The minimum date (inclusive) to accept commits from.
 * @return `true` if the commit date is after the specified date
 */
private fun Timestamp.isBefore(since: LocalDateTime): Boolean {
    val localDate = this.toLocalDateTime().truncatedTo(ChronoUnit.DAYS)
    return localDate.isBefore(since) || localDate.isEqual(since)
}

/**
 * Checks if a commit was made before (or at) the specified date.
 *
 * @param until The maximum date (inclusive) to accept commits up to.
 * @return `true` if the commit date is before the specified date
 */
private fun Timestamp.isAfter(until: LocalDateTime): Boolean {
    val localDate = this.toLocalDateTime().truncatedTo(ChronoUnit.DAYS)
    return localDate.isAfter(until) || localDate.isEqual(until)
}
