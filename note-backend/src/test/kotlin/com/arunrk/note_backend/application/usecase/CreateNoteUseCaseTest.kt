package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.application.command.CreateNoteCommand
import com.arunrk.note_backend.domain.exception.InvalidNoteException
import com.arunrk.note_backend.testsupport.FakeNoteRepository
import com.arunrk.note_backend.testsupport.MutableClock
import com.arunrk.note_backend.testsupport.TEST_NOW
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CreateNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val clock = MutableClock()
    private val createNote = CreateNoteUseCase(repository, clock)

    @Test
    fun `creates a note with both timestamps set from the clock`() {
        val created = createNote(CreateNoteCommand(title = "Groceries", content = "Milk, eggs"))

        assertEquals("Groceries", created.title)
        assertEquals("Milk, eggs", created.content)
        assertEquals(TEST_NOW, created.createdAt)
        assertEquals(TEST_NOW, created.updatedAt)
    }

    @Test
    fun `new notes start unpinned and unarchived`() {
        val created = createNote(CreateNoteCommand(title = "Groceries", content = ""))

        assertFalse(created.pinned)
        assertFalse(created.archived)
    }

    @Test
    fun `trims surrounding whitespace from the title`() {
        val created = createNote(CreateNoteCommand(title = "  Groceries  ", content = "Milk"))

        assertEquals("Groceries", created.title)
    }

    @Test
    fun `preserves content whitespace because layout is meaningful there`() {
        val created = createNote(CreateNoteCommand(title = "List", content = "  - milk\n  - eggs\n"))

        assertEquals("  - milk\n  - eggs\n", created.content)
    }

    @Test
    fun `assigns an id and persists the note`() {
        val created = createNote(CreateNoteCommand(title = "Groceries", content = ""))

        assertEquals(1, repository.count())
        assertEquals(created, repository.findById(created.id))
    }

    @Test
    fun `rejects a note that is blank once the title is trimmed`() {
        assertFailsWith<InvalidNoteException> {
            createNote(CreateNoteCommand(title = "   ", content = ""))
        }

        assertEquals(0, repository.count())
    }
}
