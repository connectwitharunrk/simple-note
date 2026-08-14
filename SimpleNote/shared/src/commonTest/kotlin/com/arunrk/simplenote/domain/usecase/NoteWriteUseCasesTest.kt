package com.arunrk.simplenote.domain.usecase

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.NoteLimits
import com.arunrk.simplenote.testsupport.FakeNoteRepository
import com.arunrk.simplenote.testsupport.expectFailure
import com.arunrk.simplenote.testsupport.expectSuccess
import com.arunrk.simplenote.testsupport.note
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateNoteUseCaseTest {

    @Test
    fun `creates a note`() = runTest {
        val repository = FakeNoteRepository()

        val created = CreateNoteUseCase(repository)(
            NoteDraft(title = "Groceries", content = "Milk"),
        ).expectSuccess()

        assertEquals("Groceries", created.title)
        assertEquals("Milk", created.content)
        assertEquals(1, repository.currentNotes().size)
    }

    @Test
    fun `trims the title but preserves content whitespace`() = runTest {
        val repository = FakeNoteRepository()

        val created = CreateNoteUseCase(repository)(
            NoteDraft(title = "  Groceries  ", content = "  - milk\n  - eggs\n"),
        ).expectSuccess()

        assertEquals("Groceries", created.title)
        assertEquals("  - milk\n  - eggs\n", created.content)
    }

    @Test
    fun `a note with only content is allowed`() = runTest {
        val repository = FakeNoteRepository()

        CreateNoteUseCase(repository)(NoteDraft(title = "", content = "Milk")).expectSuccess()
    }

    @Test
    fun `an empty note fails without ever calling the repository`() = runTest {
        val repository = FakeNoteRepository()

        val error = CreateNoteUseCase(repository)(
            NoteDraft(title = "   ", content = ""),
        ).expectFailure()

        assertIs<AppError.Validation>(error)
        assertEquals("A note must have a title or content", error.serverMessage)
        assertTrue(repository.calls.isEmpty(), "Should not have hit the network")
    }

    @Test
    fun `an over-long title fails locally with a field error`() = runTest {
        val repository = FakeNoteRepository()

        val error = CreateNoteUseCase(repository)(
            NoteDraft(title = "a".repeat(NoteLimits.MAX_TITLE_LENGTH + 1), content = "x"),
        ).expectFailure()

        assertIs<AppError.Validation>(error)
        assertEquals(
            "Title must be at most 255 characters",
            error.fieldErrors["title"],
        )
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `a title that is only over-long because of whitespace is accepted after trimming`() = runTest {
        val repository = FakeNoteRepository()
        val title = "a".repeat(NoteLimits.MAX_TITLE_LENGTH)

        CreateNoteUseCase(repository)(
            NoteDraft(title = "  $title  ", content = "x"),
        ).expectSuccess()
    }

    @Test
    fun `a repository failure is propagated`() = runTest {
        val repository = FakeNoteRepository().apply { nextError = AppError.Network }

        assertEquals(
            AppError.Network,
            CreateNoteUseCase(repository)(NoteDraft("Groceries", "Milk")).expectFailure(),
        )
    }
}

class UpdateNoteUseCaseTest {

    @Test
    fun `updates title and content and moves updatedAt`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, title = "Old", content = "Old")))

        val updated = UpdateNoteUseCase(repository)(1, NoteDraft("New", "New body")).expectSuccess()

        assertEquals("New", updated.title)
        assertEquals("New body", updated.content)
        assertTrue(updated.updatedAt > updated.createdAt)
    }

    @Test
    fun `leaves the pinned flag alone`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = true)))

        val updated = UpdateNoteUseCase(repository)(1, NoteDraft("New", "New body")).expectSuccess()

        assertTrue(updated.pinned)
    }

    @Test
    fun `an empty draft fails locally`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1)))

        assertIs<AppError.Validation>(
            UpdateNoteUseCase(repository)(1, NoteDraft("", "")).expectFailure(),
        )
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `an unknown note reports not found`() = runTest {
        val repository = FakeNoteRepository()

        assertIs<AppError.NotFound>(
            UpdateNoteUseCase(repository)(404, NoteDraft("New", "Body")).expectFailure(),
        )
    }
}

class DeleteNoteUseCaseTest {

    @Test
    fun `deletes the note`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1), note(id = 2)))

        DeleteNoteUseCase(repository)(1).expectSuccess()

        assertEquals(listOf(2L), repository.currentNotes().map { it.id })
    }

    @Test
    fun `an unknown note reports not found`() = runTest {
        assertIs<AppError.NotFound>(DeleteNoteUseCase(FakeNoteRepository())(404).expectFailure())
    }
}

class SetNotePinnedUseCaseTest {

    @Test
    fun `pins a note`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = false)))

        assertTrue(SetNotePinnedUseCase(repository)(1, pinned = true).expectSuccess().pinned)
    }

    @Test
    fun `unpins a note by passing the value rather than toggling`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = true)))
        val useCase = SetNotePinnedUseCase(repository)

        useCase(1, pinned = false).expectSuccess()
        // Calling again with the same value must be idempotent, not a toggle back to pinned.
        assertEquals(false, useCase(1, pinned = false).expectSuccess().pinned)
    }

    @Test
    fun `a network failure is reported`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1))).apply { nextError = AppError.Network }

        assertEquals(AppError.Network, SetNotePinnedUseCase(repository)(1, true).expectFailure())
    }
}

class SetNoteArchivedUseCaseTest {

    @Test
    fun `archives a note and unpins it`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, pinned = true)))

        val archived = SetNoteArchivedUseCase(repository)(1, archived = true).expectSuccess()

        assertTrue(archived.archived)
        assertEquals(false, archived.pinned)
    }

    @Test
    fun `unarchives a note`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 1, archived = true)))

        assertEquals(
            false,
            SetNoteArchivedUseCase(repository)(1, archived = false).expectSuccess().archived,
        )
    }
}

class GetNoteUseCaseTest {

    @Test
    fun `returns the note`() = runTest {
        val repository = FakeNoteRepository(listOf(note(id = 7, title = "Groceries")))

        assertEquals("Groceries", GetNoteUseCase(repository)(7).expectSuccess().title)
    }

    @Test
    fun `reports not found for an unknown id`() = runTest {
        assertIs<AppError.NotFound>(GetNoteUseCase(FakeNoteRepository())(404).expectFailure())
    }
}
