package com.arunrk.note_backend.presentation.rest.dto

import java.time.Instant

/**
 * The note as clients see it.
 *
 * `pinned`/`archived` drop the `is_` prefix the database columns carry — the column names are
 * a storage detail, not part of the API contract.
 */
data class NoteResponse(
    val id: Long,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
