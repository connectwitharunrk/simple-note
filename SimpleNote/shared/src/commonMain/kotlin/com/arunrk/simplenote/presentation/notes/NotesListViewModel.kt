package com.arunrk.simplenote.presentation.notes

import androidx.lifecycle.viewModelScope
import com.arunrk.simplenote.core.mvi.MviViewModel
import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.domain.model.inListOrder
import com.arunrk.simplenote.domain.usecase.DeleteNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNotesUseCase
import com.arunrk.simplenote.domain.usecase.SetNoteArchivedUseCase
import com.arunrk.simplenote.domain.usecase.SetNotePinnedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Store for the notes list.
 *
 * Loading is driven by a single flow rather than by scattered `launch` calls. Every reason to
 * reload — first load, a keystroke, a filter switch, pull-to-refresh, retry — becomes a new
 * [LoadRequest], and `flatMapLatest` cancels whatever was in flight. That is what makes fast
 * typing correct: the response for "mil" can never arrive after the response for "milk" and
 * overwrite it.
 */
class NotesListViewModel(
    private val getNotes: GetNotesUseCase,
    private val setNotePinned: SetNotePinnedUseCase,
    private val setNoteArchived: SetNoteArchivedUseCase,
    private val deleteNote: DeleteNoteUseCase,
) : MviViewModel<NotesListIntent, NotesListState, NotesListEffect>(NotesListState()) {

    /**
     * @param debounced set only for typing. Filter switches and refreshes must feel instant,
     *   so they skip the delay rather than waiting out a search debounce.
     * @param reloadToken makes an otherwise identical request distinct, so that pull-to-
     *   refresh and retry re-trigger the flow even when nothing else changed.
     */
    private data class LoadRequest(
        val query: String = "",
        val filter: NoteFilter = NoteFilter.Active,
        val debounced: Boolean = false,
        val isRefresh: Boolean = false,
        val reloadToken: Long = 0,
    )

    private val loadRequests = MutableStateFlow(LoadRequest())
    private var reloadToken = 0L

    /**
     * The pipeline is started once. `Load` is sent from the UI whenever the screen appears,
     * which happens again on every configuration change, and starting a second collector each
     * time would leave duplicates racing to write the same state.
     */
    private var isPipelineStarted = false

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun startLoading() {
        if (isPipelineStarted) return
        isPipelineStarted = true

        loadRequests
            .debounce { request -> if (request.debounced) SEARCH_DEBOUNCE_MILLIS else 0L }
            .flatMapLatest { request ->
                flow {
                    emit(LoadPhase.Started(request.isRefresh))
                    emit(LoadPhase.Finished(getNotes(request.filter, request.query)))
                }
            }
            .onEach(::reduceLoadPhase)
            .launchIn(viewModelScope)
    }

    private sealed interface LoadPhase {
        data class Started(val isRefresh: Boolean) : LoadPhase
        data class Finished(val result: AppResult<List<Note>>) : LoadPhase
    }

    private fun reduceLoadPhase(phase: LoadPhase) = when (phase) {
        is LoadPhase.Started -> updateState {
            copy(
                // A refresh keeps the current list visible and shows its own indicator;
                // a first load or a new search replaces the list, so it shows a spinner.
                isLoading = !phase.isRefresh,
                isRefreshing = phase.isRefresh,
                error = null,
            )
        }

        is LoadPhase.Finished -> when (val result = phase.result) {
            is AppResult.Success -> updateState {
                copy(
                    notes = result.data,
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            }

            is AppResult.Failure -> updateState {
                copy(isLoading = false, isRefreshing = false, error = result.error)
            }
        }
    }

    override fun onIntent(intent: NotesListIntent) {
        when (intent) {
            NotesListIntent.Load -> startLoading()

            NotesListIntent.Refresh -> requestLoad(isRefresh = true)

            NotesListIntent.Retry, NotesListIntent.InvalidateCache -> requestLoad()

            is NotesListIntent.QueryChanged -> {
                updateState { copy(query = intent.query) }
                requestLoad(debounced = true)
            }

            NotesListIntent.QueryCleared -> {
                updateState { copy(query = "") }
                requestLoad()
            }

            is NotesListIntent.FilterChanged -> {
                updateState { copy(filter = intent.filter, selectedNoteId = null) }
                requestLoad()
            }

            is NotesListIntent.NoteSelected -> {
                updateState { copy(selectedNoteId = intent.id) }
                emitEffect(NotesListEffect.OpenNote(intent.id))
            }

            NotesListIntent.SelectionCleared -> updateState { copy(selectedNoteId = null) }

            NotesListIntent.CreateNoteRequested -> emitEffect(NotesListEffect.OpenNewNote)

            is NotesListIntent.PinChanged -> changePin(intent.id, intent.pinned)

            is NotesListIntent.ArchiveChanged -> changeArchive(intent.id, intent.archived)

            is NotesListIntent.DeleteRequested -> delete(intent.id)
        }
    }

    private fun requestLoad(debounced: Boolean = false, isRefresh: Boolean = false) {
        loadRequests.value = LoadRequest(
            query = currentState.query,
            filter = currentState.filter,
            debounced = debounced,
            isRefresh = isRefresh,
            reloadToken = if (debounced) reloadToken else ++reloadToken,
        )
    }

    /**
     * Applies the pin change immediately, then reconciles with the server.
     *
     * Waiting for a round trip before moving the note would make the list feel dead on a slow
     * connection. The previous list is captured so a failure restores exactly what was there
     * rather than guessing at the inverse.
     */
    private fun changePin(id: Long, pinned: Boolean) {
        val previousNotes = currentState.notes
        updateState {
            copy(
                notes = notes.map { if (it.id == id) it.copy(pinned = pinned) else it }.inListOrder(),
            )
        }

        viewModelScope.launch {
            when (val result = setNotePinned(id, pinned)) {
                is AppResult.Success -> updateState {
                    copy(
                        notes = notes.map { if (it.id == id) result.data else it }.inListOrder(),
                    )
                }

                is AppResult.Failure -> {
                    updateState { copy(notes = previousNotes) }
                    emitEffect(NotesListEffect.ShowError(result.error))
                }
            }
        }
    }

    /**
     * Archiving moves a note out of the bucket being viewed, so on success it is dropped from
     * the list rather than updated in place.
     */
    private fun changeArchive(id: Long, archived: Boolean) {
        viewModelScope.launch {
            when (val result = setNoteArchived(id, archived)) {
                is AppResult.Success -> {
                    removeFromList(id)
                    emitEffect(NotesListEffect.NoteArchiveChanged(archived))
                }

                is AppResult.Failure -> emitEffect(NotesListEffect.ShowError(result.error))
            }
        }
    }

    private fun delete(id: Long) {
        viewModelScope.launch {
            when (val result = deleteNote(id)) {
                is AppResult.Success -> {
                    removeFromList(id)
                    emitEffect(NotesListEffect.NoteDeleted)
                }

                is AppResult.Failure -> emitEffect(NotesListEffect.ShowError(result.error))
            }
        }
    }

    private fun removeFromList(id: Long) = updateState {
        copy(
            notes = notes.filterNot { it.id == id },
            selectedNoteId = selectedNoteId.takeIf { it != id },
        )
    }

    private companion object {
        /**
         * Long enough that an average typist issues one request per word rather than per
         * keystroke, short enough that results still feel immediate.
         */
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
