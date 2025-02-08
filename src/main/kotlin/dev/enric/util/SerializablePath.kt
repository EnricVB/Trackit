package dev.enric.util

import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

/**
 * SerializablePath is a wrapper class for Path that allows it to be serialized.
 */
data class SerializablePath(val pathString: String) : Serializable {
    constructor(path: Path) : this(path.toString())

    fun toPath(): Path = Paths.get(pathString)

    override fun toString(): String {
        return pathString
    }

    /**
     * @param rootPath The root path to be removed from the path.
     * @return The relative path of the file.
     */
    fun relativePath(rootPath: Path): Path {
        return Path(toString().replace(rootPath.toString(), ""))
    }

    override fun hashCode(): Int {
        return pathString.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SerializablePath) {
            pathString == other.pathString
        } else {
            false
        }
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
