package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Sets the archived flag to an explicit value, for the same idempotency reason as
 * [SetNotePinnedUseCase].
 *
 * Archiving also unpins: a note the user has filed away should not keep occupying the top of
 * the list when it is later restored.
 */
@Service
class SetNoteArchivedUseCase(
    private val noteRepository: NoteRepository,
    private val getNote: GetNoteUseCase,
    private val clock: Clock,
) {
    operator fun invoke(id: Long, archived: Boolean): Note {
        val existing = getNote(id)
        if (existing.archived == archived) return existing
        return noteRepository.update(
            existing.copy(
                archived = archived,
                pinned = if (archived) false else existing.pinned,
                updatedAt = clock.instant(),
            ),
        )
    }
}
