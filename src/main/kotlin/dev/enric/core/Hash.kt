@file:Suppress("unused")

package dev.enric.core

import io.github.rctcwyvrn.blake3.Blake3
import java.io.Serializable

data class Hash(val hash: String) : Serializable {
    init {
        requireNotNull(hash.length == 32) { "Hash must have 16 characters" }
    }

    operator fun plus(other: Hash): Hash {
        return Hash(hash + other.hash)
    }

    override fun toString(): String {
        return hash
    }

    companion object {
        @JvmStatic
        fun parseText(hashData: String): Hash {
            val hasher = Blake3.newInstance()
            hasher.update(hashData.toByteArray())

            return Hash(hasher.hexdigest(16))
        }

        @JvmStatic
        fun parseText(hashData: String, length: Int): Hash {
            val hasher = Blake3.newInstance()
            hasher.update(hashData.toByteArray())

            return Hash(hasher.hexdigest(length))
        }
    }

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