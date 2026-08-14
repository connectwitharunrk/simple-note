package com.arunrk.note_backend.infrastructure.persistence

import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.model.Note

/**
 * Translation between the persistence shape and the domain shape.
 *
 * Hand-written rather than generated: it is a handful of lines, and it stays readable when the
 * two models start to diverge (which is the whole reason they are separate).
 */

fun NoteEntity.toDomain(): Note = Note(
    id = requireNotNull(id) { "A NoteEntity read from the database must have an id" },
    title = title,
    content = content,
    pinned = pinned,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun NewNote.toEntity(): NoteEntity = NoteEntity(
    id = null,
    title = title,
    content = content,
    pinned = pinned,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Copies the mutable fields of [note] onto a managed entity.
 *
 * `id` and `createdAt` are intentionally not copied: they are set once at insert and are not
 * the caller's to change.
 */
fun NoteEntity.applyChangesFrom(note: Note) {
    title = note.title
    content = note.content
    pinned = note.pinned
    archived = note.archived
    updatedAt = note.updatedAt
}
