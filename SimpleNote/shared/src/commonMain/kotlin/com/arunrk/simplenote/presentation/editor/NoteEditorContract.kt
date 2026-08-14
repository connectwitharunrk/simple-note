package com.arunrk.simplenote.presentation.editor

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.NoteDraft
import kotlin.time.Instant

/**
 * State of the note editor, used for both creating and editing.
 *
 * [savedDraft] is kept alongside [draft] so the screen can tell whether there is anything to
 * save — that is what drives the "discard changes?" prompt and stops a pointless PUT when the
 * user opened a note and changed nothing.
 */
data class NoteEditorState(
    /** `null` until the note exists on the server, which is what makes this a new note. */
    val noteId: Long? = null,
    val draft: NoteDraft = NoteDraft.Empty,
    val savedDraft: NoteDraft = NoteDraft.Empty,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    val isNewNote: Boolean get() = noteId == null

    val hasUnsavedChanges: Boolean get() = draft != savedDraft

    val hasContent: Boolean get() = draft.title.isNotBlank() || draft.content.isNotBlank()

    /** Saving an empty note would only earn a rejection, so the action is disabled instead. */
    val canSave: Boolean get() = hasContent && !isSaving && !isLoading

    /** Pin and archive act on a note that exists; they are unavailable until the first save. */
    val canChangeFlags: Boolean get() = noteId != null && !isSaving
}

sealed interface NoteEditorIntent {
    /** Opens an existing note, or a blank editor when [id] is null. */
    data class Open(val id: Long?) : NoteEditorIntent
    data object Retry : NoteEditorIntent
    data class TitleChanged(val title: String) : NoteEditorIntent
    data class ContentChanged(val content: String) : NoteEditorIntent
    data object Save : NoteEditorIntent
    data object Delete : NoteEditorIntent
    data class PinChanged(val pinned: Boolean) : NoteEditorIntent
    data class ArchiveChanged(val archived: Boolean) : NoteEditorIntent
    data object CloseRequested : NoteEditorIntent
    /** The user confirmed they want to leave without saving. */
    data object DiscardConfirmed : NoteEditorIntent
}

sealed interface NoteEditorEffect {
    data class Saved(val id: Long, val wasCreated: Boolean) : NoteEditorEffect
    data object Deleted : NoteEditorEffect
    data object Closed : NoteEditorEffect
    /** Ask the user to confirm leaving with unsaved edits. */
    data object ConfirmDiscard : NoteEditorEffect
    data class ShowError(val error: AppError) : NoteEditorEffect
}
