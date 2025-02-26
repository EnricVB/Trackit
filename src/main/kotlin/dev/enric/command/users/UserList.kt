package dev.enric.command.users

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.users.UserCreationHandler
import dev.enric.core.handler.users.UserListHandler
import dev.enric.logger.Logger
import dev.enric.util.AuthUtil
import picocli.CommandLine.Command
import picocli.CommandLine.Option

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