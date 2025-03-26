package dev.enric.command.repo.commit

import dev.enric.command.TrackitCommand
import dev.enric.domain.Hash
import dev.enric.core.handler.repo.commit.RestoreHandler
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalArgumentValueException
import dev.enric.util.index.CommitIndex
import picocli.CommandLine.*
import java.nio.file.Path

/**
 * Command to restore the Working Directory to the state of a specified commit.
 *
 * This command restores all files (or specific ones in future versions) from a given commit hash into the current Working Directory.
 * By default, it restores from the current HEAD if no hash is provided (future enhancement).
 *
 * Usage example:
 *   trackit restore a1b2c3d
 */
@Command(
    name = "restore",
    description = ["Restores files from a specified commit into the working directory"],
    mixinStandardHelpOptions = true,
    footer = [
        "",
        "Examples:",
        "  trackit restore a1b2c3d",
        "    Restores all files from the commit with hash 'a1b2c3d' into the current working directory.",
        "",
        "  trackit restore a1b2c3d README.md",
        "    Restores the 'README.md' file from the commit with hash 'a1b2c3d' into the working directory.",
        "",
        "Notes:",
        "  - If no commit hash is provided, the restore will use the current HEAD.",
        "  - If no specific file is provided, all files from the commit will be restored.",
        "  - The commit hash can be provided in full or as an abbreviation, as long as it is unique.",
        "  - Restoring a specific file requires the path relative to the root of the repository.",
        "  - If the specified file doesn't exist in the commit, it will be ignored without error.",
        "",
    ]
)
class Restore : TrackitCommand() {

    /**
     * The hash of the commit to restore from.
     * Supports full and abbreviated hashes.
     */
    @Option(
        names = ["-c", "--commit"],
        paramLabel = "Commit Hash",
        description = ["The hash of the commit to restore from"],
        required = false
    )
    var commitHash: String? = null

    /**
     * The file to restore from the commit.
     */
    @Option(
        names = ["-f", "--file"],
        paramLabel = "File path",
        description = ["The file to restore from the commit"],
        required = true
    )
    var restoreFile: Path? = null

    /**
     * Executes the restore process.
     *
     * 1. Resolves the commit hash (supports abbreviations).
     * 2. Instantiates the [RestoreHandler] with the resolved commit.
     * 3. Restores all files from the commit into the working directory.
     *
     * @return Exit code: 0 if successful, non-zero if an error occurs.
     */
    override fun call(): Int {
        super.call()

        val commit = getCommitByHash()
        val restoreHandler = RestoreHandler(commit = commit, file = restoreFile, sudoArgs = sudoArgs)
        restoreHandler.checkout()

        return 0
    }

    /**
     * Resolves the user-provided hash into a unique [Commit] object.
     *
     * - If the hash is abbreviated, attempts to expand it via [CommitIndex].
     * - Validates that the resolved hash is unique and exists.
     *
     * @throws IllegalArgumentValueException if no matching commit is found or if multiple matches exist.
     * @return The resolved [Commit] instance.
     */
    private fun getCommitByHash(): Commit? {
        if (commitHash.isNullOrEmpty()) {
            return CommitIndex.getCurrentCommit()
        }

        val hashes = if (Hash.isAbbreviatedHash(commitHash!!)) {
            CommitIndex.getAbbreviatedCommit(commitHash!!)
        } else {
            listOf(Hash(commitHash!!))
        }

        when {
            hashes.size > 1 -> throw IllegalArgumentValueException("Multiple commits found starting with $commitHash")
            hashes.isEmpty() -> return CommitIndex.getCurrentCommit()
        }

        return Commit.newInstance(hashes.first())
    }
}