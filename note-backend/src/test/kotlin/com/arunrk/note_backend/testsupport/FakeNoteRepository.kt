package com.arunrk.note_backend.testsupport

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository

/**
 * In-memory [NoteRepository] for use case tests.
 *
 * A hand-written fake rather than a mock: it enforces the ordering contract the port
 * documents, so a use case test fails if the ordering assumption is wrong, and the tests
 * read as behaviour instead of a list of stubbed calls.
 */
class FakeNoteRepository : NoteRepository {

    private val notes = linkedMapOf<Long, Note>()
    private var nextId = 1L

    override fun create(note: NewNote): Note {
        val created = Note(
            id = nextId++,
            title = note.title,
            content = note.content,
            pinned = note.pinned,
            archived = note.archived,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
        notes[created.id] = created
        return created
    }

    override fun update(note: Note): Note {
        if (!notes.containsKey(note.id)) throw NoteNotFoundException(note.id)
        notes[note.id] = note
        return note
    }

    override fun findById(id: Long): Note? = notes[id]

    override fun findAll(archived: Boolean): List<Note> =
        notes.values.filter { it.archived == archived }.sortedWith(LIST_ORDER)

    override fun search(query: String, archived: Boolean): List<Note> =
        notes.values
            .filter { it.archived == archived && it.matches(query) }
            .sortedWith(LIST_ORDER)

    override fun deleteById(id: Long): Boolean = notes.remove(id) != null

    /** Inserts notes directly, bypassing id assignment, to arrange a test's starting state. */
    fun seed(vararg seeded: Note) {
        seeded.forEach { note ->
            notes[note.id] = note
            nextId = maxOf(nextId, note.id + 1)
        }
    }

    fun count(): Int = notes.size

    private fun Note.matches(query: String) =
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)

    private companion object {
        /** Mirrors the ordering the port promises: pinned first, then most recently updated. */
        val LIST_ORDER: Comparator<Note> =
            compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt }
    }
}
