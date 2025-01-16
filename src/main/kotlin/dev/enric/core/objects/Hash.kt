package dev.enric.core.objects

import io.github.rctcwyvrn.blake3.Blake3

data class Hash(val hash : String) {
    init {
        requireNotNull(hash.length == 32) { "Hash must have 16 characters" }
    }

    companion object {
        @JvmStatic
        fun parseText(hashData : String) : Hash {
            val hasher = Blake3.newInstance()
            hasher.update(hashData.toByteArray())

            return Hash(hasher.hexdigest(16))
        }
    }
}