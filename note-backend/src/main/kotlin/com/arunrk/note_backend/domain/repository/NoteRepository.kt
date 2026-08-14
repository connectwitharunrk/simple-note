package com.arunrk.note_backend.domain.repository

import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.model.Note

/**
 * The persistence port owned by the domain.
 *
 * The domain declares what it needs; `infrastructure` supplies the JPA implementation. This
 * is the seam that keeps MySQL out of the business logic, and the one that a future
 * offline cache or a different database would plug into.
 *
 * Implementations must return list results ordered pinned-first, then most recently updated.
 */
interface NoteRepository {

    fun create(note: NewNote): Note

    /** @throws com.arunrk.note_backend.domain.exception.NoteNotFoundException if [note] no longer exists. */
    fun update(note: Note): Note

    fun findById(id: Long): Note?

    fun findAll(archived: Boolean): List<Note>

    /** Case-insensitive substring match against title and content. */
    fun search(query: String, archived: Boolean): List<Note>

    /** @return `true` if a note was deleted, `false` if no note had that id. */
    fun deleteById(id: Long): Boolean
}
