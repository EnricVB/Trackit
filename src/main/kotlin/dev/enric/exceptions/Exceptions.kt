package dev.enric.exceptions

import dev.enric.logger.Logger
import dev.enric.util.common.Utility
import kotlin.random.Random

/**
 * Custom exceptions for Trackit.
 *
 * @param message The message to be displayed when the exception is thrown.
 * @param errorCode The error code to be displayed when the exception is thrown.
 */
open class TrackitException(message: String, errorCode: Int) : Exception(message) {
    companion object {
        private val errorMessages = listOf(
            "Whoops! Something went wrong.",
            "Yikes! An unexpected error occurred.",
            "This is fine. *flames intensify*",
            "Hmm... Something broke.",
            "The bug is not a bug. It’s a surprise feature!",
            "Oh no! A wild error appeared.",
            "Oh snap! An error appeared.",
            "Ahhh! There's a bug",
            "Welp, time to blame the intern."
        )

        fun getRandomErrorMessage(): String {
            return errorMessages[Random.nextInt(errorMessages.size)]
        }
    }

    init {
        Logger.error("""
            
            ########################################
            
            ${getRandomErrorMessage()}
            
            Reason: $message
            Error code: $errorCode
            Date: ${Utility.getLogDateFormat("yyyy-MM-dd HH:mm:ss")}
            
            ########################################
            
            """.trimIndent())

        Logger.trace(stackTraceToString())
    }
}

// Data not found exceptions
class BranchNotFoundException(message: String) : TrackitException(message, 1000)
class UserNotFoundException(message: String) : TrackitException(message, 1001)
class RoleNotFoundException(message: String) : TrackitException(message, 1002)
class CommitNotFoundException(message: String) : TrackitException(message, 1003)

// Invalid data exceptions
class IllegalArgumentValueException(message: String) : TrackitException(message, 2000)
class IllegalHashException(message: String) : TrackitException(message, 2001)
class IllegalStateException(message: String) : TrackitException(message, 2002)

// Permission exceptions
class InvalidPermissionException(message: String) : TrackitException(message, 3001)
