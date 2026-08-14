package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service

@Service
class DeleteNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(id: Long) {
        if (!noteRepository.deleteById(id)) throw NoteNotFoundException(id)
    }
}
