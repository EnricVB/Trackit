package dev.enric.command.gestion.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.users.UserListHandler
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