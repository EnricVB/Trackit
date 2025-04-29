package dev.enric.cli.admin

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.GarbageRecolectorHandler
import picocli.CommandLine.*

/**
 * The GarbageRecolectorCommand is responsible for cleaning up unused indexes and objects from the system.
 * This includes removing unused tags, commits, permissions, and roles.
 * The command is useful for keeping the system lean by removing objects that are no longer needed,
 * improving performance and reducing storage consumption.
 *
 * Usage examples:
 * - Removes all unused indexes and objects:
 *   trackit gr
 *
 * - Removes all unused tags:
 *   trackit gr --tags
 *
 * - Removes all unused commits:
 *   trackit gr --commits
 *
 * - Removes all unused permissions:
 *   trackit gr --permissions
 *
 * - Removes all unused roles:
 *   trackit gr --roles
 *
 * Options:
 * - `--tags`: Removes all tags that are no longer in use.
 * - `--commits`: Removes all commits that are no longer in use, i.e., commits not part of the current branch.
 * - `--permissions`: Removes all permissions that are no longer in use.
 * - `--roles`: Removes all roles that are no longer in use.
 */
@Command(
    name = "gr",
    description = ["Removes all indexes and objects that are no longer in use."],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Examples:",
        "  trackit gr",
        "    Removes all indexes and objects that are no longer in use.",
        "",
        "  trackit gr --tags",
        "    Removes all tags that are no longer in use.",
        "",
        "  trackit gr --commits",
        "    Removes all commits that are no longer in use.",
        "",
        "  trackit gr --permissions",
        "    Removes all permissions that are no longer in use.",
        "",
        "  trackit gr --roles",
        "    Removes all roles that are no longer in use."
    ]
)
class GarbageRecolectorCommand : TrackitCommand() {

    /**
     * Flag indicating whether to remove all tags that are no longer in use.
     * If true, unused tags will be deleted from the system.
     */
    @Option(names = ["-t", "--tags"], description = ["Removes all tags that are no longer in use."])
    var recolectTags: Boolean = false

    /**
     * Flag indicating whether to remove all commits that are no longer in use.
     * If true, commits not part of the current branch will be deleted.
     */
    @Option(
        names = ["-c", "--commits"],
        description = [
            "Removes all commits that are no longer in use.",
            "This will remove all commits that are not part of the current branch."
        ]
    )
    var recolectCommits: Boolean = false

    /**
     * Flag indicating whether to remove all permissions that are no longer in use.
     * If true, unused permissions will be deleted from the system.
     */
    @Option(names = ["-p", "--permissions"], description = ["Removes all permissions that are no longer in use."])
    var recolectPermissions: Boolean = false

    /**
     * Flag indicating whether to remove all roles that are no longer in use.
     * If true, unused roles will be deleted from the system.
     */
    @Option(names = ["-r", "--roles"], description = ["Removes all roles that are no longer in use."])
    var recolectRoles: Boolean = false

    /**
     * Executes the Garbage Collection process based on the specified options.
     * Depending on which flags are enabled, the corresponding cleanup operations will be triggered.
     * It uses the `GarbageRecolectorHandler` to perform the cleanup actions.
     *
     * @return an exit code indicating the success or failure of the operation (0 for success).
     */
    override fun call(): Int {
        super.call()

        val grHandler = GarbageRecolectorHandler()

        // Perform cleanup actions based on user input
        if (recolectTags) { grHandler.recolectTags() }
        if (recolectCommits) { grHandler.recolectCommits() }
        if (recolectPermissions) { grHandler.recolectPermissions() }
        if (recolectRoles) { grHandler.recolectRoles() }

        return 0
    }
}