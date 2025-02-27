package dev.enric.core.objects

import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.USER
import dev.enric.core.TrackitObject
import dev.enric.util.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.util.*

data class User(
    val name: String = "",
    var password: Hash = Hash("0".repeat(32)),
    var mail: String = "",
    var phone: String = "",
    val roles: MutableList<Hash> = mutableListOf()
) : TrackitObject<User>(), Serializable {

    override fun decode(hash: Hash): User {
        val treeFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = treeFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return User() // If the file is empty, return an empty user

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as User
    }

    override fun generateKey(): Hash {
        val hashData = Hash.parseText(name, 15)

        return USER.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "User(name=$name, email=$mail, phone=$phone, roles=${roles.map { Role.newInstance(it).printInfo() }})"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash: Hash): User {
            return User().decode(hash)
        }

        @JvmStatic
        fun createUser(roles: List<Role>): User {
            val console = System.console()

            val username: String
            val mail: String
            val phone: String
            val password: String
            val rolesHash = roles.map { it.encode().first }.toMutableList()

            if (console != null) { // This is running in a terminal
                username = console.readLine("Enter username: ")
                mail = console.readLine("Enter mail: ")
                phone = console.readLine("Enter phone: ")
                password = String(console.readPassword("Enter password: "))
            } else { // This is running in an IDE
                val scanner = Scanner(System.`in`)
                println("Enter username: ")
                username = scanner.nextLine()
                println("Enter mail: ")
                mail = scanner.nextLine()
                println("Enter phone: ")
                phone = scanner.nextLine()
                println("Enter password: ")
                password = scanner.nextLine()
            }

            return User(username, Hash.parseText(password), mail, phone, rolesHash)
        }
    }
}
