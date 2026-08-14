package com.arunrk.simplenote.presentation.error

import com.arunrk.simplenote.core.error.AppError

/**
 * Turns an [AppError] into words.
 *
 * This is the only file in the app that puts user-facing sentences on an error, which is why
 * [AppError] itself carries structured data instead of strings — the same failure can read
 * differently here depending on context, and a translation layer would slot in at exactly
 * this point.
 *
 * The server's own message is preferred where it is meaningful, since it is more specific
 * than anything the client could guess, with a fallback for when there is no body to read.
 */
fun AppError.displayMessage(): String = when (this) {
    AppError.Network ->
        "Can't reach the server. Check your connection and try again."

    is AppError.NotFound ->
        serverMessage ?: "That note no longer exists."

    is AppError.Validation ->
        serverMessage ?: "Please check what you've entered."

    is AppError.Http ->
        serverMessage ?: "The server rejected that request (HTTP $status)."

    is AppError.Server ->
        "Something went wrong on the server. Please try again."

    is AppError.Unknown ->
        "Something went wrong. Please try again."
}

/** A short heading to sit above [displayMessage] in a full-screen error state. */
fun AppError.displayTitle(): String = when (this) {
    AppError.Network -> "No connection"
    is AppError.NotFound -> "Not found"
    is AppError.Validation -> "Check your input"
    is AppError.Http -> "Request rejected"
    is AppError.Server -> "Server error"
    is AppError.Unknown -> "Something went wrong"
}
