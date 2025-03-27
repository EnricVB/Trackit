package dev.enric.command.repo.tag

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.tag.TagHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "remove-tag",
    description = ["Removes a tag from one or multiple ."],
    mixinStandardHelpOptions = true,
)
class TagRemove : TrackitCommand() {

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

        val handler = TagHandler(name, commits, sudoArgs)

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanAssignTags()) {
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
