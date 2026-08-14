package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.application.command.UpdateNoteCommand
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Replaces a note's title and content.
 *
 * Pin and archive are deliberately not editable here: they have their own idempotent
 * endpoints, so a plain edit can never silently unpin a note.
 */
@Service
class UpdateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val getNote: GetNoteUseCase,
    private val clock: Clock,
) {
    operator fun invoke(command: UpdateNoteCommand): Note {
        val existing = getNote(command.id)
        return noteRepository.update(
            existing.copy(
                title = command.title.trim(),
                content = command.content,
                updatedAt = clock.instant(),
            ),
        )
    }
}
