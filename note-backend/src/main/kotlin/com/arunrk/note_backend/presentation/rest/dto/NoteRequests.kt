package com.arunrk.note_backend.presentation.rest.dto

import com.arunrk.note_backend.domain.model.NoteLimits
import jakarta.validation.constraints.Size

/**
 * Request bodies for the note endpoints.
 *
 * These are separate from the domain model on purpose: the wire format is a published
 * contract with clients, and it should be possible to change one without dragging the other
 * along.
 *
 * Validation here covers *shape* — does this fit the columns. The business rule that a note
 * must actually say something lives in the domain, so it holds for every caller rather than
 * only for HTTP.
 */
data class CreateNoteRequest(
    @field:Size(max = NoteLimits.MAX_TITLE_LENGTH, message = "Title must be at most {max} characters")
    val title: String = "",

    @field:Size(max = NoteLimits.MAX_CONTENT_LENGTH, message = "Content must be at most {max} characters")
    val content: String = "",
)

data class UpdateNoteRequest(
    @field:Size(max = NoteLimits.MAX_TITLE_LENGTH, message = "Title must be at most {max} characters")
    val title: String = "",

    @field:Size(max = NoteLimits.MAX_CONTENT_LENGTH, message = "Content must be at most {max} characters")
    val content: String = "",
)

/**
 * Carries the desired state rather than a toggle, which keeps the endpoint idempotent — a
 * retried request cannot flip a note back to where it started.
 *
 * No default value: omitting the field is a client bug, and Jackson's Kotlin support turns
 * that into a 400 instead of silently unpinning the note.
 */
data class PinRequest(val pinned: Boolean)

data class ArchiveRequest(val archived: Boolean)
