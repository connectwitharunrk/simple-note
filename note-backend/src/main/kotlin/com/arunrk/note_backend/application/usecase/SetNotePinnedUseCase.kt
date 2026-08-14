package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Sets the pinned flag to an explicit value.
 *
 * Takes the desired state rather than toggling, so retrying the call is safe and two devices
 * acting at once cannot flip the note back and forth.
 */
@Service
class SetNotePinnedUseCase(
    private val noteRepository: NoteRepository,
    private val getNote: GetNoteUseCase,
    private val clock: Clock,
) {
    operator fun invoke(id: Long, pinned: Boolean): Note {
        val existing = getNote(id)
        if (existing.pinned == pinned) return existing
        return noteRepository.update(
            existing.copy(pinned = pinned, updatedAt = clock.instant()),
        )
    }
}
