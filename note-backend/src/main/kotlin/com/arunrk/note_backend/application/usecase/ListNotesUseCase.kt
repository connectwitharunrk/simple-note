package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service

/** Lists either the active notes or the archived ones, pinned first. */
@Service
class ListNotesUseCase(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(archived: Boolean = false): List<Note> =
        noteRepository.findAll(archived)
}
