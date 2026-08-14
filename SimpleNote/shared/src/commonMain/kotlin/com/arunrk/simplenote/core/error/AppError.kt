package com.arunrk.simplenote.core.error

/**
 * Every way an operation can fail, as data rather than as an exception.
 *
 * The data layer catches all throwables at its boundary and converts them into one of these,
 * so nothing above it ever handles a `Throwable`. Because failures are values, the compiler
 * makes a ViewModel acknowledge them instead of letting one escape unnoticed.
 *
 * These carry structured facts, not display strings: turning an error into wording is the
 * presentation layer's job, which is what allows the same error to read differently on a list
 * screen and an editor screen, and to be localised later without touching this file.
 */
sealed interface AppError {

    /** The request never reached the server: no connectivity, refused, or timed out. */
    data object Network : AppError

    /** The server has no such note. Usually means it was deleted from another device. */
    data class NotFound(val serverMessage: String?) : AppError

    /** The server rejected the input. [fieldErrors] maps a field name to its problem. */
    data class Validation(
        val serverMessage: String?,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : AppError

    /** Any other 4xx: the request was wrong in a way the client should not retry blindly. */
    data class Http(
        val status: Int,
        val code: String?,
        val serverMessage: String?,
    ) : AppError

    /** A 5xx. The request may well succeed if retried. */
    data class Server(
        val status: Int,
        val serverMessage: String?,
    ) : AppError

    /** Unclassifiable — most often a response body that did not parse. */
    data class Unknown(val cause: String?) : AppError
}

/**
 * Whether offering the user a retry button makes sense.
 *
 * A network blip or a server fault is worth retrying; a validation failure will fail again
 * identically until the input changes.
 */
val AppError.isRetryable: Boolean
    get() = when (this) {
        is AppError.Network, is AppError.Server, is AppError.Unknown -> true
        is AppError.NotFound, is AppError.Validation, is AppError.Http -> false
    }
