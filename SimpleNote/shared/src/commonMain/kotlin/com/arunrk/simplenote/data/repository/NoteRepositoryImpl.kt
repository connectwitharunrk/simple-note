package com.arunrk.simplenote.data.repository

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.data.remote.NoteApi
import com.arunrk.simplenote.data.remote.mapper.toDomain
import com.arunrk.simplenote.data.remote.mapper.toDto
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.domain.repository.NoteRepository

/**
 * Satisfies the domain port using the REST API.
 *
 * Its whole job is translation: filters become query parameters, DTOs become domain models.
 * There is no business logic here, which is what makes swapping in a cached or offline-first
 * implementation later a contained change.
 */
class NoteRepositoryImpl(
    private val noteApi: NoteApi,
) : NoteRepository {

    override suspend fun getNotes(filter: NoteFilter): AppResult<List<Note>> =
        noteApi.getNotes(filter.archived).mapCatching { it.toDomain() }

    override suspend fun searchNotes(query: String, filter: NoteFilter): AppResult<List<Note>> =
        noteApi.searchNotes(query, filter.archived).mapCatching { it.toDomain() }

    override suspend fun getNote(id: Long): AppResult<Note> =
        noteApi.getNote(id).mapCatching { it.toDomain() }

    override suspend fun createNote(draft: NoteDraft): AppResult<Note> =
        noteApi.createNote(draft.toDto()).mapCatching { it.toDomain() }

    override suspend fun updateNote(id: Long, draft: NoteDraft): AppResult<Note> =
        noteApi.updateNote(id, draft.toDto()).mapCatching { it.toDomain() }

    override suspend fun deleteNote(id: Long): AppResult<Unit> = noteApi.deleteNote(id)

    override suspend fun setPinned(id: Long, pinned: Boolean): AppResult<Note> =
        noteApi.setPinned(id, pinned).mapCatching { it.toDomain() }

    override suspend fun setArchived(id: Long, archived: Boolean): AppResult<Note> =
        noteApi.setArchived(id, archived).mapCatching { it.toDomain() }
}

/**
 * Maps a success, converting a failure during mapping into [AppError.Unknown].
 *
 * The realistic case is a timestamp the client cannot parse. Without this, a single bad field
 * from the server would propagate as an exception out of the repository and crash the app —
 * exactly the thing the [AppResult] boundary exists to prevent.
 */
private inline fun <T, R> AppResult<T>.mapCatching(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> try {
            AppResult.Success(transform(data))
        } catch (throwable: Throwable) {
            AppResult.Failure(
                AppError.Unknown("Could not read the server response: ${throwable.message}"),
            )
        }

        is AppResult.Failure -> this
    }
