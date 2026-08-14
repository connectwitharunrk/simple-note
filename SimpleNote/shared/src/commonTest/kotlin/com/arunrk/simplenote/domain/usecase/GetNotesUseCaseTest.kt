package com.arunrk.simplenote.domain.usecase

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.testsupport.FakeNoteRepository
import com.arunrk.simplenote.testsupport.TEST_NOW
import com.arunrk.simplenote.testsupport.expectFailure
import com.arunrk.simplenote.testsupport.expectSuccess
import com.arunrk.simplenote.testsupport.note
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class GetNotesUseCaseTest {

    @Test
    fun `returns active notes pinned first then most recently updated`() = runTest {
        val repository = FakeNoteRepository(
            listOf(
                note(id = 1, updatedAt = TEST_NOW),
                note(id = 2, updatedAt = TEST_NOW.plus(2.hours)),
                note(id = 3, pinned = true, updatedAt = TEST_NOW),
            ),
        )

        val notes = GetNotesUseCase(repository)().expectSuccess()

        assertEquals(listOf(3L, 2L, 1L), notes.map { it.id })
    }

    @Test
    fun `an empty query lists rather than searches`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1)))

        GetNotesUseCase(repository)(query = "   ").expectSuccess()

        assertEquals(listOf("getNotes(Active)"), repository.calls)
    }

    @Test
    fun `a non-empty query searches with the query trimmed`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Groceries")))

        val notes = GetNotesUseCase(repository)(query = "  grocer  ").expectSuccess()

        assertEquals(listOf(1L), notes.map { it.id })
        assertEquals(listOf("searchNotes(grocer, Active)"), repository.calls)
    }

    @Test
    fun `search matches content as well as title`() = runTest {
        val repository = FakeNoteRepository(
            listOf(note(id = 1, title = "Shopping", content = "Milk and eggs")),
        )

        val notes = GetNotesUseCase(repository)(query = "eggs").expectSuccess()

        assertEquals(listOf(1L), notes.map { it.id })
    }

    @Test
    fun `the archived filter is passed through`() = runTest {
        val repository = FakeNoteRepository(
            listOf(note(id = 1), note(id = 2, archived = true)),
        )

        val archived = GetNotesUseCase(repository)(filter = NoteFilter.Archived).expectSuccess()

        assertEquals(listOf(2L), archived.map { it.id })
    }

    @Test
    fun `no matches is an empty success, not a failure`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Groceries")))

        val notes = GetNotesUseCase(repository)(query = "zzz").expectSuccess()

        assertTrue(notes.isEmpty())
    }

    @Test
    fun `a repository failure is propagated unchanged`() = runTest {
        val repository = FakeNoteRepository().apply { nextError = AppError.Network }

        assertEquals(AppError.Network, GetNotesUseCase(repository)().expectFailure())
    }
}
