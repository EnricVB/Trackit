package dev.enric.command.management.roles

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.management.roles.RoleListHandler
import picocli.CommandLine.Command

@Command(
    name = "role-list",
    description = ["List all roles"]
)
class RoleList : TrackitCommand() {

    override fun call(): Int {
        super.call()

        RoleListHandler().listRoles()

        return 0
    }
}