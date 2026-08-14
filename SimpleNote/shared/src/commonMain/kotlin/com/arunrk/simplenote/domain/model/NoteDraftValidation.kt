package com.arunrk.simplenote.domain.model

import com.arunrk.simplenote.core.error.AppError

/**
 * Checks a draft against the same rules the backend enforces.
 *
 * This is a deliberate duplication of server-side validation, not a replacement for it. The
 * server stays the authority — but catching an empty note here means the user gets an instant
 * answer instead of a round trip, and works offline-first later without changing shape.
 *
 * @return the failure, or `null` when the draft is acceptable.
 */
fun NoteDraft.validationError(): AppError.Validation? = when {
    title.isBlank() && content.isBlank() -> AppError.Validation(
        serverMessage = "A note must have a title or content",
    )

    title.length > NoteLimits.MAX_TITLE_LENGTH -> AppError.Validation(
        serverMessage = "Title is too long",
        fieldErrors = mapOf(
            "title" to "Title must be at most ${NoteLimits.MAX_TITLE_LENGTH} characters",
        ),
    )

    content.length > NoteLimits.MAX_CONTENT_LENGTH -> AppError.Validation(
        serverMessage = "Content is too long",
        fieldErrors = mapOf(
            "content" to "Content must be at most ${NoteLimits.MAX_CONTENT_LENGTH} characters",
        ),
    )

    else -> null
}

/** Trims the title while leaving content untouched, matching what the backend stores. */
fun NoteDraft.normalized(): NoteDraft = copy(title = title.trim())
