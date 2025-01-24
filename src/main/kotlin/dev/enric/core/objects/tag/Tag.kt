package dev.enric.core.objects.tag

import dev.enric.core.Hash

interface Tag {
    val name : String
    val commit : Hash?
}