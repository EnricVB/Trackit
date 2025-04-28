package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.User
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import java.io.Console
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*

/**
 * Handles the logging functionality for displaying commit history across one or multiple branches.
 * It supports filtering by author, date range, and message content, and renders a simple ASCII graph view.
 *
 * @property format The format string for displaying commit details. If null, uses default formatting.
 * @property limit The maximum number of commits to display. If 0, uses interactive mode.
 * @property authorFilter The author name to filter commits by. If null, no filtering is applied.
 * @property since The minimum date (inclusive) to filter commits. If null, no lower bound is applied.
 * @property until The maximum date (inclusive) to filter commits. If null, no upper bound is applied.
 * @property message The message to filter commits by. If empty, no filtering is applied.
 */
class LogHandler(
    val format: String?,
    val limit: Int,
    val authorFilter: String?,
    val since: LocalDateTime?,
    val until: LocalDateTime?,
    val message: String
) : CommandHandler() {

    /**
     * Displays the commit log starting from the current branch head.
     * Commits can be filtered by author and date, and limited in number.
     *
     * @throws IllegalStateException if no commits are found in the repository.
     */
    fun showFormattedLog() {
        val branchHead = BranchIndex.getBranchHead(BranchIndex.getCurrentBranch().generateKey())
        var commit: Commit? = branchHead
        var shownCount = 0

        // Determine if we use interactive mode
        val interactive = (limit == 0)

        // Initial size for interactive pagination
        var commitShowSize = 1

        while (commit != null) {
            // Filter commits
            if (!isFromAuthor(commit, authorFilter) || !isAfterSince(commit, since) || !isBeforeUntil(
                    commit,
                    until
                ) || !hasMessage(commit, message)
            ) {
                commit = decodePrevious(commit)
                continue
            }

            // Show the commit
            drawFormattedCommit(commit)
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
     * Displays the commit log starting from one or more branch heads.
     * Commits are displayed in topological order and rendered with a simple graph structure.
     */
    fun showInlineLog() {
        val branches = BranchIndex.getAllBranches()
        val commits = mutableListOf<Commit>()

        // Collect commits for each branch
        branches.forEach { branch ->
            val branchCommits = mutableListOf<Commit>()

            // Start from the branch head and traverse backwards
            var commit: Commit? = BranchIndex.getBranchHead(branch)
            while (commit != null) {
                branchCommits.add(commit)

                commit = decodePrevious(commit)
            }

            // Filter commits based on user input
            commits.addAll(branchCommits)
        }

        // Draw the graph
        drawGraph(commits.sortedByDescending { it.date }.distinct().toMutableList())
    }

    /**
     * Displays the commit graph in ASCII format, simulating a tree structure.
     */
    private fun drawGraph(commits: MutableList<Commit>) {
        val branches = BranchIndex.getAllBranches().map { Branch.newInstance(it) }.sortedByDescending { it.name }
        val branchOrder = branches.map { it.generateKey() }

        // Map to store the structure of lines
        val lineStructure = mutableMapOf<Hash, MutableList<Pair<Int, Int>>>()

        // Pre-process branches to determine their order
        commits.forEach { commit ->
            val currentBranch = commit.branch
            val previousCommit = commit.previousCommit?.let { Commit().decode(it) }
            val parentBranch = previousCommit?.branch

            if (previousCommit != null && parentBranch != currentBranch) {
                val currentIndex = branchOrder.indexOf(currentBranch)
                val parentIndex = branchOrder.indexOf(parentBranch)

                // Store the connection between branches
                val key = commit.generateKey()
                lineStructure[key] = mutableListOf(Pair(currentIndex, parentIndex))
            }
        }

        // Draw the graph
        commits.forEach { commit ->
            val lines = mutableListOf<StringBuilder>()
            val mainLine = StringBuilder()
            lines.add(mainLine)

            val currentBranch = commit.branch
            val currentIndex = branchOrder.indexOf(currentBranch)

            // Add commit date
            mainLine.append(daysBetween(commit.date.toLocalDateTime(), LocalDateTime.now()).toString().plus(" days ago").padEnd(14, ' '))

            // Draw the primary line for the commit
            branchOrder.forEachIndexed { colIndex, branchKey ->
                val branch = Branch.newInstance(branchKey)
                val isBranchOldEnough = branch.creationDate < commit.date

                if (colIndex == currentIndex) {
                    mainLine.append("* ")
                } else if (isBranchOldEnough) {
                    mainLine.append("| ")
                } else {
                    mainLine.append("  ")
                }
            }

            // Draw the commit hash
            mainLine.append(commit.generateKey().abbreviate() + "^")
            mainLine.append("\t${commit.title}")

            // Verify if this commit has special connection lines
            val commitKey = commit.generateKey()
            if (lineStructure.containsKey(commitKey)) {
                val connections = lineStructure[commitKey]!!

                // Add additional lines for each connection
                connections.forEach { (fromIndex, toIndex) ->
                    // Check if direction is diagonal
                    val isRightToLeft = fromIndex < toIndex
                    val startIndex = minOf(fromIndex, toIndex)
                    val endIndex = maxOf(fromIndex, toIndex)

                    // Create the correct number of lines for the diagonal connection
                    val distance = endIndex - startIndex
                    for (step in 1..distance) {
                        val line = StringBuilder().append(" ".repeat(14))
                        val currentStep = if (isRightToLeft) step else distance - step + 1

                        branchOrder.forEachIndexed { colIndex, branchKey ->
                            val branch = Branch.newInstance(branchKey)
                            val isNewCreatedBranch =
                                branch.creationDate.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS) ==
                                        commit.date.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
                            val isBranchOldEnough = branch.creationDate < commit.date

                            if (isNewCreatedBranch) return@forEachIndexed

                            if (colIndex == startIndex + currentStep - 1) {
                                if (isRightToLeft) line.append("\\ ") else line.append("/ ")
                            } else if (!isBranchOldEnough) {
                                line.append("  ")
                            } else if (colIndex in startIndex..endIndex) {
                                line.append("| ")
                            } else {
                                line.append("| ")
                            }
                        }
                        lines.add(line)
                    }
                }
            }

            // Imprimir todas las líneas
            lines.forEach { println(it) }
        }
    }

    /**
     * Prints commit details to the logger.
     * The output format can be customized using the provided format string.
     *
     * @param commit The commit whose details are to be displayed.
     */
    fun drawFormattedCommit(commit: Commit) {
        if (format == null) {
            Logger.info("\n${commit.printInfo()}")
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

        Logger.info(
            format.replace("{ch}", commitHash).replace("{chS}", commitHash.substring(0, 5).plus("^"))
                .replace("{date}", commitDate.toString()).replace("{title}", commit.title)
                .replace("{message}", commit.message).replace("{ah}", author.generateKey().toString())
                .replace("{ahS}", author.generateKey().toString().substring(0, 5).plus("^"))
                .replace("{an}", author.name)
                .replace("{am}", author.mail).replace("{ap}", author.phone)
                .replace("{Ch}", confirmer.generateKey().toString())
                .replace("{ChS}", confirmer.generateKey().toString().substring(0, 5).plus("^"))
                .replace("{Cn}", confirmer.name).replace("{Cm}", confirmer.mail).replace("{Cp}", confirmer.phone)
                .replace("{Th}", tags.joinToString(", ") { it.string })
                .replace("{ThS}", tags.joinToString(", ") { it.string.substring(0, 5).plus("^") })
                .replace("{Tn}", tagNames)
        )
    }

    /**
     * Helper to decode the previous commit.
     */
    private fun decodePrevious(commit: Commit): Commit? {
        return commit.previousCommit?.let { Commit().decode(it) }
    }

    /**
     * Returns true if the commit was made by the specified author.
     */
    private fun isFromAuthor(commit: Commit, authorFilter: String?): Boolean {
        return authorFilter == null || User.newInstance(commit.author).name.equals(authorFilter, ignoreCase = true)
    }

    /**
     * Returns true if the commit was made after the specified start date (inclusive).
     */
    private fun isAfterSince(commit: Commit, since: LocalDateTime?): Boolean {
        return since == null || commit.date.isAfter(since)
    }

    /**
     * Returns true if the commit was made before the specified end date (inclusive).
     */
    private fun isBeforeUntil(commit: Commit, until: LocalDateTime?): Boolean {
        return until == null || commit.date.isBefore(until)
    }

    /**
     * Returns true if the commit message or title contains the given filter string.
     */
    private fun hasMessage(commit: Commit, message: String): Boolean {
        return message.isEmpty() || commit.message.contains(
            message,
            ignoreCase = true
        ) || commit.title.contains(message, ignoreCase = true)
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
     * Calculates the number of days between two LocalDateTime instances.
     *
     * @param start The start date.
     * @param end The end date.
     *
     * @return The number of days between the two dates.
     */
    private fun daysBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return ChronoUnit.DAYS.between(start, end)
    }
}

/**
 * Checks if a commit was made after (or at) the specified date.
 *
 * @param since The minimum date (inclusive) to accept commits from.
 * @return true if the commit date is after the specified date
 */
private fun Timestamp.isBefore(since: LocalDateTime): Boolean {
    val localDate = this.toLocalDateTime().truncatedTo(ChronoUnit.DAYS)
    return localDate.isBefore(since) || localDate.isEqual(since)
}

/**
 * Checks if a commit was made before (or at) the specified date.
 *
 * @param until The maximum date (inclusive) to accept commits up to.
 * @return true if the commit date is before the specified date
 */
private fun Timestamp.isAfter(until: LocalDateTime): Boolean {
    val localDate = this.toLocalDateTime().truncatedTo(ChronoUnit.DAYS)
    return localDate.isAfter(until) || localDate.isEqual(until)
}