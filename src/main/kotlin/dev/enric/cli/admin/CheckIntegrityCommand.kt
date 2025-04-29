package dev.enric.cli.admin

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.CheckIntegrityHandler
import dev.enric.domain.Hash
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "check-integrity",
    description = ["Checks the integrity of the repository objects"],
    footer = [
        "",
        "This command checks the integrity of the repository objects.",
        "It can be used to check the integrity of a specific object or all objects of a specific type.",
        "",
        "Allowed types:",
        "  - content",
        "  - tree",
        "  - commit",
        "  - simple-tag",
        "  - complex-tag",
        "  - user",
        "  - branch",
        "  - remote",
        "  - role",
        "  - branch_permission",
        "  - user_permission",
        "",
        "Examples:",
        "  trackit check-integrity --object 123abc456def",
        "  trackit check-integrity --type commit",
        "  trackit check-integrity --type branch",
    ],
    mixinStandardHelpOptions = true,
)

class CheckIntegrityCommand : TrackitCommand() {

    @Option(
        names = ["--object"],
        description = ["The object to check the integrity of"],
        required = false,
    )
    var objectHash: String? = null

    @Option(
        names = ["--type"],
        description = ["The type of object to check the integrity of"],
        required = false,
        converter = [Hash.HashTypeConverter::class],
    )
    var objectType: Hash.HashType? = null

    override fun call(): Int {
        super.call()

        val handler = CheckIntegrityHandler()

        return 0
    }
}
