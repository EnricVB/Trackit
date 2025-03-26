package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.DiffHandler
import dev.enric.domain.Hash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.Commit
import dev.enric.exceptions.IllegalStateException
import dev.enric.logger.Logger
import dev.enric.util.index.BranchIndex
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
 *
 *   trackit diff -w                           -> Explicitly compare Working Area vs Staging Area
 *
 *   trackit diff -s                           -> Compare Staging Area vs HEAD (last commit)
 *
 *   trackit diff <commit>                     -> Compare Working Area vs specified commit
 *
 *   trackit diff <commit1> <commit2>          -> Compare two commits
 *
 *   trackit diff <branch1> <branch2>          -> Compare two branches
 *
 * Additional options allow filtering and customizing the output.
 */
@Command(
    name = "diff",
    description = ["Compare changes between repository states (commits, branches, staging, working directory)."],
    footer = [
        "",
        "Examples:",
        "  trackit diff                         Compare Working Directory vs Staging",
        "  trackit diff --staged                Compare Staging Area vs HEAD",
        "  trackit diff commit1 commit2         Compare two commits",
        "  trackit diff --file src/Main.kt      Filter by file",
    ],
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
        paramLabel = "Hash",
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
            throw IllegalStateException("Cannot use --working-area or --staged together with commit or branch parameters.")
        }

        val diffHandler = DiffHandler(fileFilter)

        // Execute the appropriate diff command based on the provided parameters
        when {
            showWorkingAreaDiff -> {
                Logger.log("Diff STAGING vs WORKDIR\n")
                diffHandler.executeDiffInWorkdir()
            }

            showStagedDiff -> {
                Logger.log("Diff HEAD vs WORKDIR\n")
                val currentBranch = BranchIndex.getCurrentBranch()
                val headCommit = currentBranch.let { BranchIndex.getBranchHead(it.generateKey()) }

                headCommit.let { diffHandler.executeDiffBetweenWorkdirAndCommit(it) }
            }

            hashes.size == 1 -> {
                Logger.log("Diff ${hashes[0]} vs WORKDIR\n")
                diffHandler.executeDiffBetweenWorkdirAndCommit(Commit.newInstance(Hash(hashes[0])))
            }

            hashes.size == 2 -> {
                Logger.log("Diff ${hashes[0]} vs ${hashes[1]}\n")
                val isFirstHashBranch = hashes[0].startsWith(Hash.HashType.BRANCH.hash.string)
                val isSecondHashBranch = hashes[1].startsWith(Hash.HashType.BRANCH.hash.string)

                if (isFirstHashBranch && isSecondHashBranch) {
                    diffHandler.executeDiffBetweenBranches(
                        Branch.newInstance(Hash(hashes[0])),
                        Branch.newInstance(Hash(hashes[1]))
                    )
                } else if (isFirstHashBranch || isSecondHashBranch) {
                    throw IllegalStateException("Cannot compare a branch with a commit.")
                } else {
                    diffHandler.executeDiffBetweenCommits(
                        Commit.newInstance(Hash(hashes[0])),
                        Commit.newInstance(Hash(hashes[1]))
                    )
                }
            }
        }

        return 0
    }
}
