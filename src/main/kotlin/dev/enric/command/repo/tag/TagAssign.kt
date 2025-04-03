package dev.enric.command.repo.tag

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.tag.TagHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "assign-tag",
    description = ["Assigns a tag to one or multiple commits."],
    mixinStandardHelpOptions = true,
)
class TagAssign : TrackitCommand() {

    /**
     * Name of the tag to be assigned to the commits.
     */
    @Option(names = ["--name", "-n"], description = ["Tag name."], required = true)
    var name: String = ""

    /**
     * A list of commit hashes that will be associated with this tag.
     */
    @Option(
        names = ["--commit", "-c"],
        description = ["Commits that will assign this tag."],
        required = false,
        arity = "0..*",
        split = " "
    )
    var commits: List<String> = emptyList()

    override fun call(): Int {
        super.call()

        val handler = TagHandler(name, commits.map { Hash(it) }, sudoArgs)

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            return 1
        }

        // Assign the tag to the commits
        commits.forEach {
            val commit = Commit.newInstance(Hash(it))
            handler.assignTag(commit)
        }

        return 0
    }
}
