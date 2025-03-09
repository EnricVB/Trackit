package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.commandconsumer.SudoArgsParameterConsumer
import dev.enric.core.management.roles.RoleModifyHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option


@Command(
    name = "role-modify",
    description = ["Modifies a new role"],
    mixinStandardHelpOptions = true,
)
class RoleModify : TrackitCommand() {
    @Option(names = ["--name", "-n"], description = ["Role name to modify."], required = true)
    var name: String = ""

    @Option(names = ["--permission-level", "-l"], description = ["Level permissions. Can't be equal or greater than user permissions level"], required = false)
    var level: Int = 0

    @Option(
        names = ["--role-permission", "-r"],
        description = ["Adds new role permissions to the current one. The permissions are: \n" +
                "  - m: Modify role permissions, except a equal or higher roles\n" +
                "  - u: User operations, as create or modify users\n" +
                "  - s: Assign new roles to specified user\n" +
                "  - a: Create new roles\n" +
                "  - '-': To specify role has not the permission"]
    )
    var rolePermissions: String = "----"

    @Option(
        names = ["--branch-permission", "-b"],
        description = ["Adds branch permissions. Format: --branch-permission <branch> <permission>. \n" +
                "  In case you want to asssign a empty permission, use '--' between ' '."],
        split = " ",
        arity = "2",
    )
    var branchPermissions: MutableList<String> = mutableListOf()

    @Option(
        names = ["--remove-brach-permission", "-rb"],
        description = ["Removes branch permissions. Format: --remove-branch-permission <branch> <branch> ..."],
        split = " "
    )
    var removeBranchPermissions: MutableList<String> = mutableListOf()

    @Option(
        names = ["--overwrite", "-o"],
        description = ["Overwrite the current role permissions"]
    )
    var overwrite: Boolean = false

    @Option(
        names = ["--sudo", "-s"],
        description = ["Execute command as user"],
        parameterConsumer = SudoArgsParameterConsumer::class,
        arity = "2"
    )
    var sudoArgs: Array<String>? = null

    override fun call(): Int {
        super.call()

        val handler = RoleModifyHandler(
            name,
            level,
            rolePermissions,
            branchPermissions,
            removeBranchPermissions,
            overwrite,
            sudoArgs
        )

        if (!handler.checkCanModifyRole()) {
            return 1
        }

        handler.modifyRole()

        return 0
    }
}