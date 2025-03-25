package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.UserListHandler
import picocli.CommandLine.Command

/**
 * Command to list all users in the Trackit system.
 *
 * This command displays a summary of all registered users along with their
 * relevant information such as assigned roles, contact details, and permission levels.
 *
 * Usage example:
 *   trackit user-list
 */
@Command(
    name = "list-user",
    description = ["Displays all users registered in the system with their assigned roles and contact info."],
    footer = [
        "",
        "Description:",
        "  Lists all users along with:",
        "    - Assigned roles",
        "    - Permission levels",
        "    - Contact details (email and phone)",
        "",
    ],
    mixinStandardHelpOptions = true,
)
class UserList : TrackitCommand() {

    /**
     * Executes the user listing process.
     * Delegates the task to the UserListHandler which handles the formatting and output.
     *
     * @return Exit code: 0 if successful.
     */
    override fun call(): Int {
        super.call()

        UserListHandler().listUsers()

        return 0
    }
}