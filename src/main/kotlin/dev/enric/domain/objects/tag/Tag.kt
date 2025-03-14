package dev.enric.domain.objects.tag

import dev.enric.domain.Hash

interface Tag {
    val name : String
    val commit : Hash?
}