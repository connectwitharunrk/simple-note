package com.arunrk.simplenote.presentation

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.usecase.CreateNoteUseCase
import com.arunrk.simplenote.domain.usecase.DeleteNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNoteUseCase
import com.arunrk.simplenote.domain.usecase.SetNoteArchivedUseCase
import com.arunrk.simplenote.domain.usecase.SetNotePinnedUseCase
import com.arunrk.simplenote.domain.usecase.UpdateNoteUseCase
import com.arunrk.simplenote.presentation.editor.NoteEditorEffect
import com.arunrk.simplenote.presentation.editor.NoteEditorIntent
import com.arunrk.simplenote.presentation.editor.NoteEditorViewModel
import com.arunrk.simplenote.testsupport.FakeNoteRepository
import com.arunrk.simplenote.testsupport.ViewModelTest
import com.arunrk.simplenote.testsupport.ofType
import com.arunrk.simplenote.testsupport.recordEffects
import com.arunrk.simplenote.testsupport.note
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoteEditorViewModelTest : ViewModelTest() {

    private fun viewModel(repository: FakeNoteRepository) = NoteEditorViewModel(
        getNote = GetNoteUseCase(repository),
        createNote = CreateNoteUseCase(repository),
        updateNote = UpdateNoteUseCase(repository),
        deleteNote = DeleteNoteUseCase(repository),
        setNotePinned = SetNotePinnedUseCase(repository),
        setNoteArchived = SetNoteArchivedUseCase(repository),
    )

    // ---------- opening ----------

    @Test
    fun `opening with no id gives a blank new note and touches no network`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository()
            val sut = viewModel(repository)

            sut.onIntent(NoteEditorIntent.Open(null))
            advanceUntilIdle()

            assertTrue(sut.state.value.isNewNote)
            assertEquals("", sut.state.value.draft.title)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `opening an existing note loads it with no unsaved changes`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 3, title = "Groceries", content = "Milk")))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(3))
        advanceUntilIdle()

        with(sut.state.value) {
            assertEquals(3L, noteId)
            assertEquals("Groceries", draft.title)
            assertEquals("Milk", draft.content)
            assertFalse(isNewNote)
            assertFalse(hasUnsavedChanges, "just-loaded content must not look edited")
            assertFalse(isLoading)
        }
    }

    @Test
    fun `reopening with a different id replaces the content`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "One"), note(id = 2, title = "Two")))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.Open(2))
        advanceUntilIdle()

        assertEquals("Two", sut.state.value.draft.title)
    }

    @Test
    fun `opening a blank editor after an existing note leaves nothing stale behind`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(listOf(note(id = 1, title = "One")))
            val sut = viewModel(repository)

            sut.onIntent(NoteEditorIntent.Open(1))
            advanceUntilIdle()
            sut.onIntent(NoteEditorIntent.Open(null))
            advanceUntilIdle()

            assertTrue(sut.state.value.isNewNote)
            assertEquals("", sut.state.value.draft.title)
        }

    @Test
    fun `a failed load is reported and can be retried`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1))).apply { nextError = AppError.Network }
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        assertEquals(AppError.Network, sut.state.value.error)

        sut.onIntent(NoteEditorIntent.Retry)
        advanceUntilIdle()

        assertEquals(null, sut.state.value.error)
        assertEquals("Title 1", sut.state.value.draft.title)
    }

    // ---------- editing ----------

    @Test
    fun `editing marks the note as having unsaved changes`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Old")))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.TitleChanged("New"))

        assertTrue(sut.state.value.hasUnsavedChanges)
    }

    @Test
    fun `typing the original value back means there is nothing to save`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Old")))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.TitleChanged("New"))
        sut.onIntent(NoteEditorIntent.TitleChanged("Old"))

        assertFalse(sut.state.value.hasUnsavedChanges)
    }

    @Test
    fun `an empty note cannot be saved`() = runTest(testDispatcher) {
        val sut = viewModel(FakeNoteRepository())

        sut.onIntent(NoteEditorIntent.Open(null))
        advanceUntilIdle()

        assertFalse(sut.state.value.canSave)

        sut.onIntent(NoteEditorIntent.ContentChanged("Milk"))
        assertTrue(sut.state.value.canSave)
    }

    // ---------- saving ----------

    @Test
    fun `saving a new note creates it and reports that it was created`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository()
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(null))
        sut.onIntent(NoteEditorIntent.TitleChanged("Groceries"))
        sut.onIntent(NoteEditorIntent.ContentChanged("Milk"))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        val saved = effects.ofType<NoteEditorEffect.Saved>().single()
        assertTrue(saved.wasCreated)
        assertFalse(sut.state.value.isNewNote, "the editor now holds a persisted note")
        assertFalse(sut.state.value.hasUnsavedChanges)
    }

    @Test
    fun `saving twice updates rather than creating a second note`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository()
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(null))
        sut.onIntent(NoteEditorIntent.TitleChanged("Groceries"))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        sut.onIntent(NoteEditorIntent.TitleChanged("Groceries and more"))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        assertEquals(1, repository.currentNotes().size, "a second note would be a duplicate")
        assertFalse(effects.ofType<NoteEditorEffect.Saved>().last().wasCreated)
    }

    @Test
    fun `saving an existing note updates it`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 4, title = "Old")))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(4))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.TitleChanged("New"))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        assertEquals("New", repository.currentNotes().single { it.id == 4L }.title)
        assertFalse(sut.state.value.hasUnsavedChanges)
    }

    /**
     * `wasCreated` is what tells the navigation host whether to close the editor: creating
     * returns the user to the list, editing leaves them where they are. Both halves are pinned
     * so the two flows cannot quietly converge.
     */
    @Test
    fun `saving an existing note reports that it was not created`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 4, title = "Old")))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(4))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.TitleChanged("New"))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        val saved = effects.ofType<NoteEditorEffect.Saved>().single()
        assertFalse(saved.wasCreated, "an edit must not be reported as a creation")
        assertEquals(4L, saved.id)
    }

    @Test
    fun `a validation failure fills in the field errors`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository()
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(null))
        sut.onIntent(NoteEditorIntent.TitleChanged("a".repeat(300)))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()

        assertIs<AppError.Validation>(sut.state.value.error)
        assertEquals(
            "Title must be at most 255 characters",
            sut.state.value.fieldErrors["title"],
        )
        assertFalse(sut.state.value.isSaving)
    }

    @Test
    fun `editing a field clears that field's error`() = runTest(testDispatcher) {
        val sut = viewModel(FakeNoteRepository())

        sut.onIntent(NoteEditorIntent.Open(null))
        sut.onIntent(NoteEditorIntent.TitleChanged("a".repeat(300)))
        sut.onIntent(NoteEditorIntent.Save)
        advanceUntilIdle()
        assertTrue(sut.state.value.fieldErrors.containsKey("title"))

        sut.onIntent(NoteEditorIntent.TitleChanged("Groceries"))

        assertFalse(sut.state.value.fieldErrors.containsKey("title"))
    }

    @Test
    fun `a network failure while saving keeps the edits so nothing is lost`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(listOf(note(id = 1, title = "Old")))
            val sut = viewModel(repository)
            val effects = recordEffects(sut.effects)

            sut.onIntent(NoteEditorIntent.Open(1))
            advanceUntilIdle()
            sut.onIntent(NoteEditorIntent.TitleChanged("New"))

            repository.nextError = AppError.Network
            sut.onIntent(NoteEditorIntent.Save)
            advanceUntilIdle()

            assertEquals("New", sut.state.value.draft.title)
            assertTrue(sut.state.value.hasUnsavedChanges)
            assertIs<NoteEditorEffect.ShowError>(effects.last)
        }

    // ---------- flags ----------

    @Test
    fun `pin and archive are unavailable until the note has been saved once`() =
        runTest(testDispatcher) {
            val sut = viewModel(FakeNoteRepository())

            sut.onIntent(NoteEditorIntent.Open(null))
            advanceUntilIdle()

            assertFalse(sut.state.value.canChangeFlags)
        }

    @Test
    fun `pinning updates the state`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.PinChanged(true))
        advanceUntilIdle()

        assertTrue(sut.state.value.pinned)
        assertTrue(sut.state.value.canChangeFlags)
    }

    @Test
    fun `archiving from the editor also unpins, matching the server`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = true)))
        val sut = viewModel(repository)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.ArchiveChanged(true))
        advanceUntilIdle()

        assertTrue(sut.state.value.archived)
        assertFalse(sut.state.value.pinned)
    }

    // ---------- deleting and closing ----------

    @Test
    fun `deleting an unsaved note just closes without calling the server`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository()
            val sut = viewModel(repository)
            val effects = recordEffects(sut.effects)

            sut.onIntent(NoteEditorIntent.Open(null))
            sut.onIntent(NoteEditorIntent.Delete)
            advanceUntilIdle()

            assertEquals(NoteEditorEffect.Closed, effects.last)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `deleting a saved note announces the deletion`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.Delete)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.Deleted, effects.last)
        assertTrue(repository.currentNotes().isEmpty())
    }

    @Test
    fun `closing with unsaved edits asks for confirmation first`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Old")))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.TitleChanged("New"))
        sut.onIntent(NoteEditorIntent.CloseRequested)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.ConfirmDiscard, effects.last)

        sut.onIntent(NoteEditorIntent.DiscardConfirmed)
        advanceUntilIdle()
        assertEquals(NoteEditorEffect.Closed, effects.last)
    }

    @Test
    fun `closing an unchanged note leaves straight away`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(listOf(note(id = 1)))
        val sut = viewModel(repository)
        val effects = recordEffects(sut.effects)

        sut.onIntent(NoteEditorIntent.Open(1))
        advanceUntilIdle()
        sut.onIntent(NoteEditorIntent.CloseRequested)
        advanceUntilIdle()

        assertEquals(NoteEditorEffect.Closed, effects.last)
    }
}
