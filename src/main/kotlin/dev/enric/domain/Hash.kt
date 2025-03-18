@file:Suppress("unused")

package dev.enric.domain

import io.github.rctcwyvrn.blake3.Blake3
import java.io.Serializable

/**
 * This class represents a Hash that is going to be used to store the objects in the repository.
 * The Hash is 16 characters long string that is generated using the Blake3 algorithm.
 */
data class Hash(val string: String) : Serializable {

    /**
     * Operator that concatenates two Hash objects.
     */
    operator fun plus(other: Hash): Hash {
        return Hash(string + other.string)
    }

    override fun toString(): String {
        return string
    }

    override fun equals(other: Any?): Boolean {
        return other is Hash && string == other.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    companion object {
        private fun blake3Hash(data: ByteArray, length: Int = 16): String {
            val hasher = Blake3.newInstance()
            hasher.update(data)
            return hasher.hexdigest(length)
        }

        /**
         * Hashes a string using Blake3.
         */
        @JvmStatic
        fun parseText(text: String, length: Int = 16): Hash {
            return Hash(blake3Hash(text.toByteArray(), length))
        }

        /**
         * Hashes a byte array using Blake3.
         */
        @JvmStatic
        fun parse(data: ByteArray, length: Int = 16): Hash {
            return Hash(blake3Hash(data, length))
        }

        /**
         * Returns an empty Hash object with a string of 32 zeros.
         */
        @JvmStatic
        fun empty32(): Hash {
            return Hash("0".repeat(32))
        }
    }

    /**
     * Enum class that represents the different types of Hash objects.
     * The HashType is used to identify the type of the object that is going to be stored in the repository.
     *
     * Each HashType is computed using the Blake3 algorithm the same way as the Hash object.
     */
    enum class HashType(val hash: Hash) {
        CONTENT(parseText("Content", 1)),                   // cd
        TREE(parseText("Tree", 1)),                         // 89
        COMMIT(parseText("Commit", 1)),                     // c0
        SIMPLE_TAG(parseText("SimpleTag", 1)),              //
        COMPLEX_TAG(parseText("ComplexTag", 1)),            //
        USER(parseText("User", 1)),                         // 7b
        BRANCH(parseText("Branch", 1)),                     // 21
        REMOTE(parseText("Remote", 1)),                     //
        ROLE(parseText("Role", 1)),                         // d4
        BRANCH_PERMISSION(parseText("BranchPermission", 1)),// 71
        ROLE_PERMISSION(parseText("RolePermission", 1))     // 8d
    }
}