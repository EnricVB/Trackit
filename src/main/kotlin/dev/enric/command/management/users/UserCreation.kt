package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.UserCreationHandler
import dev.enric.logger.Logger
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.Console

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
 *     trackit user-create -n Alice -m alice@example.com -P 123456789 -r Developer Tester
 *
 *   - Create user interactively:
 *     trackit user-create -n Bob -p secretPass -r Admin
 */
@Command(
    name = "user-create",
    description = ["Create a new user account with optional contact info and role assignment."],
    footer = [
        "",
        "Description:",
        "  Creates a user with a unique name, password, optional email/phone, and assigned roles.",
        "  If password is omitted, it will be prompted interactively (if supported).",
        "",
        "Examples:",
        "  trackit user-create -n Alice -p secretPass -m alice@example.com -P 123456789 -r Developer Tester",
        "  trackit user-create -n Bob -p myPassword -r Admin",
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
    @Option(names = ["--name", "-n"], description = ["User name."], required = true)
    var name: String = ""

    /**
     * The password for the user.
     * If omitted, the password will be prompted interactively (if environment supports it).
     */
    @Option(names = ["--password", "-p"], description = ["User password."], interactive = true)
    var password: String? = null

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

        // Handle interactive password input if none was provided via argument
        password = assignInteractivePassword() ?: return 1

        val handler = UserCreationHandler(
            name,
            password!!,
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

    /**
     * Prompts the user to enter a password interactively via the console.
     * Returns the entered password or null if interactive input is not possible.
     */
    fun assignInteractivePassword(): String? {
        if (password.isNullOrBlank()) {
            val console: Console? = System.console()

            return if (console != null) {
                // Read password securely without echoing to console
                String(console.readPassword("Enter password: "))
            } else {
                Logger.error("Password is required but cannot be read in this environment")
                null
            }
        }

        return password
    }
}