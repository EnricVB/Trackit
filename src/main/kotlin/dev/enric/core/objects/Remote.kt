package dev.enric.core.objects

import dev.enric.Main
import dev.enric.core.Hash
import dev.enric.core.Hash.HashType.REMOTE
import dev.enric.core.TrackitObject
import java.io.Serializable
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant

data class Remote(
    val protocol: DataProtocolEnum? = null,
    val type: RemoteTypeEnum? = null
) : TrackitObject<Remote>(), Serializable {

    override fun decode(hash: Hash): Remote {
        val commitFolder = Main.repository.getObjectsFolderPath().resolve(hash.toString().take(2))
        val objectFile = commitFolder.resolve(hash.toString())
        val decompressedData = decompressContent(Files.readAllBytes(objectFile)) ?: return Remote() // If the file is empty, return an empty commit

        val byteArrayInputStream = decompressedData.inputStream()
        val objectIStream = java.io.ObjectInputStream(byteArrayInputStream)

        return objectIStream.readObject() as Remote
    }

    override fun generateKey(): Hash {
        val instantNow = Timestamp.from(Instant.now())
        val hashData = Hash.parseText("${instantNow};${toString()};${protocol.toString()};${type.toString()}", 15)

        return REMOTE.hash.plus(hashData)
    }

    override fun printInfo(): String {
        return "Remote(protocol=$protocol, type=$type)"
    }

    override fun showDifferences(newer: Hash, oldest: Hash): String {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun newInstance(hash : Hash) : Remote {
            return Remote().decode(hash)
        }
    }
}


enum class DataProtocolEnum(
    var user: String? = null,
    var host: String? = null,
    var port: Int? = null,
    var path: String? = null
) : Serializable {

    SSH,
    LOCAL;

    override fun toString(): String {
        return when (this) {
            SSH -> "SSH://$user@$host:$port/$path"
            LOCAL -> "LOCAL://$user:/$path"
        }
    }

    companion object Factory {
        @JvmStatic
        fun newSSHInstance(user: String, host: String, port: Int, path: String): DataProtocolEnum {
            return SSH.apply {
                this.user = user
                this.host = host
                this.port = port
                this.path = path
            }
        }

        @JvmStatic
        fun newLocalInstance(user: String, path: String): DataProtocolEnum {
            return LOCAL.apply {
                this.user = user
                this.path = path
            }
        }
    }
}

enum class RemoteTypeEnum : Serializable {
    ORIGIN,
    DESTINATION
}