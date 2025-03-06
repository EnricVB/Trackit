package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import picocli.CommandLine.Command
import picocli.CommandLine.Option


@Command(
    name = "role-create",
    description = ["Creates a new role"],
    mixinStandardHelpOptions = true,
)
class RoleCreation : TrackitCommand() {
    @Option(names = ["--name", "-n"], description = ["Role name."], required = true)
    var name: String = ""

    @Option(names = ["--permission-level", "-l"], description = ["Level permissions. Can't be equal or greater than user permissions level"], required = false)
    var level: Int = 0

    @Option(
        names = ["--role-permission", "-r"],
        description = ["Assign role permissions. The permissions are: \n" +
                "  - m: Modify role permissions, except a equal or higher roles\n" +
                "  - u: User operations, as create or modify users\n" +
                "  - s: Assign new roles to specified user\n" +
                "  - a: Create new roles\n" +
                "  - '-': To specify role has not the permission"]
    )
    var rolePermissions: String = "----"

    @Option(
        names = ["--branch-permission", "-b"],
        description = ["Assign branch permissions. The permissions are: \n" +
                "  - r: Read the content of the branch, as clone it into your local\n" +
                "  - w: Write commits into the branch\n" +
                "  - '-': To specify role has not the permission"]
    )
    var branchPermissions: String = "--"

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        return 0
    }
}