package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.application.command.UpdateNoteCommand
import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.testsupport.FakeNoteRepository
import com.arunrk.note_backend.testsupport.MutableClock
import com.arunrk.note_backend.testsupport.TEST_NOW
import com.arunrk.note_backend.testsupport.note
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdateNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val clock = MutableClock()
    private val updateNote = UpdateNoteUseCase(repository, GetNoteUseCase(repository), clock)

    @Test
    fun `replaces title and content`() {
        repository.seed(note(id = 1, title = "Old", content = "Old body"))

        val updated = updateNote(UpdateNoteCommand(id = 1, title = "New", content = "New body"))

        assertEquals("New", updated.title)
        assertEquals("New body", updated.content)
    }

    @Test
    fun `moves updatedAt forward but leaves createdAt alone`() {
        repository.seed(note(id = 1, createdAt = TEST_NOW, updatedAt = TEST_NOW))
        clock.advanceBy(Duration.ofMinutes(5))

        val updated = updateNote(UpdateNoteCommand(id = 1, title = "New", content = "New body"))

        assertEquals(TEST_NOW, updated.createdAt)
        assertEquals(TEST_NOW.plus(Duration.ofMinutes(5)), updated.updatedAt)
        assertTrue(updated.updatedAt.isAfter(updated.createdAt))
    }

    @Test
    fun `does not disturb the pinned or archived flags`() {
        repository.seed(note(id = 1, pinned = true, archived = true))

        val updated = updateNote(UpdateNoteCommand(id = 1, title = "New", content = "New body"))

        assertTrue(updated.pinned)
        assertTrue(updated.archived)
    }

    @Test
    fun `fails when the note does not exist`() {
        val failure = assertFailsWith<NoteNotFoundException> {
            updateNote(UpdateNoteCommand(id = 404, title = "New", content = "New body"))
        }

        assertEquals(404, failure.noteId)
    }
}
