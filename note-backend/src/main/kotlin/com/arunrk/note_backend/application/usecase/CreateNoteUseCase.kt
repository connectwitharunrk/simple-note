package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.application.command.CreateNoteCommand
import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class CreateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    operator fun invoke(command: CreateNoteCommand): Note {
        val now = clock.instant()
        return noteRepository.create(
            NewNote(
                title = command.title.trim(),
                content = command.content,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
