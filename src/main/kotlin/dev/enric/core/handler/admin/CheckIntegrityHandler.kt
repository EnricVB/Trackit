package dev.enric.core.handler.admin

import dev.enric.core.handler.CommandHandler
import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType
import dev.enric.domain.Hash.HashType.*
import dev.enric.domain.objects.*
import dev.enric.domain.objects.permission.BranchPermission
import dev.enric.domain.objects.permission.RolePermission
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.exists
import kotlin.io.path.pathString

/**
 * Handler responsible for performing integrity checks over all Trackit objects.
 *
 * This class provides utilities to:
 * - Check the integrity of a single object by hash.
 * - Check the integrity of all objects of a specific type.
 * - Check the integrity of all objects across all types.
 *
 * It delegates to the static `checkIntegrity` method defined in each object class.
 */
class CheckIntegrityHandler : CommandHandler() {

    /**
     * Verifies the integrity of a single object by using its hash.
     *
     * The method infers the object type from the hash, then delegates to the
     * corresponding object's `checkIntegrity` static method.
     *
     * @param objectHash the hash of the object to check.
     * @return true if the object's integrity is valid; false otherwise.
     */
    fun checkObjectIntegrity(objectHash: Hash): Boolean {
        val hashType = HashType.fromHash(objectHash)

        return when (hashType) {
            CONTENT -> Content.checkIntegrity(objectHash)
            TREE -> Tree.checkIntegrity(objectHash)
            COMMIT -> Commit.checkIntegrity(objectHash)
            SIMPLE_TAG -> SimpleTag.checkIntegrity(objectHash)
            COMPLEX_TAG -> ComplexTag.checkIntegrity(objectHash)
            USER -> User.checkIntegrity(objectHash)
            BRANCH -> Branch.checkIntegrity(objectHash)
            ROLE -> Role.checkIntegrity(objectHash)
            BRANCH_PERMISSION -> BranchPermission.checkIntegrity(objectHash)
            ROLE_PERMISSION -> RolePermission.checkIntegrity(objectHash)
        }
    }

    /**
     * Verifies the integrity of all objects within a given type folder.
     *
     * For each object file found under the folder corresponding to the given type,
     * it checks its integrity using [checkObjectIntegrity]. Logs detailed results.
     *
     * @param objectType the type of object to check.
     * @return true if all objects of this type pass the check; false if at least one fails.
     */
    fun checkObjectTypeIntegrity(objectType: HashType): Boolean {
        val repositoryFolderManager = RepositoryFolderManager()
        val objectsFolder = repositoryFolderManager.getObjectsFolderPath()

        val objectTypeFolder = objectsFolder.resolve(objectType.hash.string)
        if (!objectTypeFolder.exists()) {
            Logger.error("Object type folder does not exist: ${objectTypeFolder.pathString}")
            return false
        }

        var result = true

        objectTypeFolder.toFile().listFiles()?.forEach {
            val currentResult = checkObjectIntegrity(Hash(it.name))
            result = result && currentResult

            if (!currentResult) {
                Logger.error("Integrity check failed for object: ${it.name}")
            } else {
                Logger.debug("Integrity check passed for object: ${it.name}")
            }
        }

        return result
    }

    /**
     * Performs a full integrity check of the entire repository.
     *
     * Iterates through all known object types ([HashType.entries]) and
     * validates the integrity of each object found for each type.
     *
     * @return true if all object types and all their objects pass; false otherwise.
     */
    fun checkAllIntegrity(): Boolean {
        var result = true

        HashType.entries.forEach { hashType ->
            val currentResult = checkObjectTypeIntegrity(hashType)
            result = result && currentResult

            if (!currentResult) {
                Logger.error("Integrity check failed for type: ${hashType.name}\n")
            } else {
                Logger.debug("Integrity check passed for type: ${hashType.name}\n")
            }
        }

        return result
    }
}
