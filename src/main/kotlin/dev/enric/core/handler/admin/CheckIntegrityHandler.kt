package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.objects.*
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.domain.objects.remote.Remote
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.exists
import kotlin.io.path.pathString

class CheckIntegrityHandler : CommandHandler() {

    fun checkObjectIntegrity(objectHash : Hash) : Boolean {
        val hashType = HashType.fromHash(objectHash)

        return when (hashType) {
            CONTENT -> Content.checkIntegrity(objectHash)
            TREE -> Tree.checkIntegrity(objectHash)
            COMMIT -> Commit.checkIntegrity(objectHash)
            SIMPLE_TAG -> SimpleTag.checkIntegrity(objectHash)
            COMPLEX_TAG -> ComplexTag.checkIntegrity(objectHash)
            USER -> User.checkIntegrity(objectHash)
            BRANCH -> Branch.checkIntegrity(objectHash)
            REMOTE -> Remote.checkIntegrity(objectHash)
            ROLE -> Role.checkIntegrity(objectHash)
            BRANCH_PERMISSION -> BranchPermission.checkIntegrity(objectHash)
            ROLE_PERMISSION -> RolePermission.checkIntegrity(objectHash)
        }
    }

    fun checkObjectTypeIntegrity(objectType : HashType) : Boolean {
        val repositoryFolderManager = RepositoryFolderManager()
        val objectsFolder = repositoryFolderManager.getObjectsFolderPath()

        // Check if the objects folder exists and is a directory
        val objectTypeFolder = objectsFolder.resolve(objectType.hash.string)
        if (!objectTypeFolder.exists()) {
            Logger.error("Object type folder does not exist: ${objectTypeFolder.pathString}")
            return false
        }

        // Check all objects in the folder
        var result = true

        objectTypeFolder.toFile().listFiles()?.forEach {
            result = checkObjectIntegrity(Hash(it.name))

            if (!result) {
                Logger.error("Integrity check failed for object: ${it.name}")
            } else {
                Logger.debug("Integrity check passed for object: ${it.name}")
            }
        }

        return result
    }

    fun checkAllIntegrity() : Boolean {
        var result = true

        HashType.entries.forEach { hashType ->
            result = checkObjectTypeIntegrity(hashType)

            if (!result) {
                Logger.error("Integrity check failed for type: ${hashType.name}")
            } else {
                Logger.debug("Integrity check passed for type: ${hashType.name}")
            }
        }

        return result
    }
}
