package com.arunrk.note_backend.presentation.rest.mapper

import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.presentation.rest.dto.NoteResponse

/** Domain note to wire format. The only direction needed: requests become commands instead. */
fun Note.toResponse(): NoteResponse = NoteResponse(
    id = id,
    title = title,
    content = content,
    pinned = pinned,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Note>.toResponses(): List<NoteResponse> = map { it.toResponse() }
