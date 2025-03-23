package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.handler.repo.commit.CommitHandler
import dev.enric.domain.objects.Commit
import picocli.CommandLine.*

/**
 * Command to create a new commit in the repository.
 *
 * This command finalizes changes staged for commit and records them into the repository history.
 * It allows optional staging of all modified/untracked files and can support commit confirmation
 * by a different user through sudo-like behavior.
 *
 * Usage examples:
 *   trackit commit "Fix login bug" "Fixed NPE on login when user has no roles"
 *   trackit commit "Add tests" --all
 */
@Command(
    name = "commit",
    description = ["Commits the staged files"],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Examples:",
        "  trackit commit \"Fix login bug\" \"Fixed NPE on login when user has no roles\"",
        "    Creates a new commit with the title 'Fix login bug' and the detailed message 'Fixed NPE on login when user has no roles'.",
        "",
        "  trackit commit \"Add tests\" --all",
        "    Stages all modified and untracked files and creates a commit with the title 'Add tests'. No commit message is provided, so it defaults to an empty message.",
        "",
        "  trackit commit \"Fix issue\" --confirmer alice password123",
        "    Commits the staged changes as user 'alice' after confirming the action with the provided password.",
        "",
        "Notes:",
        "  - The commit title is mandatory, while the message is optional.",
        "  - Use the --all flag to stage all modified and untracked files before committing.",
        "  - The --confirmer option allows an administrative user to confirm the commit on behalf of another user, requiring both username and password.",
        "  - If no files are staged for commit, the operation will fail.",
        "  - If the commit is invalid or the user lacks permissions, the operation will not proceed.",
        "",
        "For more information, see the Trackit documentation or use the '--help' option for each command."
    ]
)
class Commit : TrackitCommand() {

    /**
     * Title of the commit to be created.
     * This should be a concise summary of the change.
     */
    @Parameters(index = "0", paramLabel = "TITLE", description = ["The commit title"])
    lateinit var title: String

    /**
     * Detailed description of the commit.
     * This is optional but recommended for clarity.
     */
    @Parameters(index = "1", paramLabel = "MESSAGE", description = ["The commit message"], arity = "0..1")
    var message: String? = null

    /**
     * If set, all modified and untracked files are staged before committing.
     */
    @Option(names = ["--all", "-a"], description = ["Stage all modified and untracked files before committing"])
    var stageAllFiles: Boolean = false

    /**
     * Allows confirming the commit as a different user.
     * Requires two arguments (username and password).
     * This supports administrative overrides or peer confirmation.
     */
    @Option(
        names = ["--confirmer", "-c"],
        description = ["Confirm commit as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2",
        required = false
    )
    var confirmerArgs: Array<String>? = sudoArgs

    /**
     * Executes the commit operation.
     *
     * 1. Creates a new Commit object with the given title and message.
     * 2. Initializes commit metadata including author and optional confirmer.
     * 3. Optionally stages all files if requested.
     * 4. Verifies commit preconditions.
     * 5. Finalizes and saves the commit.
     *
     * @return Exit code 0 if commit succeeds, 1 if commit validation fails (exceptions may still be thrown).
     */
    override fun call(): Int {
        super.call()

        // Create the Commit object with title and message
        val commit = Commit(title = title, message = message ?: "")
        val commitHandler = CommitHandler(commit)

        // Initialize commit metadata such as author and confirmer
        commitHandler.initializeCommitProperties(sudoArgs, confirmerArgs)

        // Stage all files if the flag is set
        commitHandler.preCommit(stageAllFiles)

        // Check if commit is allowed; will throw if invalid
        if (!commitHandler.canDoCommit()) {
            return 1
        }

        // Execute the commit process
        commitHandler.processCommit()
        commitHandler.postCommit()

        return 0
    }
}