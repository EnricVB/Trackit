package dev.enric.command.repo.tag

import dev.enric.command.TrackitCommand
import dev.enric.core.handler.repo.tag.TagListHandler
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "list-tags",
    description = ["Lists a tag. Can be filtered by tag name."],
    mixinStandardHelpOptions = true,
    footer = [
        "Examples:",
        "  trackit repo tag list-tags",
        "  trackit repo tag list-tags --name <tag-name>",
        "  trackit repo tag list-tags -n <tag-name>"
    ]
)
class TagList : TrackitCommand() {

    /**
     * Name of the tag to be filtered.
     */
    @Option(names = ["--name", "-n"], description = ["Tag name to filter."], required = false)
    var name: String = ""

    override fun call(): Int {
        super.call()

        val handler = TagListHandler(name)
        handler.showTags()

        return 0
    }
}
