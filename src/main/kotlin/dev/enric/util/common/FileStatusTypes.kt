package dev.enric.util.common

/**
 * Enum class representing the different types of file status in Trackit.
 * Each status indicates the state of a file in the working directory, staging area, or repository.
 *
 * @param symbol The shorthand symbol used to represent the file status.
 */
enum class FileStatusTypes(val symbol: String) {
    /**
     * The file is not being tracked by Trackit. It exists in the working directory but has not been added to the index.
     * Represented by `?` in Trackit status.
     */
    UNTRACKED("?"),

    /**
     * The file is tracked and has not been modified since the last commit.
     * Represented by a space character in Trackit status.
     */
    UNMODIFIED(" "),

    /**
     * The file has been modified but is not yet staged for commit.
     * Represented by `M` in Trackit status.
     */
    MODIFIED("M"),

    /**
     * The file has been modified and staged for the next commit.
     * Represented by `S` (commonly `A` or `M` in Trackit, but using `S` for "Staged" here).
     */
    STAGED("S"),

    /**
     * The file has been deleted and the deletion has been staged.
     * Represented by `D` in Trackit status.
     */
    DELETE("D"),

    /**
     * The file has been renamed or moved.
     * Represented by `R` in Trackit status.
     */
    RENAMED("R"),

    /**
     * The file is ignored based on `.ignore` rules and will not be tracked.
     * Represented by `I` (Trackit typically doesn't show ignored files unless explicitly requested).
     */
    IGNORED("I")
}
