package dev.enric.core.handler.administration

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.Hash.HashType.SIMPLE_TAG
import dev.enric.domain.objects.Commit
import dev.enric.domain.objects.User
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
import dev.enric.util.index.TagIndex
import java.io.Console
import java.io.Serializable
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
            if (!isFromAuthor(commit, authorFilter)
                || !isAfterSince(commit, since)
                || !isBeforeUntil(commit, until)
                || !hasMessage(commit, message)
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
        val branchHeads = BranchIndex.getAllBranches().map { BranchIndex.getBranchHead(it) }
        val commitGraph = buildGraph(branchHeads)

        val filteredCommits = commitGraph.values.filter {
            isFromAuthor(it.commit, authorFilter)
                    && isAfterSince(it.commit, since)
                    && isBeforeUntil(it.commit, until)
                    && hasMessage(it.commit, message)
        }

        val sortedCommits = filteredCommits.sortedByDescending { it.commit.date }

        drawGraph(sortedCommits, limit)
    }

    /**
     * Internal representation of a commit node for graph construction.
     *
     * @property commit The actual commit object.
     * @property parents List of parent commit hashes.
     * @property children List of child commit hashes.
     */
    private data class CommitNode(
        val commit: Commit,
        val parents: MutableList<Hash> = mutableListOf(),
        val children: MutableList<Hash> = mutableListOf()
    )

    /**
     * Builds a graph of commits starting from a set of head commits.
     *
     * @param starts List of head commits to start the traversal from.
     * @return A map of commit hash to CommitNode representing the graph.
     */
    private fun buildGraph(starts: List<Commit>): Map<Hash, CommitNode> {
        val visited = mutableMapOf<Hash, CommitNode>()
        val queue = ArrayDeque<Commit>()
        queue.addAll(starts)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val node = visited.getOrPut(current.generateKey()) { CommitNode(current) }

            val parentHash = current.previousCommit ?: continue
            val parent = Commit.newInstance(parentHash)
            node.parents.add(parentHash)

            val parentNode = visited.getOrPut(parent.generateKey()) { CommitNode(parent) }
            parentNode.children.add(current.generateKey())

            queue.addLast(parent)
        }

        return visited
    }

    /**
     * Displays the commit graph in ASCII format, simulating a tree structure.
     *
     * @param commits The list of commit nodes to display.
     * @param limit The maximum number of commits to render.
     */
    private fun drawGraph(commits: List<CommitNode>, limit: Int) {
        val shown = commits.take(limit)
        val lines = mutableListOf<String>()
        val activeBranches = mutableListOf<Hash>()

        for (node in shown) {
            val shortHash = node.commit.generateKey().abbreviate() + "^"
            val line = buildLinePrefix(node, activeBranches) + shortHash
            lines.add(line)

            node.parents.forEach {
                if (!activeBranches.contains(it)) activeBranches.add(it)
            }
        }

        lines.forEach { println(it) }
    }

    /**
     * Constructs a visual prefix for a commit line, indicating its place in the branch graph.
     *
     * @param node The current commit node.
     * @param activeBranches List of active branch heads to align columns.
     * @return A formatted string with ASCII tree structure.
     */
    private fun buildLinePrefix(node: CommitNode, activeBranches: List<Hash>): String {
        val idx = activeBranches.indexOf<Serializable>(node.commit.generateKey())
        val builder = StringBuilder()

        for (i in activeBranches.indices) {
            if (i == idx) builder.append("* ")
            else builder.append("| ")
        }

        if (idx == -1) builder.append("* ")
        return builder.toString()
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
            format
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
        return message.isEmpty()
                || commit.message.contains(message, ignoreCase = true)
                || commit.title.contains(message, ignoreCase = true)
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