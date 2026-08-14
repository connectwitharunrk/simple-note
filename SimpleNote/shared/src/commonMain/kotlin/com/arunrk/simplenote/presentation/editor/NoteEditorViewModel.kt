package com.arunrk.simplenote.presentation.editor

import androidx.lifecycle.viewModelScope
import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.core.mvi.MviViewModel
import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.toDraft
import com.arunrk.simplenote.domain.usecase.CreateNoteUseCase
import com.arunrk.simplenote.domain.usecase.DeleteNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNoteUseCase
import com.arunrk.simplenote.domain.usecase.SetNoteArchivedUseCase
import com.arunrk.simplenote.domain.usecase.SetNotePinnedUseCase
import com.arunrk.simplenote.domain.usecase.UpdateNoteUseCase
import kotlinx.coroutines.launch

/**
 * Store for creating and editing a note.
 *
 * One store handles both cases because they differ in exactly one place — whether [Save]
 * creates or updates — and a separate "new note" screen would duplicate the editing,
 * validation and error handling to avoid a single `if`.
 *
 * The note id is supplied by an intent rather than by the constructor, so the same instance
 * can follow the selection in the tablet and desktop two-pane layout without being recreated.
 */
class NoteEditorViewModel(
    private val getNote: GetNoteUseCase,
    private val createNote: CreateNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setNotePinned: SetNotePinnedUseCase,
    private val setNoteArchived: SetNoteArchivedUseCase,
) : MviViewModel<NoteEditorIntent, NoteEditorState, NoteEditorEffect>(NoteEditorState()) {

    override fun onIntent(intent: NoteEditorIntent) {
        when (intent) {
            is NoteEditorIntent.Open -> open(intent.id)

            NoteEditorIntent.Retry -> currentState.noteId?.let { load(it) }

            is NoteEditorIntent.TitleChanged -> updateState {
                // Clearing the field error as soon as the user edits that field keeps a stale
                // complaint from sitting under an input they have already fixed.
                copy(
                    draft = draft.copy(title = intent.title),
                    fieldErrors = fieldErrors - "title",
                    error = null,
                )
            }

            is NoteEditorIntent.ContentChanged -> updateState {
                copy(
                    draft = draft.copy(content = intent.content),
                    fieldErrors = fieldErrors - "content",
                    error = null,
                )
            }

            NoteEditorIntent.Save -> save()

            NoteEditorIntent.Delete -> delete()

            is NoteEditorIntent.PinChanged -> changePin(intent.pinned)

            is NoteEditorIntent.ArchiveChanged -> changeArchive(intent.archived)

            NoteEditorIntent.CloseRequested ->
                if (currentState.hasUnsavedChanges) {
                    emitEffect(NoteEditorEffect.ConfirmDiscard)
                } else {
                    emitEffect(NoteEditorEffect.Closed)
                }

            NoteEditorIntent.DiscardConfirmed -> emitEffect(NoteEditorEffect.Closed)
        }
    }

    private fun open(id: Long?) {
        if (id == null) {
            // A blank editor needs no request; reset so a reused instance shows nothing stale.
            updateState { NoteEditorState() }
        } else {
            load(id)
        }
    }

    private fun load(id: Long) {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getNote(id)) {
                is AppResult.Success -> updateState { applyLoaded(result.data) }
                is AppResult.Failure -> updateState {
                    copy(noteId = id, isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun save() {
        val state = currentState
        if (!state.canSave) return

        updateState { copy(isSaving = true, error = null, fieldErrors = emptyMap()) }

        viewModelScope.launch {
            val existingId = state.noteId
            val result = if (existingId == null) {
                createNote(state.draft)
            } else {
                updateNote(existingId, state.draft)
            }

            when (result) {
                is AppResult.Success -> {
                    updateState { applyLoaded(result.data).copy(isSaving = false) }
                    emitEffect(NoteEditorEffect.Saved(result.data.id, wasCreated = existingId == null))
                }

                is AppResult.Failure -> {
                    updateState {
                        copy(
                            isSaving = false,
                            error = result.error,
                            fieldErrors = (result.error as? AppError.Validation)?.fieldErrors.orEmpty(),
                        )
                    }
                    emitEffect(NoteEditorEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun delete() {
        val id = currentState.noteId ?: run {
            // Nothing was ever saved, so there is nothing to delete — just leave.
            emitEffect(NoteEditorEffect.Closed)
            return
        }

        viewModelScope.launch {
            when (val result = deleteNote(id)) {
                is AppResult.Success -> emitEffect(NoteEditorEffect.Deleted)
                is AppResult.Failure -> emitEffect(NoteEditorEffect.ShowError(result.error))
            }
        }
    }

    private fun changePin(pinned: Boolean) {
        val id = currentState.noteId ?: return
        viewModelScope.launch {
            when (val result = setNotePinned(id, pinned)) {
                is AppResult.Success -> updateState { applyLoaded(result.data) }
                is AppResult.Failure -> emitEffect(NoteEditorEffect.ShowError(result.error))
            }
        }
    }

    private fun changeArchive(archived: Boolean) {
        val id = currentState.noteId ?: return
        viewModelScope.launch {
            when (val result = setNoteArchived(id, archived)) {
                is AppResult.Success -> updateState { applyLoaded(result.data) }
                is AppResult.Failure -> emitEffect(NoteEditorEffect.ShowError(result.error))
            }
        }
    }

    /**
     * Adopts a note from the server as the new baseline.
     *
     * [NoteEditorState.savedDraft] is set to the same value as [NoteEditorState.draft], so
     * immediately after a load or save there are no unsaved changes and closing the screen
     * will not prompt.
     */
    private fun NoteEditorState.applyLoaded(note: Note): NoteEditorState {
        val draft = note.toDraft()
        return copy(
            noteId = note.id,
            draft = draft,
            savedDraft = draft,
            pinned = note.pinned,
            archived = note.archived,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            isLoading = false,
            error = null,
            fieldErrors = emptyMap(),
        )
    }
}
