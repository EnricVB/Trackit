package dev.enric.command.repo.tag

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.tag.TagCreationHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Command for creating a new tag in the repository.
 *
 * This command allows users to create either a simple or complex tag.
 * It verifies that the user has the required permissions and associates the tag with specified commits.
 */
@Command(
    name = "create-tag",
    description = ["Create a new simple or complex tag."],
    mixinStandardHelpOptions = true,
)
class TagCreation : TrackitCommand() {

    /**
     * Name of the tag to be created.
     */
    @Option(names = ["--name", "-n"], description = ["Tag name."], required = true)
    var name: String = ""

    /**
     * Message of the tag to be created. If provided, a complex tag is created; otherwise, a simple tag is created.
     */
    @Option(names = ["--message", "-m"], description = ["Tag message."], required = false)
    var message: String = ""

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

    /**
     * Executes the tag creation command.
     *
     * This method verifies permissions using `checkCanModifyBranch()`, and if successful,
     * it proceeds with creating the tag.
     *
     * @return 0 if successful, otherwise an exception is thrown.
     */
    override fun call(): Int {
        super.call()

        val handler = TagCreationHandler(
            name,
            message,
            commits,
            sudoArgs
        )

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanAssignTags()) {
            return 1
        }

        handler.createTag()

        return 0
    }
}
