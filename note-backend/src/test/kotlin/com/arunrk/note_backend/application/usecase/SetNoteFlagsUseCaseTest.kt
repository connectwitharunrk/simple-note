package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.testsupport.FakeNoteRepository
import com.arunrk.note_backend.testsupport.MutableClock
import com.arunrk.note_backend.testsupport.TEST_NOW
import com.arunrk.note_backend.testsupport.note
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetNotePinnedUseCaseTest {

    private val repository = FakeNoteRepository()
    private val clock = MutableClock()
    private val setPinned = SetNotePinnedUseCase(repository, GetNoteUseCase(repository), clock)

    @Test
    fun `pins an unpinned note and bumps updatedAt`() {
        repository.seed(note(id = 1, pinned = false, updatedAt = TEST_NOW))
        clock.advanceBy(Duration.ofMinutes(1))

        val updated = setPinned(1, pinned = true)

        assertTrue(updated.pinned)
        assertEquals(TEST_NOW.plus(Duration.ofMinutes(1)), updated.updatedAt)
    }

    @Test
    fun `unpins a pinned note`() {
        repository.seed(note(id = 1, pinned = true))

        assertFalse(setPinned(1, pinned = false).pinned)
    }

    @Test
    fun `setting the value it already has is a no-op that does not bump updatedAt`() {
        repository.seed(note(id = 1, pinned = true, updatedAt = TEST_NOW))
        clock.advanceBy(Duration.ofHours(3))

        val updated = setPinned(1, pinned = true)

        assertTrue(updated.pinned)
        assertEquals(TEST_NOW, updated.updatedAt)
    }

    @Test
    fun `fails when the note does not exist`() {
        assertFailsWith<NoteNotFoundException> { setPinned(404, pinned = true) }
    }
}

class SetNoteArchivedUseCaseTest {

    private val repository = FakeNoteRepository()
    private val clock = MutableClock()
    private val setArchived = SetNoteArchivedUseCase(repository, GetNoteUseCase(repository), clock)

    @Test
    fun `archives a note and bumps updatedAt`() {
        repository.seed(note(id = 1, archived = false, updatedAt = TEST_NOW))
        clock.advanceBy(Duration.ofMinutes(2))

        val updated = setArchived(1, archived = true)

        assertTrue(updated.archived)
        assertEquals(TEST_NOW.plus(Duration.ofMinutes(2)), updated.updatedAt)
    }

    @Test
    fun `archiving a pinned note also unpins it`() {
        repository.seed(note(id = 1, pinned = true, archived = false))

        val updated = setArchived(1, archived = true)

        assertTrue(updated.archived)
        assertFalse(updated.pinned)
    }

    @Test
    fun `unarchiving leaves the note unpinned`() {
        repository.seed(note(id = 1, pinned = false, archived = true))

        val updated = setArchived(1, archived = false)

        assertFalse(updated.archived)
        assertFalse(updated.pinned)
    }

    @Test
    fun `setting the value it already has is a no-op that does not bump updatedAt`() {
        repository.seed(note(id = 1, archived = true, updatedAt = TEST_NOW))
        clock.advanceBy(Duration.ofHours(3))

        assertEquals(TEST_NOW, setArchived(1, archived = true).updatedAt)
    }

    @Test
    fun `fails when the note does not exist`() {
        assertFailsWith<NoteNotFoundException> { setArchived(404, archived = true) }
    }
}
