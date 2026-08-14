package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.exception.InvalidSearchQueryException
import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.testsupport.FakeNoteRepository
import com.arunrk.note_backend.testsupport.TEST_NOW
import com.arunrk.note_backend.testsupport.note
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GetNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val getNote = GetNoteUseCase(repository)

    @Test
    fun `returns the note`() {
        repository.seed(note(id = 7, title = "Groceries"))

        assertEquals("Groceries", getNote(7).title)
    }

    @Test
    fun `fails with the requested id when the note does not exist`() {
        val failure = assertFailsWith<NoteNotFoundException> { getNote(404) }

        assertEquals(404, failure.noteId)
        assertEquals("NOTE_NOT_FOUND", failure.errorCode)
    }
}

class DeleteNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val deleteNote = DeleteNoteUseCase(repository)

    @Test
    fun `removes the note`() {
        repository.seed(note(id = 1), note(id = 2))

        deleteNote(1)

        assertEquals(1, repository.count())
        assertEquals(null, repository.findById(1))
    }

    @Test
    fun `fails when the note does not exist`() {
        assertFailsWith<NoteNotFoundException> { deleteNote(404) }
    }
}

class ListNotesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val listNotes = ListNotesUseCase(repository)

    @Test
    fun `returns active notes pinned first then most recently updated`() {
        repository.seed(
            note(id = 1, pinned = false, updatedAt = TEST_NOW),
            note(id = 2, pinned = false, updatedAt = TEST_NOW.plus(Duration.ofHours(2))),
            note(id = 3, pinned = true, updatedAt = TEST_NOW),
        )

        assertEquals(listOf(3L, 2L, 1L), listNotes(archived = false).map { it.id })
    }

    @Test
    fun `excludes archived notes by default`() {
        repository.seed(note(id = 1), note(id = 2, archived = true))

        assertEquals(listOf(1L), listNotes().map { it.id })
    }

    @Test
    fun `returns only archived notes when asked for them`() {
        repository.seed(note(id = 1), note(id = 2, archived = true))

        assertEquals(listOf(2L), listNotes(archived = true).map { it.id })
    }

    @Test
    fun `returns an empty list when there is nothing to show`() {
        assertTrue(listNotes().isEmpty())
    }
}

class SearchNotesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val searchNotes = SearchNotesUseCase(repository)

    @Test
    fun `matches the title case-insensitively`() {
        repository.seed(note(id = 1, title = "Groceries", content = "x"))

        assertEquals(listOf(1L), searchNotes("grocer").map { it.id })
    }

    @Test
    fun `matches the content too`() {
        repository.seed(note(id = 1, title = "Shopping", content = "Milk and eggs"))

        assertEquals(listOf(1L), searchNotes("eggs").map { it.id })
    }

    @Test
    fun `does not return archived notes unless asked`() {
        repository.seed(
            note(id = 1, title = "Groceries", content = "x"),
            note(id = 2, title = "Groceries old", content = "x", archived = true),
        )

        assertEquals(listOf(1L), searchNotes("groceries").map { it.id })
        assertEquals(listOf(2L), searchNotes("groceries", archived = true).map { it.id })
    }

    @Test
    fun `returns an empty list when nothing matches`() {
        repository.seed(note(id = 1, title = "Groceries", content = "Milk"))

        assertTrue(searchNotes("zzz").isEmpty())
    }

    @Test
    fun `rejects a blank query rather than returning everything`() {
        repository.seed(note(id = 1))

        assertFailsWith<InvalidSearchQueryException> { searchNotes("   ") }
    }

    @Test
    fun `trims the query before matching`() {
        repository.seed(note(id = 1, title = "Groceries", content = "x"))

        assertEquals(listOf(1L), searchNotes("  groceries  ").map { it.id })
    }
}
