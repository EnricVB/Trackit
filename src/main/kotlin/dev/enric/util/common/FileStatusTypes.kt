package dev.enric.util.common

/**
 * Enum class to represent the different types of file status in git.
 * @param type The type of the file status.
 * @property NON_TRACKED The file is not tracked by git.
 * @property UP_TO_DATE The file is not modified.
 * @property MODIFIED The file is modified and not in staging area.
 * @property STAGED The file is staged.
 * @property STAGED_MODIFIED The file is staged and modified.
 * @property IGNORED The file is ignored.
 */
enum class FileStatusTypes(type: String) {
    NON_TRACKED("?"),
    UP_TO_DATE(" "),
    MODIFIED("M"),
    STAGED("A"),
    STAGED_MODIFIED("M"),
    IGNORED("I"),
}