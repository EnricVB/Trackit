package dev.enric.util.staging

enum class FileStatusTypes(type: String) {
    NON_TRACKED("?"),
    NON_MODIFIED(" "),
    MODIFIED("M"),
    STAGED("A"),
    STAGED_MODIFIED("M"),
    IGNORED("I"),
}