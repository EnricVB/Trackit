package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.LogHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

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
    description = ["Show the commit history with filtering options by author, date, and message."],
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
        "  trackit log --message 'bugfix'",
        "    Show commits containing 'bugfix' in their message.",
        "",
        "Notes:",
        "  - Date format: YYYY-MM-DD or YYYY-MM-DDTHH:MM (e.g., 2024-01-01T10:00).",
        "  - All filters can be combined."
    ],
    mixinStandardHelpOptions = true,
)

class Log : TrackitCommand() {

    /**
     * The maximum number of commits to show in the log.
     *
     * This option is used to limit the number of commits shown in the log.
     * By default, the log shows the 3 most recent commits.
     */
    @Option(
        names = ["--limit", "-l"],
        description = ["Shows the specified quantity directly without pressing enter"],
        required = false
    )
    var limit: Int = 3

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
        names = ["--message", "-m"], description = ["Shows the commits with the specified message"], required = false
    )
    var message: String? = null

    /**
     * Entry point for the `log` command execution.
     * Delegates to [LogHandler.showLog] to print commit information.
     *
     * @return 0 on success, other codes for error states.
     */
    override fun call(): Int {
        super.call()

        // Show the commit history
        LogHandler().showLog(
            limit,
            author,
            since?.let { LogHandler.parseDate(it)},
            until?.let { LogHandler.parseDate(it)},
            message ?: "")

        return 0
    }
}
