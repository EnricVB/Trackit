package dev.enric.cli.repo

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.repo.TagCreationHandler
import dev.enric.domain.Hash
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
    usageHelpWidth = 500,
    footer = [
        "",
        "Examples:",
        "  trackit create-tag --name v1.0",
        "    Creates a simple tag named 'v1.0'.",
        "",
        "  trackit create-tag --name v1.0 --message 'Version 1.0 release' --commit a1b2c3d e4f5g6h",
        "    Creates a complex tag named 'v1.0' with the message 'Version 1.0 release' and assigns it to the commits with hashes 'a1b2c3d' and 'e4f5g6h'.",
        "",
        "Notes:",
        "  - The tag name must be unique within the repository.",
        "  - The commit hashes can be provided in full or as abbreviations, as long as they are unique.",
        "  - If no commit hashes are provided, the tag will not be assigned to any commits, but will be created.",
        "  - The message is optional; if not provided, a simple tag will be created.",
        "  - If the specified commit does not exist, it will be ignored without error.",
        ""
    ]
)
class TagCreationCommand : TrackitCommand() {

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
            commits.map { Hash(it) },
            sudoArgs
        )

        // checkCanModifyBranch throws an exception if the user lacks permissions, so this will never return 1
        if (!handler.checkCanModifyTags()) {
            return 1
        }

        handler.createTag()

        return 0
    }
}
