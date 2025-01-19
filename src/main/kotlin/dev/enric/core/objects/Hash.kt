@file:Suppress("unused")

package dev.enric.core.objects

import io.github.rctcwyvrn.blake3.Blake3

data class Hash(val hash: String) {
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
        TREE(parseText("Content", 1)),
        COMMIT(parseText("Content", 1)),
        SIMPLE_TAG(parseText("Content", 1)),
        COMPLEX_TAG(parseText("Content", 1)),
        USER(parseText("Content", 1)),
        BRANCH(parseText("Content", 1)),
        ROLE(parseText("Content", 1)),
        BRANCH_PERMISSION(parseText("Content", 1)),
        ROLE_PERMISSION(parseText("Content", 1))
    }
}