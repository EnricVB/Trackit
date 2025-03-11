package dev.enric.core.repo.commit

import dev.enric.core.Hash
import dev.enric.domain.Commit
import dev.enric.domain.Content
import dev.enric.domain.Tree
import dev.enric.util.repository.RepositoryFolderManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

class CheckoutHandler(
    val commit: Commit,
    val sudoArgs: Array<String>? = null
) {

    /**
     * Deletes all files and folders from Working Space except .trackit folder and key.secret file.
     *
     * This method is called before checking out a commit to avoid conflicts with the current state of the repository.
     */
    @OptIn(ExperimentalPathApi::class)
    fun preCheckout() {
        val repositoryFolderManager = RepositoryFolderManager()

        repositoryFolderManager.getInitFolderPath()
            .walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .forEach {
                val isTrackitFolder = it.toRealPath().startsWith(repositoryFolderManager.getTrackitFolderPath())
                val isSecretKey = it.toRealPath().startsWith(repositoryFolderManager.getSecretKeyPath())
                val isRootFolder = it.toRealPath() == repositoryFolderManager.getInitFolderPath()

                val isValidFolder = !isTrackitFolder && !isSecretKey && !isRootFolder
                val canDelete = it.toFile().exists()

                if (isValidFolder && canDelete) {
                    it.toFile().deleteRecursively()
                }
            }
    }

    /**
     * Checks out a specific commit by restoring its tree structure and content.
     */
    fun checkout() {
        commit.tree.forEach {
            val tree = Tree.newInstance(it)
            Path.of(tree.serializablePath.pathString).parent.toFile().mkdirs()

            if (!tree.serializablePath.toPath().toFile().exists()) {
                Files.writeString(
                    tree.serializablePath.toPath(),
                    String(Content.newInstance(tree.hash).content),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                )
            }
        }
    }
}