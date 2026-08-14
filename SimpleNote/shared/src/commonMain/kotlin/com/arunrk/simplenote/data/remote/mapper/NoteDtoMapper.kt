package com.arunrk.simplenote.data.remote.mapper

import com.arunrk.simplenote.data.remote.dto.NoteDraftDto
import com.arunrk.simplenote.data.remote.dto.NoteDto
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteDraft
import kotlin.time.Instant

/**
 * Wire format to domain model and back.
 *
 * [NoteDto.toDomain] throws if a timestamp is unparseable; callers run it inside the
 * repository's `mapCatching`, which turns that into `AppError.Unknown`. Failing loudly here
 * and classifying once at the boundary beats silently substituting a default date.
 */
fun NoteDto.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    pinned = pinned,
    archived = archived,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun List<NoteDto>.toDomain(): List<Note> = map { it.toDomain() }

fun NoteDraft.toDto(): NoteDraftDto = NoteDraftDto(title = title, content = content)
