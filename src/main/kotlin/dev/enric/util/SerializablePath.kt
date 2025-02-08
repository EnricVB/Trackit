package dev.enric.util

import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * SerializablePath is a wrapper class for Path that allows it to be serialized.
 */
data class SerializablePath(val pathString: String) : Serializable {

    constructor(path: Path) : this(path.toString())

    /**
     * @param rootPath The root path to be removed from the path.
     * @return The relative path of the file.
     */
    fun relativePath(rootPath: Path): Path = rootPath.normalize().relativize(toPath())

    fun toPath(): Path = Paths.get(pathString)

    override fun toString(): String { return pathString }

    override fun hashCode(): Int { return pathString.hashCode() }

    override fun equals(other: Any?): Boolean {
        return other is SerializablePath && toPath() == other.toPath()
    }

    companion object {
        fun of(s: String): SerializablePath = SerializablePath(s)
        fun of(p: Path): SerializablePath = SerializablePath(p)
    }
}
