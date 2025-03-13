package dev.enric.util.index

import dev.enric.domain.Hash
import dev.enric.domain.Hash.HashType.*
import dev.enric.util.repository.RepositoryFolderManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

object ContentIndex {

    /**
     * Checks if a file has been committed before.
     *
     * @param hash The hash of the file to check.
     * @return True if the file is tracked, false otherwise.
     */
    fun isFileTracked(hash: Hash): Boolean {
        return getAllContent().contains(hash)
    }

    /**
     * Retrieves all the content objects.
     *
     * @return A list of [Hash] objects representing a content object.
     */
    @OptIn(ExperimentalPathApi::class)
    fun getAllContent(): List<Hash> {
        val contentFolder = RepositoryFolderManager().getObjectsFolderPath().resolve(CONTENT.hash.toString())

        return contentFolder.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
            !it.isDirectory()
        }.map {
            Hash(it.toString().substringAfterLast("\\" + USER.hash + "\\"))
        }.toList()
    }
}