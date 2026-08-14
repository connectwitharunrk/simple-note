package com.arunrk.simplenote.domain.model

import kotlin.time.Instant

/**
 * A note as the app understands it.
 *
 * Kept separate from the network DTO so a change to the wire format is absorbed by a mapper
 * rather than rippling through the UI, and so this stays free of serialization annotations.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** True when the note has never been edited since it was written. */
    val isUnedited: Boolean get() = createdAt == updatedAt
}

/** Field limits, mirroring the backend's `NoteLimits` and the MySQL column widths. */
object NoteLimits {
    const val MAX_TITLE_LENGTH: Int = 255
    const val MAX_CONTENT_LENGTH: Int = 65_535
}

/**
 * The editable part of a note.
 *
 * Creating and updating both submit exactly these two fields, so they share one type. Pin and
 * archive are not here on purpose: they have their own operations, which is what stops an
 * ordinary edit from silently unpinning a note.
 */
data class NoteDraft(
    val title: String,
    val content: String,
) {
    companion object {
        val Empty = NoteDraft(title = "", content = "")
    }
}

fun Note.toDraft(): NoteDraft = NoteDraft(title = title, content = content)

/** Which bucket of notes the user is looking at. */
enum class NoteFilter {
    Active,
    Archived;

    val archived: Boolean get() = this == Archived
}
