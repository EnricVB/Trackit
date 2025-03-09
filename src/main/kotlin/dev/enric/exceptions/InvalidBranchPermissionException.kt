package dev.enric.exceptions

import dev.enric.logger.Logger

class InvalidBranchPermissionException(message: String) : Exception(message) {
    init {
        Logger.error(message)
    }
}
