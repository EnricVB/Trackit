package dev.enric.cli.admin

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.LogHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Displays the commit history of the current repository in a structured and readable format.
 *
 * This command lists past commits in reverse chronological order, showing details such as:
 * - Commit hash
 * - Author
 * - Commit date
 * - Commit message
 *
 * Usage examples:
 *   trackit log                  → Shows the entire commit history
 *   trackit log --help           → Displays help for this command
 *
 * This class delegates the actual logging logic to [LogHandler].
 */
@Command(
    name = "log",
    description = ["Show the commit history."],
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit log",
        "    Show the last 3 commits (default).",
        "",
        "  trackit log --limit 10",
        "    Show the 10 most recent commits.",
        "",
        "  trackit log --author alice",
        "    Show commits authored by 'alice'.",
        "",
        "  trackit log --since 2024-01-01 --until 2024-03-01",
        "    Show commits between January and March 2024.",
        "",
        "  trackit log --message \"bugfix\"",
        "    Show commits containing 'bugfix' in their message.",
        "",
        "  trackit log --format \"{chS} - {an} <{ahS}> : {title} - {message}\"",
        "    Show commits with a custom format.",
        "",
        "Format parameters:",
        "  {ch}    Full commit hash",
        "  {chS}   Short commit hash (first 5 characters + '^')",
        "  {date}  Commit date",
        "  {title} Commit title",
        "  {message} Commit message",
        "",
        "  {ah}    Author hash",
        "  {ahS}   Short author hash (first 5 characters + '^')",
        "  {an}    Author name",
        "  {am}    Author email",
        "  {ap}    Author phone",
        "",
        "  {Ch}    Confirmer hash",
        "  {ChS}   Short confirmer hash (first 5 characters + '^')",
        "  {Cn}    Confirmer name",
        "  {Cm}    Confirmer email",
        "  {Cp}    Confirmer phone",
        "",
        "  {Th}    Full tag names (comma-separated)",
        "  {ThS}   Short tag names (first 5 characters + '^', comma-separated)",
        "  {Tn}    Tag titles (comma-separated)",
        "",
        "Notes:",
        "  - Date format: YYYY-MM-DD or YYYY-MM-DDTHH:MM (e.g., 2024-01-01T10:00).",
        "  - All filters can be combined."
    ],
    mixinStandardHelpOptions = true,
)

class LogCommand : TrackitCommand() {

    /**
     * The maximum number of commits to show in the log.
     *
     * This option is used to limit the number of commits shown in the log.
     * By default, the log shows the 3 most recent commits.
     */
    @Option(
        names = ["--limit", "-l"],
        description = ["Shows the specified quantity."],
        required = false
    )
    var limit: Int = 10

    /**
     * The author of the commits to show in the log.
     *
     * This option is used to filter the commits shown in the log by the author.
     * By default, the log shows all commits.
     */
    @Option(names = ["--author", "-a"], description = ["Shows the commits of the specified author"], required = false)
    var author: String? = null

    /**
     * The date since which to show commits in the log.
     *
     * This option is used to filter the commits shown in the log by the date.
     * By default, the log shows all commits.
     *
     * Example of valid date formats:
     * - "2021-01-01"
     * - "2021-01-01T00:00"
     */
    @Option(names = ["--since", "-S"], description = ["Shows the commits since the specified date"], required = false)
    var since: String? = null

    /**
     * The date until which to show commits in the log.
     *
     * This option is used to filter the commits shown in the log by the date.
     * By default, the log shows all commits.
     *
     * Example of valid date formats:
     * - "2021-01-01"
     * - "2021-01-01T00:00"
     */
    @Option(names = ["--until", "-u"], description = ["Shows the commits until the specified date"], required = false)
    var until: String? = null

    /**
     * The commit message to search for in the log.
     *
     * This option is used to filter the commits shown in the log by the commit message.
     * By default, the log shows all commits.
     */
    @Option(
        names = ["--message", "-m"],
        description = ["Shows the commits with the specified title/message"],
        required = false
    )
    var message: String = ""

    /**
     * The format in which to show the commits in the log.
     *
     * This option is used to specify the format of the commit information displayed in the log.
     * By default, the log shows the commit information in a standard format.
     */
    @Option(
        names = ["-f", "--format"], description = ["Shows the commits with the specified format"], required = false
    )
    var format: String? = null

    @Option(
        names = ["-g", "--graph"], description = ["Prints commit as a graph"], required = false
    )
    var graph: Boolean = false

    @Option(
        names = ["-i", "--inline"], description = ["Prints commits in a single line"], required = false
    )
    var inline: Boolean = false

    /**
     * Entry point for the `log` command execution.
     * Delegates to [LogHandler.showGraphLog] to print commit information.
     *
     * @return 0 on success, other codes for error states.
     */
    override fun call(): Int {
        super.call()

        // Show the commit history
        val logHandler = LogHandler(
            format,
            limit,
            author,
            parseDateTime(since),
            parseDateTime(until),
            message
        )

        // If inline option is set, print inline log replacing the default format
        if (inline) {
            format = "{chS} - {an} <{ahS}> : {title} - {message}"
        }

        // Depending if oneline option is introduced, print inline or formatted log
        if (graph) {
            logHandler.showGraphLog()
        } else {
            logHandler.showFormattedLog()
        }


        return 0
    }
}

/**
 * Parses a date-time string in ISO_LOCAL_DATE_TIME format.
 *
 * @param str the input date string.
 * @return LocalDateTime object or null if input is null or empty.
 */
private fun parseDateTime(str: String?): LocalDateTime? {
    if (str.isNullOrEmpty()) return null
    return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
