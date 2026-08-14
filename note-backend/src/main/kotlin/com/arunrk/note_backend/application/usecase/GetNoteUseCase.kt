package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service

/**
 * Loads a note or fails.
 *
 * The other use cases that need an existing note delegate here rather than repeating the
 * find-or-throw pair, so "what happens when the id is unknown" is decided in one place.
 */
@Service
class GetNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(id: Long): Note =
        noteRepository.findById(id) ?: throw NoteNotFoundException(id)
}
