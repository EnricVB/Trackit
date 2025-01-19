package dev.enric

import dev.enric.core.objects.Content
import dev.enric.util.RepositoryFolderManager
import java.nio.file.Paths

class Main {
    companion object {
        @JvmStatic
        val repository = RepositoryFolderManager(Paths.get("D:\\test"))
    }
}

fun main() {
    Main.repository.createRepositoryFolder()

    val content = Content("Hello, World!")
    val encodedContent = content.encode(true)

    val decodedContent = content.decode(encodedContent.first)
    println(decodedContent.printInfo())
}