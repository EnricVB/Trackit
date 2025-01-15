package dev.enric.core

data class Hash(val hash : String) {
    init {
        requireNotNull(hash.length == 16) { "Hash must have 16 characters" }
    }
}