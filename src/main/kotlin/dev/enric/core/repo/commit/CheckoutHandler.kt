package dev.enric.core.repo.commit

import dev.enric.core.Hash
import dev.enric.domain.Commit
import dev.enric.domain.Content
import dev.enric.domain.Tree
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class CheckoutHandler(
    val commit: Commit,
    val sudoArgs: Array<String>? = null
) {
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