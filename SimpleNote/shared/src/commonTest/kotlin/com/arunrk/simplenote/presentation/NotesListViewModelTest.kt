package com.arunrk.simplenote.presentation

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.domain.usecase.DeleteNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNotesUseCase
import com.arunrk.simplenote.domain.usecase.SetNoteArchivedUseCase
import com.arunrk.simplenote.domain.usecase.SetNotePinnedUseCase
import com.arunrk.simplenote.presentation.notes.NotesListEffect
import com.arunrk.simplenote.presentation.notes.NotesListIntent
import com.arunrk.simplenote.presentation.notes.NotesListViewModel
import com.arunrk.simplenote.testsupport.FakeNoteRepository
import com.arunrk.simplenote.testsupport.TEST_NOW
import com.arunrk.simplenote.testsupport.ViewModelTest
import com.arunrk.simplenote.testsupport.ofType
import com.arunrk.simplenote.testsupport.recordEffects
import com.arunrk.simplenote.testsupport.note
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class NotesListViewModelTest : ViewModelTest() {

    private fun viewModel(repository: FakeNoteRepository) = NotesListViewModel(
        getNotes = GetNotesUseCase(repository),
        setNotePinned = SetNotePinnedUseCase(repository),
        setNoteArchived = SetNoteArchivedUseCase(repository),
        deleteNote = DeleteNoteUseCase(repository),
    )

    // ---------- loading ----------

    @Test
    fun `load shows a spinner and then the notes`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1), note(id = 2)))
            .apply { responseDelayMillis = 100 }
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceTimeBy(1)
        assertTrue(sut.state.value.isLoading)

        advanceUntilIdle()
        assertFalse(sut.state.value.isLoading)
        assertEquals(2, sut.state.value.notes.size)
        assertNull(sut.state.value.error)
    }

    @Test
    fun `notes arrive pinned first then most recently updated`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(id = 1, updatedAt = TEST_NOW),
                note(id = 2, updatedAt = TEST_NOW.plus(2.hours)),
                note(id = 3, pinned = true, updatedAt = TEST_NOW),
            ),
        )
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        assertEquals(listOf(3L, 2L, 1L), sut.state.value.notes.map { it.id })
    }

    @Test
    fun `a failed load records the error and stops loading`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository().apply { nextError = AppError.Network }
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        assertEquals(AppError.Network, sut.state.value.error)
        assertFalse(sut.state.value.isLoading)
    }

    @Test
    fun `an empty list is only reported as empty once loading finished without error`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository().apply { responseDelayMillis = 100 }
            val sut = viewModel(repository)

            sut.onIntent(NotesListIntent.Load)
            advanceTimeBy(1)
            assertFalse(sut.state.value.isEmpty, "must not claim empty while still loading")

            advanceUntilIdle()
            assertTrue(sut.state.value.isEmpty)
        }

    @Test
    fun `a failed load is not reported as an empty list`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository().apply { nextError = AppError.Network }
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        assertFalse(sut.state.value.isEmpty, "an error must not look like 'no notes yet'")
    }

    @Test
    fun `sending Load twice does not start a second pipeline`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        assertEquals(1, repository.calls.size, "duplicate collectors would double every load")
    }

    @Test
    fun `retry clears the error and loads again`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1))).apply { nextError = AppError.Network }
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()
        assertEquals(AppError.Network, sut.state.value.error)

        sut.onIntent(NotesListIntent.Retry)
        advanceUntilIdle()

        assertNull(sut.state.value.error)
        assertEquals(1, sut.state.value.notes.size)
    }

    @Test
    fun `refresh keeps the current notes visible and uses the refresh indicator`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(listOf(note(id = 1)))
            val sut = viewModel(repository)

            sut.onIntent(NotesListIntent.Load)
            advanceUntilIdle()

            repository.responseDelayMillis = 100
            sut.onIntent(NotesListIntent.Refresh)
            advanceTimeBy(1)

            assertTrue(sut.state.value.isRefreshing)
            assertFalse(sut.state.value.isLoading, "refresh must not blank the list")
            assertEquals(1, sut.state.value.notes.size)
        }

    // ---------- search ----------

    @Test
    fun `typing issues one search after the debounce, not one per keystroke`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(listOf(note(id = 1, title = "Milk")))
            val sut = viewModel(repository)

            sut.onIntent(NotesListIntent.Load)
            advanceUntilIdle()
            val callsAfterLoad = repository.calls.size

            "milk".forEachIndexed { index, _ ->
                sut.onIntent(NotesListIntent.QueryChanged("milk".take(index + 1)))
                advanceTimeBy(50)
            }
            advanceUntilIdle()

            assertEquals(
                1,
                repository.calls.size - callsAfterLoad,
                "four keystrokes 50ms apart should collapse into one request",
            )
            assertEquals("searchNotes(milk, Active)", repository.calls.last())
        }

    @Test
    fun `the query is reflected in state immediately even though the search is debounced`() =
        runTest(testDispatcher) {
            val sut = viewModel(FakeNoteRepository())

            sut.onIntent(NotesListIntent.Load)
            advanceUntilIdle()
            sut.onIntent(NotesListIntent.QueryChanged("mi"))

            assertEquals("mi", sut.state.value.query, "the text field must not lag behind typing")
            assertTrue(sut.state.value.isSearching)
        }

    @Test
    fun `a slower typist gets one request per pause`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Milk")))
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()
        val callsAfterLoad = repository.calls.size

        sut.onIntent(NotesListIntent.QueryChanged("mi"))
        advanceUntilIdle()
        sut.onIntent(NotesListIntent.QueryChanged("milk"))
        advanceUntilIdle()

        assertEquals(2, repository.calls.size - callsAfterLoad)
    }

    @Test
    fun `clearing the query goes back to listing immediately`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Milk")))
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()
        sut.onIntent(NotesListIntent.QueryChanged("milk"))
        advanceUntilIdle()

        sut.onIntent(NotesListIntent.QueryCleared)
        advanceUntilIdle()

        assertEquals("", sut.state.value.query)
        assertEquals("getNotes(Active)", repository.calls.last())
    }

    @Test
    fun `switching filter reloads without waiting for the search debounce`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(
                listOf(note(id = 1), note(id = 2, archived = true)),
            )
            val sut = viewModel(repository)

            sut.onIntent(NotesListIntent.Load)
            advanceUntilIdle()

            sut.onIntent(NotesListIntent.FilterChanged(NoteFilter.Archived))
            advanceTimeBy(10)
            advanceUntilIdle()

            assertEquals(NoteFilter.Archived, sut.state.value.filter)
            assertEquals(listOf(2L), sut.state.value.notes.map { it.id })
        }

    // ---------- pin ----------

    @Test
    fun `pinning moves the note to the top before the server replies`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(id = 1, updatedAt = TEST_NOW.plus(1.hours)),
                note(id = 2, updatedAt = TEST_NOW),
            ),
        )
        val sut = viewModel(repository)
        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), sut.state.value.notes.map { it.id })

        sut.onIntent(NotesListIntent.PinChanged(id = 2, pinned = true))

        // No time advanced: the request has not run yet, but the UI has already reordered.
        assertEquals(listOf(2L, 1L), sut.state.value.notes.map { it.id })
        assertTrue(sut.state.value.notes.first().pinned)
    }

    @Test
    fun `a failed pin restores the previous list and reports the error`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(id = 1, updatedAt = TEST_NOW.plus(1.hours)),
                note(id = 2, updatedAt = TEST_NOW),
            ),
        )
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        repository.nextError = AppError.Network
        sut.onIntent(NotesListIntent.PinChanged(id = 2, pinned = true))
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), sut.state.value.notes.map { it.id })
        assertFalse(sut.state.value.notes.single { it.id == 2L }.pinned)
        assertIs<NotesListEffect.ShowError>(effects.last)
    }

    @Test
    fun `unpinning works the same way`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = true)))
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        sut.onIntent(NotesListIntent.PinChanged(id = 1, pinned = false))
        advanceUntilIdle()

        assertFalse(sut.state.value.notes.single().pinned)
    }

    // ---------- archive and delete ----------

    @Test
    fun `archiving removes the note from the active list and announces it`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(listOf(note(id = 1), note(id = 2)))
            val sut = viewModel(repository)
            val effects = recordEffects(sut.effects)

            sut.onIntent(NotesListIntent.Load)
            advanceUntilIdle()

            sut.onIntent(NotesListIntent.ArchiveChanged(id = 1, archived = true))
            advanceUntilIdle()

            assertEquals(listOf(2L), sut.state.value.notes.map { it.id })
            assertEquals(NotesListEffect.NoteArchiveChanged(archived = true), effects.last)
        }

    @Test
    fun `a failed archive leaves the list alone`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        repository.nextError = AppError.Server(500, null)
        sut.onIntent(NotesListIntent.ArchiveChanged(id = 1, archived = true))
        advanceUntilIdle()

        assertEquals(listOf(1L), sut.state.value.notes.map { it.id })
        assertIs<NotesListEffect.ShowError>(effects.last)
    }

    @Test
    fun `deleting removes the note and announces it`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1), note(id = 2)))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()

        sut.onIntent(NotesListIntent.DeleteRequested(id = 1))
        advanceUntilIdle()

        assertEquals(listOf(2L), sut.state.value.notes.map { it.id })
        assertEquals(NotesListEffect.NoteDeleted, effects.last)
    }

    @Test
    fun `deleting the selected note also clears the selection`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)

        sut.onIntent(NotesListIntent.Load)
        advanceUntilIdle()
        sut.onIntent(NotesListIntent.NoteSelected(1))

        sut.onIntent(NotesListIntent.DeleteRequested(id = 1))
        advanceUntilIdle()

        assertNull(sut.state.value.selectedNoteId, "the detail pane would show a deleted note")
    }

    // ---------- navigation ----------

    @Test
    fun `selecting a note records it and asks to open it`() = runTest(testDispatcher) {
        val sut = viewModel(FakeNoteRepository(listOf(note(id = 5))))
        val effects = recordEffects(sut.effects)

        sut.onIntent(NotesListIntent.NoteSelected(5))
        advanceUntilIdle()

        assertEquals(5L, sut.state.value.selectedNoteId)
        assertEquals(NotesListEffect.OpenNote(5), effects.last)
    }

    @Test
    fun `the create button asks for a blank editor`() = runTest(testDispatcher) {
        val sut = viewModel(FakeNoteRepository())
        val effects = recordEffects(sut.effects)

        sut.onIntent(NotesListIntent.CreateNoteRequested)
        advanceUntilIdle()

        assertEquals(NotesListEffect.OpenNewNote, effects.last)
    }
}
