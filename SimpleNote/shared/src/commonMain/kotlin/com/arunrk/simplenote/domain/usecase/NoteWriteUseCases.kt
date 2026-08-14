package com.arunrk.simplenote.domain.usecase

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.core.result.asFailure
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.normalized
import com.arunrk.simplenote.domain.model.validationError
import com.arunrk.simplenote.domain.repository.NoteRepository

/**
 * Creates a note.
 *
 * Validates before calling out, so an empty note fails instantly and offline instead of after
 * a round trip. The backend still enforces the same rule — this is a courtesy to the user,
 * not the security boundary.
 */
class CreateNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(draft: NoteDraft): AppResult<Note> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return it.asFailure() }
        return noteRepository.createNote(normalized)
    }
}

/** Replaces a note's title and content, leaving its pinned and archived flags alone. */
class UpdateNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: Long, draft: NoteDraft): AppResult<Note> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return it.asFailure() }
        return noteRepository.updateNote(id, normalized)
    }
}

class DeleteNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> = noteRepository.deleteNote(id)
}

/**
 * Sets the pinned flag to an explicit value rather than toggling it.
 *
 * The caller passes what it wants to be true, so a retry after a failed request cannot flip
 * the note to the opposite of what the user asked for.
 */
class SetNotePinnedUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: Long, pinned: Boolean): AppResult<Note> =
        noteRepository.setPinned(id, pinned)
}

/** Sets the archived flag to an explicit value, for the same reason as [SetNotePinnedUseCase]. */
class SetNoteArchivedUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: Long, archived: Boolean): AppResult<Note> =
        noteRepository.setArchived(id, archived)
}
