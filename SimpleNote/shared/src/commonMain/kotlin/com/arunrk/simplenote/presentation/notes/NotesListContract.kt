package com.arunrk.simplenote.presentation.notes

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteFilter

/**
 * Everything true about the notes list right now.
 *
 * The screen is a pure function of this: given the same state it always renders the same
 * thing, which is what makes the UI trivially previewable and the ViewModel testable without
 * a single Composable.
 */
data class NotesListState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val filter: NoteFilter = NoteFilter.Active,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    /** Drives the detail pane on tablet and desktop; ignored in single-pane layouts. */
    val selectedNoteId: Long? = null,
) {
    val isSearching: Boolean get() = query.isNotBlank()

    /**
     * True only when there is genuinely nothing to show — not while still loading, and not
     * when the list is empty because a request failed. Distinguishing these is what stops the
     * UI from claiming "no notes yet" when it simply could not reach the server.
     */
    val isEmpty: Boolean get() = notes.isEmpty() && !isLoading && error == null
}

/** Everything the user can do on this screen. The only way into the ViewModel. */
sealed interface NotesListIntent {
    data object Load : NotesListIntent
    data object Refresh : NotesListIntent
    data object Retry : NotesListIntent
    data class QueryChanged(val query: String) : NotesListIntent
    data object QueryCleared : NotesListIntent
    data class FilterChanged(val filter: NoteFilter) : NotesListIntent
    data class NoteSelected(val id: Long) : NotesListIntent
    data object CreateNoteRequested : NotesListIntent
    data object SelectionCleared : NotesListIntent
    data class PinChanged(val id: Long, val pinned: Boolean) : NotesListIntent
    data class ArchiveChanged(val id: Long, val archived: Boolean) : NotesListIntent
    data class DeleteRequested(val id: Long) : NotesListIntent
    /** The list changed elsewhere (the editor saved or deleted) and needs re-reading. */
    data object InvalidateCache : NotesListIntent
}

/**
 * One-shot events.
 *
 * These carry facts, not sentences — the UI decides the wording. That keeps the ViewModel
 * free of copy and makes localisation a presentation-layer concern.
 */
sealed interface NotesListEffect {
    data class OpenNote(val id: Long) : NotesListEffect
    data object OpenNewNote : NotesListEffect
    data object NoteDeleted : NotesListEffect
    data class NoteArchiveChanged(val archived: Boolean) : NotesListEffect
    data class ShowError(val error: AppError) : NotesListEffect
}
