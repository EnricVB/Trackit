package dev.enric.util.repository

import dev.enric.core.security.SecretKey
import dev.enric.util.common.Utility
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions

/**
 * This class is responsible for managing the repository folder structure.
 * @param initFolder The folder where the repository will be created.
 * @property trackitFolder The folder where the repository will be created.
 * @property logsFolder The folder where the logs will be stored.
 * @property objectsFolder The folder where the trackit objects will be stored.
 * @property indexFolder The folder where the index files will be stored.
 */
data class RepositoryFolderManager(val initFolder: Path = Path.of(System.getProperty("user.dir"))) {

    /**
     * Constants for the repository folder structure.
     * Most of the constants are the names of the folders and files that will be created.
     */
    companion object {
        const val TRACKIT_FOLDER = ".trackit"
        const val LOGS_FOLDER = "logs"
        const val OBJECTS_FOLDER = "objects"
        const val INDEX_FOLDER = "index"
        const val CONFIG_FILE = "config.cfg"
        const val STAGING_INDEX = "staging.index"
        const val CURRENT_COMMIT = "CURRENT_COMMIT"
        const val REMOTE_POINTER = "REMOTE_POINTER"
        const val USER_INDEX = "USER_INDEX"
        const val BRANCH_HEAD = "BRANCH_HEAD"
        const val TAG_INDEX = "TAG_INDEX"
        const val PERMISSION_INDEX = "PERMISSION_INDEX"
    }

    private val trackitFolder: Path by lazy { initFolder.resolve(TRACKIT_FOLDER) }
    private val ignoreFile: Path by lazy { initFolder.resolve(".ignore") }
    private val secretKey: Path by lazy { initFolder.resolve("key.secret") }

    private val logsFolder: Path by lazy { trackitFolder.resolve(LOGS_FOLDER) }
    private val commandLogsFile: Path by lazy { logsFolder.resolve("command-${Utility.getLogDateFormat("yyyy-MM-dd")}.txt") }

    private val objectsFolder: Path by lazy { trackitFolder.resolve(OBJECTS_FOLDER) }
    private val indexFolder: Path by lazy { trackitFolder.resolve(INDEX_FOLDER) }

    /**
     * Create the repository folder structure.
     * This method creates the folders and files that are needed to store the repository data.
     */
    fun createRepositoryFolder() {
        initFolder.toFile().mkdir()

        trackitFolder.toFile().mkdir()
        logsFolder.toFile().mkdir()
        objectsFolder.toFile().mkdir()
        indexFolder.toFile().mkdir()

        ignoreFile.toFile().createNewFile()
        secretKey.toFile().createNewFile()

        getConfigFilePath().toFile().createNewFile()
        getStagingIndexPath().toFile().createNewFile()
        getCurrentCommitPath().toFile().createNewFile()
        getRemotePointerPath().toFile().createNewFile()
        getBranchHeadPath().toFile().createNewFile()
        getTagIndexPath().toFile().createNewFile()
        getPermissionIndexPath().toFile().createNewFile()

        assignSecretKey()
    }

    fun assignSecretKey() {
        Files.writeString(secretKey, SecretKey.generateKey())
        Files.writeString(ignoreFile, "key.secret\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)

        // Assign permissions to the secret key file, so only the owner can read and write it.
        try {
            if (Files.getFileStore(secretKey).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(secretKey, PosixFilePermissions.fromString("rw-------"))
            } else {
                secretKey.toFile().setReadable(true, true)
                secretKey.toFile().setWritable(true, true)
            }
        } catch (e: Exception) {
            println("Error setting permissions: ${e.message}")
        }
    }

    fun getTrackitFolderPath(): Path {
        return trackitFolder
    }

    fun getLogsFolderPath(): Path {
        return logsFolder
    }

    fun getCommandLogsFilePath(): Path {
        return commandLogsFile
    }

    fun getObjectsFolderPath(): Path {
        return objectsFolder
    }

    fun getIndexFolderPath(): Path {
        return indexFolder
    }

    fun getInitFolderPath(): Path {
        return initFolder
    }

    fun getConfigFilePath(): Path {
        return getTrackitFolderPath().resolve(CONFIG_FILE)
    }

    fun getStagingIndexPath(): Path {
        return getTrackitFolderPath().resolve(STAGING_INDEX)
    }

    fun getCurrentCommitPath(): Path {
        return getIndexFolderPath().resolve(CURRENT_COMMIT)
    }

    fun getRemotePointerPath(): Path {
        return getIndexFolderPath().resolve(REMOTE_POINTER)
    }

    fun getBranchHeadPath(): Path {
        return getIndexFolderPath().resolve(BRANCH_HEAD)
    }

    fun getTagIndexPath(): Path {
        return getIndexFolderPath().resolve(TAG_INDEX)
    }

    fun getPermissionIndexPath(): Path {
        return getIndexFolderPath().resolve(PERMISSION_INDEX)
    }

    fun getIgnorePath(): Path {
        return getInitFolderPath().resolve(".ignore")
    }

    fun getSecretKeyPath(): Path {
        return secretKey
    }
}