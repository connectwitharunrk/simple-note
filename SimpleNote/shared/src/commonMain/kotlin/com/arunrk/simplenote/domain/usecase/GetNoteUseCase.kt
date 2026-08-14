package com.arunrk.simplenote.domain.usecase

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.repository.NoteRepository

/** Loads a single note, for the editor and the detail pane. */
class GetNoteUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: Long): AppResult<Note> = noteRepository.getNote(id)
}
