package dev.enric.domain.objects

import dev.enric.domain.Hash.HashType.USER
import dev.enric.core.security.PasswordHash
import dev.enric.domain.Hash
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.repository.RepositoryFolderManager
import java.io.Serializable
import java.nio.file.Files
import java.util.*

data class User(
    val name: String = "",
    var password: String = "",
    var salt: ByteArray? = null,
    var mail: String = "",
    var phone: String = "",
    val roles: MutableList<Hash> = mutableListOf()
) : TrackitObject<User>(), Serializable {

    override fun decode(hash: Hash): User {
        if (!hash.string.startsWith(USER.hash.string)) throw IllegalHashException("Hash $hash is not a User hash")

        val userFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = userFolder.resolve(hash.toString())

        if(!Files.exists(objectFile)) throw IllegalHashException("User with hash $hash not found")
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
        val userDetails = buildString {
            appendLine(ColorUtil.title("User Details"))

            append(ColorUtil.label("  Hash: "))
            appendLine(ColorUtil.text(generateKey().toString()))

            append(ColorUtil.label("  Name: "))
            appendLine(
                if (name.isNotEmpty()) ColorUtil.text(name)
                else ColorUtil.message("No name assigned")
            )

            append(ColorUtil.label("  Mail: "))
            appendLine(
                if (mail.isNotEmpty()) ColorUtil.text(mail)
                else ColorUtil.message("No mail assigned")
            )

            append(ColorUtil.label("  Phone: "))
            appendLine(
                if (phone.isNotEmpty()) ColorUtil.text(phone)
                else ColorUtil.message("No phone assigned")
            )

            appendLine(
                roles.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { ColorUtil.text(Role.newInstance(it).printInfo()) }
                    ?: ColorUtil.message("No roles assigned")
            )
        }

        return userDetails
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (password != other.password) return false
        if (salt != null) {
            if (other.salt == null) return false
            if (!salt.contentEquals(other.salt)) return false
        } else if (other.salt != null) return false
        if (mail != other.mail) return false
        if (phone != other.phone) return false
        if (roles != other.roles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + (salt?.contentHashCode() ?: 0)
        result = 31 * result + mail.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + roles.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID: Long = 1L

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
            val salt = PasswordHash.generateSalt()

            if (console != null) { // This is running in a terminal
                username = console.readLine("Enter username: ")
                mail = console.readLine("Enter mail: ")
                phone = console.readLine("Enter phone: ")
                password = String(console.readPassword("Enter password: "))
            } else { // This is running in an IDE
                val scanner = Scanner(System.`in`)
                Logger.log("Enter username: ")
                username = scanner.nextLine()
                Logger.log("Enter mail: ")
                mail = scanner.nextLine()
                Logger.log("Enter phone: ")
                phone = scanner.nextLine()
                Logger.log("Enter password: ")
                password = scanner.nextLine()
            }

            return User(username, PasswordHash.hash(password, salt), salt, mail, phone, rolesHash)
        }

        @JvmStatic
        fun createUser(username: String, password: String, mail: String, phone: String, roles: List<Role>): User {
            val rolesHash = roles.map { it.encode().first }.toMutableList()
            val salt = PasswordHash.generateSalt()

            return User(username, PasswordHash.hash(password, salt), salt, mail, phone, rolesHash)
        }
    }
}
