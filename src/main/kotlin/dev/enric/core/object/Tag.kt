package dev.enric.core.`object`

import dev.enric.core.Hash

interface Tag {
    val name : String
    val commit : Hash?
}