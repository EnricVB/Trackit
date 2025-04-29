package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.TagHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "remove-tag",
    description = ["Removes a tag from one or multiple ."],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Examples:",
        "  trackit repo tag remove-tag --name <tag> --commit <commit1> <commit2>",
        "       This will remove the tag <tag> from the commits <commit1> and <commit2>",
        "",
        "Notes:",
        "  - The tag name must be unique.",
        "  - The commit hashes must be valid.",
        "  - The tag must exist in the repository.",
        "  - The user must have permission to modify the tag.",
        "  - The user must have permission to modify the commits.",
    ]
)
class TagRemoveCommand : TrackitCommand() {

    /**
     * Name of the tag to be removed from the commits.
     */
    @Option(names = ["--name", "-n"], description = ["Tag name."], required = true)
    var name: String = ""

    /**
     * A list of commit hashes that will be removed from having this tag.
     */
    @Option(
        names = ["--commit", "-c"],
        description = ["Commits that will no longer have this tag."],
        required = false,
        arity = "0..*",
        split = " "
    )
    var commits: List<String> = emptyList()

    override fun call(): Int {
        super.call()

        val handler = TagHandler(name, commits.map { Hash(it) }, sudoArgs)

        // checkModifyModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            return 1
        }

        // Remove the tag to the commits
        commits.forEach {
            val commit = Commit.newInstance(Hash(it))
            handler.removeTagFromCommit(commit)
        }

        return 0
    }
}
