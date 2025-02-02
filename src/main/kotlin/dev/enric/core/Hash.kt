@file:Suppress("unused")

package dev.enric.core

import io.github.rctcwyvrn.blake3.Blake3
import java.io.Serializable

/**
 * This class represents a Hash that is going to be used to store the objects in the repository.
 * The Hash is 16 characters long string that is generated using the Blake3 algorithm.
 */
data class Hash(val hash: String) : Serializable {
    /**
     * Constructor that checks if the hash is 16 characters long.
     */
    init {
        requireNotNull(hash.length == 32) { "Hash must have 16 characters" }
    }

    /**
     * Operator that concatenates two Hash objects.
     */
    operator fun plus(other: Hash): Hash {
        return Hash(hash + other.hash)
    }

    override fun toString(): String {
        return hash
    }

    companion object {
        /**
         * Parses a text into a Hash object. The text is going to be hashed using the Blake3 algorithm.
         * @param hashData String that is going to be parsed into a Hash object.
         * @return 16 length long Hash object that represents the hashed text.
         */
        @JvmStatic
        fun parseText(hashData: String): Hash {
            val hasher = Blake3.newInstance()
            hasher.update(hashData.toByteArray())

            return Hash(hasher.hexdigest(16))
        }

        /**
         * Parses a text into a Hash object. The text is going to be hashed using the Blake3 algorithm.
         * @param hashData String that is going to be parsed into a Hash object.
         * @param length Int that indicates the length of the Hash object.
         * @return Hash object that represents the hashed text.
         */
        @JvmStatic
        fun parseText(hashData: String, length: Int): Hash {
            val hasher = Blake3.newInstance()
            hasher.update(hashData.toByteArray())

            return Hash(hasher.hexdigest(length))
        }
    }

    /**
     * Enum class that represents the different types of Hash objects.
     * The HashType is used to identify the type of the object that is going to be stored in the repository.
     *
     * Each HashType is computed using the Blake3 algorithm the same way as the Hash object.
     */
    enum class HashType(val hash: Hash) {
        CONTENT(parseText("Content", 1)),
        TREE(parseText("Tree", 1)),
        COMMIT(parseText("Commit", 1)),
        SIMPLE_TAG(parseText("SimpleTag", 1)),
        COMPLEX_TAG(parseText("ComplexTag", 1)),
        USER(parseText("User", 1)),
        BRANCH(parseText("Branch", 1)),
        REMOTE(parseText("Remote", 1)),
        ROLE(parseText("Role", 1)),
        BRANCH_PERMISSION(parseText("BranchPermission", 1)),
        ROLE_PERMISSION(parseText("RolePermission", 1))
    }
}