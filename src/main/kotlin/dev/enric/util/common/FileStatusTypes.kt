package dev.enric.util.common

/**
 * Enum class representing the different types of file status in Trackit.
 * Each status indicates the state of a file in the working directory, staging area, or repository.
 *
 * @param symbol The shorthand symbol used to represent the file status.
 * @param description A human-readable description of the status.
 */
enum class FileStatusTypes(val symbol: String, val description: String) {

    /**
     * The file is not being tracked by Trackit. It exists in the working directory but has not been added to the index.
     */
    UNTRACKED("?",
        """Untracked files:
            |   (use "trackit stage <file>..." to include in what will be committed)""".trimMargin()),

    /**
     * The file is tracked and has not been modified since the last commit.
     */
    UNMODIFIED("*", """Files up to date:"""),

    /**
     * The file has been modified but is not yet staged for commit.
     */
    MODIFIED("M",
        """Files with changes:
            |   (use "trackit stage <file>..." to update what will be committed)""".trimMargin()),

    /**
     * The file has been modified and staged for the next commit.
     */
    STAGED("S",
        """Changes to be committed:
            |   (use "trackit commit" to commit changes)
            |   (use "trackit unstage" to unstage changes)""".trimMargin()),

    /**
     * The file has been deleted and the deletion has been staged.
     */
    DELETE("D",
        """Deleted files:
            |   (use "trackit restore <file>..." to restore the file)""".trimMargin()),

    /**
     * The file has been renamed or moved.
     */
    RENAMED("R",
        """Renamed files:
            |   (use "trackit restore <file>..." to restore the file)""".trimMargin()),

    /**
     * The file is ignored based on `.ignore` rules and will not be tracked.
     */
    IGNORED("I", "Ignored files");

    companion object {
        /**
         * Retrieves a [FileStatusTypes] from its symbol.
         * Returns null if the symbol does not match any status.
         */
        fun fromSymbol(symbol: String): FileStatusTypes? {
            return entries.find { it.symbol == symbol }
        }
    }
}