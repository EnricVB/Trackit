package dev.enric.exceptions

import dev.enric.logger.Logger

class BranchNotFoundException(message: String) : Exception(message) {
    init {
        Logger.error(message)
    }
}