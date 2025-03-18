package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.UserModifyHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Command to modify an existing user in the Trackit system.
 *
 * This command allows for the update of various user attributes, such as password,
 * contact information, and assigned roles. It supports interactive password input
 * and role reassignment.
 *
 * Usage example:
 *   trackit user-modify --name alice --password <current> --new-password <new> --new-mail alice@newmail.com
 */
@Command(
    name = "user-modify",
    description = ["Modifies an existing user"],
    mixinStandardHelpOptions = true,
)
class UserModify : TrackitCommand() {

    /** The username of the user to modify. */
    @Option(names = ["--name", "-n"], description = ["Name of the user to modify"], required = true)
    var name: String = ""

    /** Current password for authentication. */
    @Option(
        names = ["--password", "-p"],
        description = ["Password of the user to modify"],
        interactive = true,
        required = true
    )
    var password: String = ""

    /** Optional new password to set for the user. */
    @Option(
        names = ["--new-password", "-N"],
        description = ["New password for the user"],
        interactive = true,
        required = false
    )
    var newPassword: String? = null

    /** Optional new email address for the user. */
    @Option(names = ["--new-mail", "-M"], description = ["New mail for the user"], required = false)
    var newMail: String? = null

    /** Optional new phone number for the user. */
    @Option(names = ["--new-phone", "-P"], description = ["New phone for the user"], required = false)
    var newPhone: String? = null

    /** Roles to assign to the user. Can assign multiple roles at once. */
    @Option(
        names = ["--role", "-r"],
        description = ["Roles to assign to the user"],
        split = " ",
        required = false
    )
    var roles: Array<String> = emptyArray()

    /** Flag to indicate if the user's previous roles should be deleted before assigning new ones. */
    @Option(
        names = ["--delete-previous-roles", "-d"],
        description = ["Delete previous roles"],
        required = false
    )
    var deletePreviousRoles: Boolean = false

    /**
     * Executes the user modification process.
     *
     * @return Exit code: 0 if successful, 1 if modification is not permitted.
     */
    override fun call(): Int {
        super.call()

        val handler = UserModifyHandler(
            name,
            password,
            newPassword,
            newMail,
            newPhone,
            roles,
            deletePreviousRoles,
            sudoArgs
        )

        // Returns 1 only if user modification is not allowed; usually handled via exception.
        if (!handler.checkCanModifyUser()) {
            return 1
        }

        handler.modifyUser()

        return 0
    }
}
