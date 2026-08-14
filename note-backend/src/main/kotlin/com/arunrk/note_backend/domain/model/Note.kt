package com.arunrk.note_backend.domain.model

import com.arunrk.note_backend.domain.exception.InvalidNoteException
import java.time.Instant

/** Field limits shared by the domain model and mirrored by the database schema. */
object NoteLimits {
    const val MAX_TITLE_LENGTH: Int = 255
    const val MAX_CONTENT_LENGTH: Int = 65_535
}

/**
 * A note that has not been persisted yet, and therefore has no id.
 *
 * Keeping this separate from [Note] means no code in the system ever has to deal with a
 * nullable id or unwrap one with `!!`.
 */
data class NewNote(
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        requireValidNoteFields(title, content)
    }
}

/** A persisted note. Immutable: every change produces a new instance via [copy]. */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        requireValidNoteFields(title, content)
    }
}

/**
 * The single business rule this version enforces: a note must say something, and must fit in
 * the columns that store it.
 *
 * Declared here rather than in a use case so the invariant holds for every [Note] and
 * [NewNote] in the system, no matter which layer constructed it.
 */
internal fun requireValidNoteFields(title: String, content: String) {
    if (title.isBlank() && content.isBlank()) {
        throw InvalidNoteException("A note must have a title or content")
    }
    if (title.length > NoteLimits.MAX_TITLE_LENGTH) {
        throw InvalidNoteException("Title must be at most ${NoteLimits.MAX_TITLE_LENGTH} characters")
    }
    if (content.length > NoteLimits.MAX_CONTENT_LENGTH) {
        throw InvalidNoteException("Content must be at most ${NoteLimits.MAX_CONTENT_LENGTH} characters")
    }
}
