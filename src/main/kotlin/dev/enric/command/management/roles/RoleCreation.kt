package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.management.roles.RoleCreationHandler
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
        description = ["Assign branch permissions. Format: --branch-permission <branch> <permission>. \n" +
                "  In case you want to asssign a empty permission, use '--' between ' '."],
        split = " ",
        arity = "2",
    )
    var branchPermissions: MutableList<String> = mutableListOf()

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        val handler = RoleCreationHandler(
            name,
            level,
            rolePermissions,
            branchPermissions,
            sudoArgs
        )

        // Will never return 1 because the checkCanCreateRole method will throw an exception if the role can't be created
        if (!handler.checkCanCreateRole()) {
            return 1
        }

        handler.createRole()

        return 0
    }
}