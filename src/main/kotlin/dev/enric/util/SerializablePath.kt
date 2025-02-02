package dev.enric.util

import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

/**
 * SerializablePath is a wrapper class for Path that allows it to be serialized.
 */
data class SerializablePath(val path: String) : Serializable {
    fun toPath(): Path = Paths.get(path)

    override fun toString(): String {
        return path
    }

    /**
     * @param rootPath The root path to be removed from the path.
     * @return The relative path of the file.
     */
    fun relativePath(rootPath: Path): Path {
        return Path(toString().replace(rootPath.toString(), ""))
    }

    companion object {
        fun of(s: String): SerializablePath {
            return SerializablePath(s)
        }

        fun of(p: Path): SerializablePath {
            return SerializablePath(p.toString())
        }
    }
}
