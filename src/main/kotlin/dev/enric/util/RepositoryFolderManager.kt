package dev.enric.util

import java.nio.file.Path

data class RepositoryFolderManager(val initFolder: Path) {

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

    private val repositoryFolder: Path by lazy { initFolder.resolve(TRACKIT_FOLDER) }
    private val logsFolder: Path by lazy { repositoryFolder.resolve(LOGS_FOLDER) }
    private val objectsFolder: Path by lazy { repositoryFolder.resolve(OBJECTS_FOLDER) }
    private val indexFolder: Path by lazy { repositoryFolder.resolve(INDEX_FOLDER) }


    fun createRepositoryFolder() {
        initFolder.toFile().mkdir()

        repositoryFolder.toFile().mkdir()
        logsFolder.toFile().mkdir()
        objectsFolder.toFile().mkdir()
        indexFolder.toFile().mkdir()

        getConfigFilePath().toFile().createNewFile()
        getStagingIndexPath().toFile().createNewFile()
        getCurrentCommitPath().toFile().createNewFile()
        getRemotePointerPath().toFile().createNewFile()
        getUserIndexPath().toFile().createNewFile()
        getBranchHeadPath().toFile().createNewFile()
        getTagIndexPath().toFile().createNewFile()
        getPermissionIndexPath().toFile().createNewFile()
    }

    fun getRepositoryFolderPath(): Path { return repositoryFolder }

    fun getLogsFolderPath(): Path { return logsFolder }

    fun getObjectsFolderPath(): Path { return objectsFolder }

    fun getIndexFolderPath(): Path { return indexFolder }

    fun getInitFolderPath(): Path { return initFolder }

    fun getTrackitFolderPath(): Path { return getRepositoryFolderPath().resolve(TRACKIT_FOLDER) }

    fun getConfigFilePath(): Path { return getRepositoryFolderPath().resolve(CONFIG_FILE) }

    fun getStagingIndexPath(): Path { return getRepositoryFolderPath().resolve(STAGING_INDEX) }

    fun getCurrentCommitPath(): Path { return getIndexFolderPath().resolve(CURRENT_COMMIT) }

    fun getRemotePointerPath(): Path { return getIndexFolderPath().resolve(REMOTE_POINTER) }

    fun getUserIndexPath(): Path { return getIndexFolderPath().resolve(USER_INDEX) }

    fun getBranchHeadPath(): Path { return getIndexFolderPath().resolve(BRANCH_HEAD) }

    fun getTagIndexPath(): Path { return getIndexFolderPath().resolve(TAG_INDEX) }

    fun getPermissionIndexPath(): Path { return getIndexFolderPath().resolve(PERMISSION_INDEX) }
}