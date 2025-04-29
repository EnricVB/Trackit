package dev.enric.domain.objects

import dev.enric.core.security.PasswordHash
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.USER
import dev.enric.domain.TrackitObject
import dev.enric.exceptions.IllegalHashException
import dev.enric.logger.Logger
import dev.enric.util.common.ColorUtil
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.repository.RepositoryFolderManager
import java.io.ObjectInputStream
import java.io.Serializable
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

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

        if (!Files.exists(objectFile)) throw IllegalHashException("User with hash $hash not found")
        val decompressedData = decompressContent(Files.readAllBytes(objectFile))
            ?: return User() // If the file is empty, return an empty user

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = ObjectInputStream(byteArrayInputStream)

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
            val console = SystemConsoleInput.getInstance()

            val username = console.readLine("Enter username: ")
            val password = String(console.readPassword("Enter password: "))
            val mail = console.readLine("Enter mail: ")
            val phone = console.readLine("Enter phone: ")

            val rolesHash = roles.map { it.generateKey() }.toMutableList()
            val salt = PasswordHash.generateSalt()

            return User(username, PasswordHash.hash(password, salt), salt, mail, phone, rolesHash)
        }

        @JvmStatic
        fun createUser(username: String, password: String, mail: String, phone: String, roles: List<Role>): User {
            val rolesHash = roles.map { it.generateKey() }.toMutableList()
            val salt = PasswordHash.generateSalt()

            return User(username, PasswordHash.hash(password, salt), salt, mail, phone, rolesHash)
        }

        @JvmStatic
        fun checkIntegrity(objectHash: Hash): Boolean {
            val repositoryFolderManager = RepositoryFolderManager()
            val objectsFolder = repositoryFolderManager.getObjectsFolderPath()
            val objectFolder = objectsFolder.resolve(USER.hash.string)
            val objectFile = objectFolder.resolve(objectHash.string)

            return if (objectFile.exists()) {
                val decompressedData = User().decompressContent(objectFile.readBytes())
                    ?: return false // If the file is empty, return false
                val byteArrayInputStream = decompressedData.inputStream()
                val objectIStream = ObjectInputStream(byteArrayInputStream)

                val supposedContentHash = (objectIStream.readObject() as User).generateKey()

                // Check if the hash of the content matches the hash of the file
                if (Hash(objectFile.name) == supposedContentHash) {
                    Logger.debug("Integrity check passed for User: ${objectHash.string}")
                    true
                } else {
                    Logger.error("Integrity check failed for User: ${objectHash.string}")
                    false
                }
            } else {
                Logger.error("Content file does not exist: ${objectFile.pathString}")
                false
            }
        }
    }
}
