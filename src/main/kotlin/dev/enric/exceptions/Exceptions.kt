package dev.enric.exceptions

import dev.enric.logger.Logger

/**
 * Custom exceptions for Trackit.
 *
 * @param message The message to be displayed when the exception is thrown.
 * @param errorCode The error code to be displayed when the exception is thrown.
 */
open class TrackitException(message: String, errorCode: Int) : Exception(message) {
    init {
        Logger.error("Oops! Something went wrong.")
        Logger.error("Reason: $message")
        Logger.error("Error code: $errorCode")
    }
}

// Data not found exceptions
class BranchNotFoundException(message: String) : TrackitException(message, 1000)
class UserNotFoundException(message: String) : TrackitException(message, 1001)
class RoleNotFoundException(message: String) : TrackitException(message, 1002)

// Invalid data exceptions
class IllegalArgumentValueException(message: String) : TrackitException(message, 2000)

// Permission exceptions
class InvalidPermissionException(message: String) : TrackitException(message, 3001)
