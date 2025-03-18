package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.DiffHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Commit
import picocli.CommandLine.*

/**
 * Command to show differences between various states of the repository, including:
 * - Working Directory vs Staging Area
 * - Staging Area vs Repository (HEAD)
 * - Working Directory vs Commit/Branch
 * - Commit/Branch vs Commit/Branch
 *
 * Usage examples:
 *   trackit diff                              -> Compare Working Area vs Staging Area (default)
 *   trackit diff -w                           -> Explicitly compare Working Area vs Staging Area
 *   trackit diff -s                           -> Compare Staging Area vs HEAD (last commit)
 *   trackit diff <commit>                     -> Compare Working Area vs specified commit
 *   trackit diff <commit1> <commit2>          -> Compare two commits
 *   trackit diff <branch1> <branch2>          -> Compare two branches
 *
 * Additional options allow filtering and customizing the output.
 */
@Command(
    name = "diff",
    description = ["Shows the difference between two specified commits or files"],
    mixinStandardHelpOptions = true,
)
class Diff : TrackitCommand() {

    /**
     * When set, compares the Working Area with the Staging Area.
     * Cannot be used with commit or branch parameters.
     *
     * Example: trackit diff --working-area
     */
    @Option(
        names = ["--working-area", "-w"],
        description = ["Shows the differences between Working Area and Staging Area"],
        required = false
    )
    var showWorkingAreaDiff: Boolean = false

    /**
     * When set, compares the Staging Area with the last committed state (HEAD).
     * Cannot be used with commit or branch parameters.
     *
     * Example: trackit diff --staged
     */
    @Option(
        names = ["--staged", "-S"],
        description = ["Shows the differences between Staging Area and Repository (HEAD)"],
        required = false
    )
    var showStagedDiff: Boolean = false

    /**
     * Optional positional parameters to specify commits or branches to compare.
     * - If one is provided: compares it with the Working Area.
     * - If two are provided: compares both.
     *
     * Example: trackit diff commit1 commit2
     */
    @Parameters(
        index = "0..1",
        paramLabel = "COMMITS/BRANCHES",
        description = ["Specify one or two commits/branches to compare. If only one is specified, it is compared with Working Area."],
        arity = "0..2"
    )
    var hashes: List<String> = emptyList()

    /**
     * Filter to compare differences for a specific file or directory only.
     *
     * Example: trackit diff --file src/Main.kt
     */
    @Option(
        names = ["--file"],
        description = ["Filter differences for a specific file or directory"],
        required = false
    )
    var fileFilter: String? = null

    override fun call(): Int {
        super.call()

        // Validate that commit/branch parameters are not used with --working-area or --staged
        if ((showWorkingAreaDiff || showStagedDiff) && hashes.isNotEmpty()) {
            println("Error: Cannot use --working-area or --staged together with commit or branch parameters.")
            return 1
        }

        val diffHandler = DiffHandler(fileFilter)

        // Execute the appropriate diff command based on the provided parameters
        when {
            showWorkingAreaDiff -> diffHandler.executeDiffInWorkdir()
            showStagedDiff -> diffHandler.executeDIffInStagingArea()
            //hashes.size == 1 -> diffHandler.executeDiffBetweenWorkdirAndCommit()
            hashes.size == 2 -> diffHandler.executeDiffBetweenCommits(
                Commit.newInstance(Hash(hashes[0])),
                Commit.newInstance(Hash(hashes[1]))
            )
        }

        return 0
    }
}
