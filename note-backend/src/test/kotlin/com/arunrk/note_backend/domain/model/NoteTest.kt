package com.arunrk.note_backend.domain.model

import com.arunrk.note_backend.domain.exception.InvalidNoteException
import com.arunrk.note_backend.testsupport.TEST_NOW
import com.arunrk.note_backend.testsupport.note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoteTest {

    @Test
    fun `a note with only a title is valid`() {
        val created = NewNote(title = "Groceries", content = "", createdAt = TEST_NOW, updatedAt = TEST_NOW)

        assertEquals("Groceries", created.title)
    }

    @Test
    fun `a note with only content is valid`() {
        val created = NewNote(title = "", content = "Milk, eggs", createdAt = TEST_NOW, updatedAt = TEST_NOW)

        assertEquals("Milk, eggs", created.content)
    }

    @Test
    fun `a note that is blank on both fields is rejected`() {
        val failure = assertFailsWith<InvalidNoteException> {
            NewNote(title = "   ", content = "\n\t ", createdAt = TEST_NOW, updatedAt = TEST_NOW)
        }

        assertEquals("A note must have a title or content", failure.message)
        assertEquals("INVALID_NOTE", failure.errorCode)
    }

    @Test
    fun `a title longer than the column is rejected`() {
        assertFailsWith<InvalidNoteException> {
            NewNote(
                title = "a".repeat(NoteLimits.MAX_TITLE_LENGTH + 1),
                content = "body",
                createdAt = TEST_NOW,
                updatedAt = TEST_NOW,
            )
        }
    }

    @Test
    fun `content longer than the column is rejected`() {
        assertFailsWith<InvalidNoteException> {
            NewNote(
                title = "title",
                content = "a".repeat(NoteLimits.MAX_CONTENT_LENGTH + 1),
                createdAt = TEST_NOW,
                updatedAt = TEST_NOW,
            )
        }
    }

    @Test
    fun `the invariant also holds for a copy of a persisted note`() {
        val existing = note(id = 1, title = "Groceries", content = "Milk")

        assertFailsWith<InvalidNoteException> {
            existing.copy(title = "", content = "")
        }
    }
}
