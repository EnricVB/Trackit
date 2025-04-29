package dev.enric.cli.management

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.management.UserModifyHandler
import dev.enric.logger.Logger
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
 *   trackit modify-user --name alice --password <current> --new-password <new> --new-mail alice@newmail.com
 */
@Command(
    name = "modify-user",
    description = ["Modifies an existing user in the Trackit system by updating their details."],
    usageHelpWidth = 500,
    mixinStandardHelpOptions = true,
    footer = [
        "Examples:",
        "  trackit modify-user --name alice --password currentPass --new-password newPass --new-mail alice@newmail.com",
        "  trackit modify-user --name bob --password oldPass --new-phone 1234567890",
        "Notes:",
        "  - Password fields are interactive if not provided.",
        "  - If the '--delete-previous-roles' flag is used, the user will lose their previous roles.",
        "  - Multiple roles can be assigned at once using the '--role' option."
    ]
)
class UserModifyCommand : TrackitCommand() {

    /** The username of the user to modify. */
    @Option(names = ["--name", "-n"], description = ["Name of the user to modify"], interactive = true)
    var name: String = ""

    /** Current password for authentication. */
    @Option(
        names = ["--password", "-p"],
        description = ["Password of the user to modify"],
        interactive = true,
        echo = false
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

        askUserInteractiveData()

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

    private fun askUserInteractiveData() {
        if (name.isBlank()) {
            do {
                name = askForUsername()

                if (name.isBlank()) Logger.error("Username is required.")
            } while (name.isBlank())
        }

        if (password.isBlank()) {
            do {
                password = askForPassword()

                if (password.isBlank()) Logger.error("Password is required.")
            } while (password.isBlank())
        }
    }
}
