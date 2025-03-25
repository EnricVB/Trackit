package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.UserCreationHandler
import dev.enric.logger.Logger
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Command to create a new user in the Trackit system.
 *
 * This command allows administrators to create a user account with a specified name,
 * password, and optional contact information (email and phone). Additionally, one or
 * more roles can be assigned at creation.
 *
 * Passwords can be provided interactively if not passed as an argument.
 *
 * Usage examples:
 *   - Create user with password argument:
 *     trackit create-user -n Alice -m alice@example.com -P 123456789 -r Developer Tester
 *
 *   - Create user interactively:
 *     trackit create-user -n Bob -p secretPass -r Admin
 */
@Command(
    name = "create-user",
    description = ["Create a new user account with optional contact info and role assignment."],
    footer = [
        "",
        "Description:",
        "  Creates a user with a unique name, password, optional email/phone, and assigned roles.",
        "  If password is omitted, it will be prompted interactively (if supported).",
        "",
        "Examples:",
        "  trackit create-user -n Alice -p secretPass -m alice@example.com -P 123456789 -r Developer Tester",
        "  trackit create-user -n Bob -p myPassword -r Admin",
        "",
        "Notes:",
        "  - Use '-r' followed by one or more roles separated by space.",
        "  - Password entered interactively will not be echoed to the terminal.",
        ""
    ],
    mixinStandardHelpOptions = true,
)
class UserCreation : TrackitCommand() {

    /**
     * The username for the new user.
     * Must be unique within the system.
     */
    @Option(names = ["--name", "-n"], description = ["User name."], interactive = true)
    var name: String = ""

    /**
     * The password for the user.
     * If omitted, the password will be prompted interactively (if environment supports it).
     */
    @Option(names = ["--password", "-p"], description = ["User password."], interactive = true, echo = false)
    var password: String = ""

    /**
     * Optional email contact for the user.
     */
    @Option(names = ["--mail", "-m"], description = ["User mail contact"], required = false)
    var mail: String = ""

    /**
     * Optional phone contact for the user.
     */
    @Option(names = ["--phone", "-P"], description = ["User phone contact"], required = false)
    var phone: String = ""

    /**
     * Roles to assign to the user upon creation.
     * Multiple roles can be assigned by separating them with spaces.
     *
     * Example: -r Admin Developer
     */
    @Option(
        names = ["--role", "-r"],
        description = ["Assign roles"],
        split = " "
    )
    var roles: Array<String> = emptyArray()

    /**
     * Executes the user creation process.
     * Validates inputs, prompts for password if necessary, and delegates to the handler.
     */
    override fun call(): Int {
        super.call()

        askUserInteractiveData()

        val handler = UserCreationHandler(
            name,
            password,
            mail,
            phone,
            roles,
            sudoArgs
        )

        // Will return 1 as it will throw an exception if the user cannot be created
        if (!handler.checkCanCreateUser()) {
            return 1
        }

        handler.createUser()

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