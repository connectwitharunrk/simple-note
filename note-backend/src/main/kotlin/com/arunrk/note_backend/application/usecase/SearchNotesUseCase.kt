package com.arunrk.note_backend.application.usecase

import com.arunrk.note_backend.domain.exception.InvalidSearchQueryException
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Service

/**
 * Searches titles and content within one archive bucket.
 *
 * The blank-query check lives here rather than as a controller annotation so it holds for
 * every caller, and so an empty search can never turn into "return the whole table".
 */
@Service
class SearchNotesUseCase(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(query: String, archived: Boolean = false): List<Note> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            throw InvalidSearchQueryException("Search query must not be blank")
        }
        return noteRepository.search(trimmed, archived)
    }
}
