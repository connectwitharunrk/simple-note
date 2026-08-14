package com.arunrk.simplenote.domain.usecase

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.domain.repository.NoteRepository

/**
 * Returns the notes the user should currently be looking at.
 *
 * Listing and searching are one use case rather than two because from the screen's point of
 * view they are the same question — "what belongs on screen given the current filter and
 * search box". Keeping the choice here rather than in the ViewModel means the rule
 * "an empty search box means show everything" is stated once and is directly testable.
 */
class GetNotesUseCase(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(
        filter: NoteFilter = NoteFilter.Active,
        query: String = "",
    ): AppResult<List<Note>> {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            noteRepository.getNotes(filter)
        } else {
            noteRepository.searchNotes(trimmedQuery, filter)
        }
    }
}
