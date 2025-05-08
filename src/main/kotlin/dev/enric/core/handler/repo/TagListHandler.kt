package dev.enric.core.handler.repo

import dev.enric.domain.Hash.HashType.COMPLEX_TAG
import dev.enric.domain.objects.tag.ComplexTag
import dev.enric.domain.objects.tag.SimpleTag
import dev.enric.logger.Logger
import dev.enric.util.index.TagIndex

class TagListHandler(name: String) : TagHandler(name) {

    fun showTags() {
        val tagsByName = TagIndex.getTagsByName(name)

        if (tagsByName.isEmpty()) {
            Logger.warning("No tags found.")
            return
        }

        tagsByName.forEach { tagHash ->
            val isComplexTag = tagHash.string.startsWith(COMPLEX_TAG.hash.string)

            if (isComplexTag) {
                Logger.info(ComplexTag.newInstance(tagHash).printInfo())
            } else {
                Logger.info(SimpleTag.newInstance(tagHash).printInfo())
            }
        }
    }
}