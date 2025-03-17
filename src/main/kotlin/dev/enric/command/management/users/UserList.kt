package dev.enric.command.management.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.users.UserListHandler
import picocli.CommandLine.Command

@Command(
    name = "user-list",
    description = ["List all users"]
)
class UserList : TrackitCommand() {

    override fun call(): Int {
        super.call()

        UserListHandler().listUsers()

        return 0
    }
}