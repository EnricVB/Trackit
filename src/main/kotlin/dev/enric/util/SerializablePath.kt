package dev.enric.util

import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths

data class SerializablePath(val path: String) : Serializable {
    fun toPath(): Path = Paths.get(path)

    override fun toString(): String {
        return path
    }

    companion object {
        fun of(s: String): SerializablePath {
            return SerializablePath(s)
        }
    }
}
