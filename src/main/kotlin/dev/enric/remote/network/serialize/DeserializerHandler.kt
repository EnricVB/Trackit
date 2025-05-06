package dev.enric.remote.network.serialize

import dev.enric.domain.Hash.HashType
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.TrackitObject
import dev.enric.domain.objects.*
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import java.io.ObjectInputStream

object DeserializerHandler {
    private val deserializers = mutableMapOf<HashType, (ByteArray) -> TrackitObject<*>>()

    fun registerDeserializer(hashType: HashType, deserializer: (ByteArray) -> TrackitObject<*>) {
        deserializers[hashType] = deserializer
    }

    fun deserialize(hashType: HashType, data: ByteArray): TrackitObject<*> {
        val deserializer = deserializers[hashType]
            ?: throw IllegalArgumentException("No deserializer registered for hash type: $hashType")
        return deserializer(data)
    }

    init {
        registerDeserializer(CONTENT) { data ->
            val decompressedData = Content().decompressContent(data)?: return@registerDeserializer Content()
            return@registerDeserializer Content(decompressedData)
        }

        registerDeserializer(TREE) { data ->
            val decompressedData = Tree().decompressContent(data)?: return@registerDeserializer Tree()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as Tree
        }

        registerDeserializer(COMMIT) { data ->
            val decompressedData = Commit().decompressContent(data)?: return@registerDeserializer Commit()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as Commit
        }

        registerDeserializer(SIMPLE_TAG) { data ->
            val decompressedData = SimpleTag().decompressContent(data)?: return@registerDeserializer SimpleTag()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as SimpleTag
        }

        registerDeserializer(COMPLEX_TAG) { data ->
            val decompressedData = ComplexTag().decompressContent(data)?: return@registerDeserializer ComplexTag()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as ComplexTag
        }

        registerDeserializer(USER) { data ->
            val decompressedData = User().decompressContent(data)?: return@registerDeserializer User()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as User
        }

        registerDeserializer(BRANCH) { data ->
            val decompressedData = Branch().decompressContent(data)?: return@registerDeserializer Branch()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as Branch
        }

        registerDeserializer(ROLE) { data ->
            val decompressedData = Role().decompressContent(data)?: return@registerDeserializer Role()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as Role
        }

        registerDeserializer(BRANCH_PERMISSION) { data ->
            val decompressedData = BranchPermission().decompressContent(data)?: return@registerDeserializer BranchPermission()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as BranchPermission
        }

        registerDeserializer(ROLE_PERMISSION) { data ->
            val decompressedData = RolePermission().decompressContent(data)?: return@registerDeserializer RolePermission()
            val objectIStream = ObjectInputStream(decompressedData.inputStream())

            return@registerDeserializer objectIStream.readObject() as RolePermission
        }
    }
}