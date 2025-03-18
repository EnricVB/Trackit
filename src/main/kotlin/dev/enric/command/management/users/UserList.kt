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
    name = "user-list",
    description = ["List all users"]
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