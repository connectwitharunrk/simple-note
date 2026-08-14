package com.arunrk.simplenote.testsupport

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.domain.repository.NoteRepository
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * In-memory [NoteRepository] for domain and ViewModel tests.
 *
 * Behaves like the real thing — it honours the ordering contract and the archive filter — so
 * a test failure means the code under test is wrong, not that a stub was set up wrong. Set
 * [nextError] to make the next call fail without changing any wiring.
 */
class FakeNoteRepository(
    initialNotes: List<Note> = emptyList(),
) : NoteRepository {

    private val notes = initialNotes.associateBy { it.id }.toMutableMap()
    private var nextId: Long = (initialNotes.maxOfOrNull { it.id } ?: 0L) + 1
    private var clock: Instant = TEST_NOW

    /** When set, the next repository call fails with this error and then clears it. */
    var nextError: AppError? = null

    /** Records every call, so tests can assert what was asked of the repository. */
    val calls = mutableListOf<String>()

    override suspend fun getNotes(filter: NoteFilter): AppResult<List<Note>> =
        respond("getNotes(${filter.name})") {
            notes.values.filter { it.archived == filter.archived }.sortedWith(LIST_ORDER)
        }

    override suspend fun searchNotes(query: String, filter: NoteFilter): AppResult<List<Note>> =
        respond("searchNotes($query, ${filter.name})") {
            notes.values
                .filter { it.archived == filter.archived && it.matches(query) }
                .sortedWith(LIST_ORDER)
        }

    override suspend fun getNote(id: Long): AppResult<Note> = respondOrNotFound("getNote($id)") {
        notes[id]
    }

    override suspend fun createNote(draft: NoteDraft): AppResult<Note> =
        respond("createNote(${draft.title})") {
            val created = Note(
                id = nextId++,
                title = draft.title,
                content = draft.content,
                pinned = false,
                archived = false,
                createdAt = clock,
                updatedAt = clock,
            )
            notes[created.id] = created
            created
        }

    override suspend fun updateNote(id: Long, draft: NoteDraft): AppResult<Note> =
        respondOrNotFound("updateNote($id)") {
            notes[id]?.copy(title = draft.title, content = draft.content, updatedAt = advance())
                ?.also { notes[id] = it }
        }

    override suspend fun deleteNote(id: Long): AppResult<Unit> =
        respondOrNotFound("deleteNote($id)") { notes.remove(id)?.let { } }

    override suspend fun setPinned(id: Long, pinned: Boolean): AppResult<Note> =
        respondOrNotFound("setPinned($id, $pinned)") {
            notes[id]?.copy(pinned = pinned, updatedAt = advance())?.also { notes[id] = it }
        }

    override suspend fun setArchived(id: Long, archived: Boolean): AppResult<Note> =
        respondOrNotFound("setArchived($id, $archived)") {
            notes[id]
                ?.copy(
                    archived = archived,
                    pinned = if (archived) false else notes.getValue(id).pinned,
                    updatedAt = advance(),
                )
                ?.also { notes[id] = it }
        }

    fun currentNotes(): List<Note> = notes.values.sortedWith(LIST_ORDER)

    private fun advance(): Instant = clock.plus(1.minutes).also { clock = it }

    private fun <T> respond(call: String, produce: () -> T): AppResult<T> {
        calls += call
        consumeError()?.let { return AppResult.Failure(it) }
        return AppResult.Success(produce())
    }

    private fun <T> respondOrNotFound(call: String, produce: () -> T?): AppResult<T> {
        calls += call
        consumeError()?.let { return AppResult.Failure(it) }
        val produced = produce() ?: return AppResult.Failure(AppError.NotFound("Note not found"))
        return AppResult.Success(produced)
    }

    private fun consumeError(): AppError? = nextError?.also { nextError = null }

    private fun Note.matches(query: String) =
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)

    private companion object {
        val LIST_ORDER: Comparator<Note> =
            compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt }
    }
}
