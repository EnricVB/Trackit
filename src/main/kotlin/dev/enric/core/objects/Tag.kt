package dev.enric.core.objects

import dev.enric.core.Hash

interface Tag {
    val name : String
    val commit : Hash?
}