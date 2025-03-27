package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.GarbageRecolectorHandler
import picocli.CommandLine.*

/**
 * The Blame command identifies the author responsible for changes in a given file.
 * It retrieves the latest commit for the current branch and determines who made the changes.
 */
@Command(
    name = "gr",
    description = ["Removes all indexes and objects that are no longer in use."],
    mixinStandardHelpOptions = true,
)

class GarbageRecolector : TrackitCommand() {

    @Option(names = ["-t", "--tags"], description = ["Removes all tags that are no longer in use."])
    var recolectTags: Boolean = false

    @Option(
        names = ["-c", "--commits"],
        description = [
            "Removes all commits that are no longer in use.",
            "This will remove all commits that are not part of the current branch."
        ]
    )
    var recolectCommits: Boolean = false

    @Option(names = ["-p", "--permissions"], description = ["Removes all permissions that are no longer in use."])
    var recolectPermissions: Boolean = false

    @Option(names = ["-r", "--roles"], description = ["Removes all roles that are no longer in use."])
    var recolectRoles: Boolean = false

    override fun call(): Int {
        super.call()

        val grHandler = GarbageRecolectorHandler()

        if (recolectTags) { grHandler.recolectTags() }
        if (recolectCommits) { grHandler.recolectCommits() }
        if (recolectPermissions) { grHandler.recolectPermissions() }
        if (recolectRoles) { grHandler.recolectRoles() }

        return 0
    }
}
