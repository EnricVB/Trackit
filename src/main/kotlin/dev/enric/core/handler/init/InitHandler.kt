package dev.enric.core.handler.init

import dev.enric.core.objects.Role
import dev.enric.core.objects.permission.RolePermission
import dev.enric.util.RepositoryFolderManager

object InitHandler {

    fun init() {
        RepositoryFolderManager().createRepositoryFolder()

        createDefaultRoles()
    }

    private fun createDefaultRoles() {
        val ownerPermissions = RolePermission("musa").encode(true).first
        val projectManagerPermissions = RolePermission("musa").encode(true).first
        val undefinedPermissions = RolePermission("----").encode(true).first

        Role("owner", 1, mutableListOf(ownerPermissions)).encode(true).first
        Role("projectManager", 2, mutableListOf(projectManagerPermissions)).encode(true).first
        Role("undefined", Int.MAX_VALUE, mutableListOf(undefinedPermissions)).encode(true).first
    }
}