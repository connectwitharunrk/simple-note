package com.arunrk.simplenote.domain.repository

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.NoteFilter

/**
 * How the app reaches notes, declared by the domain and implemented in `data`.
 *
 * Nothing here mentions HTTP, Ktor, or JSON. That is what lets the use cases be tested
 * without a server, and what would let an offline cache be slotted in later — a new
 * implementation of this interface, with no change above it.
 *
 * Every method returns [AppResult] rather than throwing: failure is an expected outcome of
 * talking to a network, not an exceptional one.
 */
interface NoteRepository {

    /** Notes in the given bucket, pinned first, then most recently updated. */
    suspend fun getNotes(filter: NoteFilter): AppResult<List<Note>>

    /** Notes in the given bucket whose title or content contains [query]. */
    suspend fun searchNotes(query: String, filter: NoteFilter): AppResult<List<Note>>

    suspend fun getNote(id: Long): AppResult<Note>

    suspend fun createNote(draft: NoteDraft): AppResult<Note>

    suspend fun updateNote(id: Long, draft: NoteDraft): AppResult<Note>

    suspend fun deleteNote(id: Long): AppResult<Unit>

    suspend fun setPinned(id: Long, pinned: Boolean): AppResult<Note>

    suspend fun setArchived(id: Long, archived: Boolean): AppResult<Note>
}
