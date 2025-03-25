package dev.enric.command.administration

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.administration.BlameHandler
import dev.enric.exceptions.IllegalStateException
import dev.enric.util.index.BranchIndex
import picocli.CommandLine.*
import java.nio.file.Path

@Command(
    name = "blame",
    description = ["Indicates who is responsible for the changes in a file."],
    header = ["--- Trackit Blame Command ---"],
    footer = [
    ],
    mixinStandardHelpOptions = true,
)

class Blame : TrackitCommand() {

    @Parameters(index = "0", paramLabel = "File", description = ["The file path to the file to blame."])
    lateinit var filePath: Path

    override fun call(): Int {
        super.call()

        val branch = BranchIndex.getCurrentBranch()
        val commit = BranchIndex.getBranchHead(branch.generateKey())
        val file = filePath.toFile().takeIf { it.exists() }
            ?: throw IllegalStateException("No file found at $filePath")

        println(BlameHandler.blame(file, commit))

        return 0
    }
}
