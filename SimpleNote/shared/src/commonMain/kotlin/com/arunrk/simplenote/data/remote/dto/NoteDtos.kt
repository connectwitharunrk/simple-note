package com.arunrk.simplenote.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * The wire shape of a note.
 *
 * Timestamps stay as `String` rather than being deserialized straight into `Instant`: parsing
 * happens in the mapper, where a malformed value can become an `AppError` instead of an
 * exception thrown from inside the serialization library.
 */
@Serializable
data class NoteDto(
    val id: Long,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class NoteDraftDto(
    val title: String,
    val content: String,
)

@Serializable
data class PinRequestDto(val pinned: Boolean)

@Serializable
data class ArchiveRequestDto(val archived: Boolean)

/**
 * The backend's error envelope.
 *
 * Every field is optional. A failing server is exactly the situation where the body may be
 * truncated or not the shape we expect, and losing the status code because one field was
 * missing would be the wrong trade.
 */
@Serializable
data class ErrorResponseDto(
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val timestamp: String? = null,
    val fieldErrors: Map<String, String>? = null,
)
