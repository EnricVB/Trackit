package dev.enric.cli.repository

import dev.enric.cli.CommandTest
import dev.enric.core.handler.repo.InitHandler
import dev.enric.core.security.PasswordHash
import dev.enric.domain.objects.Branch
import dev.enric.domain.objects.User
import dev.enric.logger.Logger
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.index.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class InitCommandCommandTest : CommandTest() {

    companion object {
        // Given
        const val USERNAME = "test"
        const val MAIL = "test@mail.com"
        const val PHONE = "123456789"
        const val PASSWORD = "password"
    }

    @Before
    fun setUpInput() {
        SystemConsoleInput.setInput("$USERNAME\n$PASSWORD\n$MAIL\n$PHONE")
    }

    @Test
    fun `User is created correctly with specified params`() {
        Logger.info("Executing test: User is created correctly with specified params\n")

        // When
        InitHandler().init()

        // Then
        // Check only one user is created
        assertEquals(1, UserIndex.getAllUsers().size)

        // Check the user is created with the correct data
        val user = User.newInstance(UserIndex.getAllUsers().first())
        assertEquals(USERNAME, user.name)
        assertEquals(MAIL, user.mail)
        assertEquals(PHONE, user.phone)
        assertEquals(PasswordHash.hash(PASSWORD, user.salt), user.password)
    }

    @Test
    fun `Main branch is created correctly`() {
        Logger.info("Executing test: Main branch is created correctly\n")

        // When
        InitHandler().init()

        // Then
        // Check only one branch is created
        assertEquals(1, BranchIndex.getAllBranches().size)

        // Check the branch is created with the correct data
        val branch = Branch.newInstance(BranchIndex.getAllBranches().first())
        assertEquals("main", branch.name)
    }

    @Test
    fun `Default roles has been created correctly`() {
        Logger.info("Executing test: Default roles has been created correctly\n")

        // When
        InitHandler().init()

        // Then
        // Check the roles are created
        assertEquals(3, RoleIndex.getAllRoles().size)

        // Check the roles are created with the correct data

        /* Check the owner role */
        val branchPermission = BranchPermissionIndex.getBranchPermission("rw", "main") ?: error("RW Branch main permission not found")
        val musaRolePermission = RolePermissionIndex.getRolePermission("musa") ?: error("Role permission musa not found")
        val noneRolePermission = RolePermissionIndex.getRolePermission("----") ?: error("Role permission ---- not found")

        val owner = RoleIndex.getRoleByName("owner") ?: error("Role owner not found")

        assertEquals("owner", owner.name)       // Check the role name
        assertEquals(1, owner.permissionLevel)  // Check the role permission level
        assertContains(owner.permissions, musaRolePermission.generateKey())    // Check the role permissions
        assertContains(owner.permissions, branchPermission.generateKey())      // Check the role permissions

        /* Check the projectManager role */

        val projectManager = RoleIndex.getRoleByName("projectManager") ?: error("Role projectManager not found")

        assertEquals("projectManager", projectManager.name)        // Check the role name
        assertEquals(2, projectManager.permissionLevel)           // Check the role permission level
        assertContains(projectManager.permissions, musaRolePermission.generateKey())   // Check the role permissions
        assertContains(projectManager.permissions, branchPermission.generateKey())    // Check the role permissions

        /* Check the undefined role */

        val undefined = RoleIndex.getRoleByName("undefined") ?: error("Role undefined not found")

        assertEquals("undefined", undefined.name)       // Check the role name
        assertEquals(Int.MAX_VALUE, undefined.permissionLevel)  // Check the role permission level
        assertFalse { undefined.permissions.contains(branchPermission.generateKey()) }  // Check does not have branch permission
        assertContains(undefined.permissions, noneRolePermission.generateKey())        // Check the role permissions
    }
}