package com.arunrk.simplenote.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arunrk.simplenote.presentation.editor.NoteEditorEffect
import com.arunrk.simplenote.presentation.editor.NoteEditorIntent
import com.arunrk.simplenote.presentation.editor.NoteEditorScreen
import com.arunrk.simplenote.presentation.editor.NoteEditorViewModel
import com.arunrk.simplenote.presentation.error.displayMessage
import com.arunrk.simplenote.presentation.notes.NotesListEffect
import com.arunrk.simplenote.presentation.notes.NotesListIntent
import com.arunrk.simplenote.presentation.notes.NotesListScreen
import com.arunrk.simplenote.presentation.notes.NotesListViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Width at which the window is wide enough to show list and editor side by side.
 *
 * 840dp is Material's medium/expanded boundary: a phone in either orientation stays below it,
 * while a tablet in landscape, an iPad, and any reasonable desktop window sit above.
 */
private val TwoPaneBreakpoint = 840.dp

/**
 * Hosts the list and the editor, and owns navigation between them.
 *
 * Navigation is a single boolean plus the list's selected id rather than a navigation library:
 * there are exactly two destinations, and in the two-pane layout they are both on screen at
 * once, which is a poor fit for a back stack anyway.
 *
 * The same composable serves phone, tablet, iPad and desktop — only [BoxWithConstraints]
 * decides which arrangement to use, so a desktop window being resized switches layout live.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val listViewModel: NotesListViewModel = koinViewModel()
    val editorViewModel: NoteEditorViewModel = koinViewModel()

    val listState by listViewModel.state.collectAsStateWithLifecycle()
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isEditorOpen by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { listViewModel.onIntent(NotesListIntent.Load) }

    LaunchedEffect(listViewModel) {
        listViewModel.effects.collect { effect ->
            when (effect) {
                is NotesListEffect.OpenNote -> {
                    editorViewModel.onIntent(NoteEditorIntent.Open(effect.id))
                    isEditorOpen = true
                }

                NotesListEffect.OpenNewNote -> {
                    editorViewModel.onIntent(NoteEditorIntent.Open(null))
                    isEditorOpen = true
                }

                NotesListEffect.NoteDeleted ->
                    scope.launch { snackbarHostState.showSnackbar("Note deleted") }

                is NotesListEffect.NoteArchiveChanged -> scope.launch {
                    snackbarHostState.showSnackbar(
                        if (effect.archived) "Note archived" else "Note restored",
                    )
                }

                is NotesListEffect.ShowError ->
                    scope.launch { snackbarHostState.showSnackbar(effect.error.displayMessage()) }
            }
        }
    }

    LaunchedEffect(editorViewModel) {
        editorViewModel.effects.collect { effect ->
            when (effect) {
                is NoteEditorEffect.Saved -> {
                    // The list is re-read rather than patched in place: the server owns
                    // ordering and updatedAt, so asking it again is both simpler and correct.
                    listViewModel.onIntent(NotesListIntent.InvalidateCache)
                    snackbarHostState.showSnackbar(
                        if (effect.wasCreated) "Note created" else "Note saved",
                    )
                }

                NoteEditorEffect.Deleted -> {
                    listViewModel.onIntent(NotesListIntent.InvalidateCache)
                    closeEditor(
                        onClosed = { isEditorOpen = false },
                        clearSelection = { listViewModel.onIntent(NotesListIntent.SelectionCleared) },
                    )
                    snackbarHostState.showSnackbar("Note deleted")
                }

                NoteEditorEffect.Closed -> closeEditor(
                    onClosed = { isEditorOpen = false },
                    clearSelection = { listViewModel.onIntent(NotesListIntent.SelectionCleared) },
                )

                NoteEditorEffect.ConfirmDiscard -> showDiscardPrompt = true

                is NoteEditorEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.error.displayMessage())
            }
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isTwoPane = maxWidth >= TwoPaneBreakpoint

            if (isTwoPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NotesListScreen(
                        state = listState,
                        onIntent = listViewModel::onIntent,
                        modifier = Modifier.width(400.dp),
                        showSelection = true,
                    )
                    VerticalDivider()
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isEditorOpen) {
                            NoteEditorScreen(
                                state = editorState,
                                onIntent = editorViewModel::onIntent,
                                // Nothing to close back to — the list is already beside it.
                                showCloseAction = false,
                            )
                        } else {
                            NoDetailSelected()
                        }
                    }
                }
            } else {
                if (isEditorOpen) {
                    NoteEditorScreen(
                        state = editorState,
                        onIntent = editorViewModel::onIntent,
                    )
                } else {
                    NotesListScreen(
                        state = listState,
                        onIntent = listViewModel::onIntent,
                    )
                }
            }
        }
    }

    if (showDiscardPrompt) {
        DiscardChangesDialog(
            onDismiss = { showDiscardPrompt = false },
            onDiscard = {
                showDiscardPrompt = false
                editorViewModel.onIntent(NoteEditorIntent.DiscardConfirmed)
            },
        )
    }
}

private fun closeEditor(onClosed: () -> Unit, clearSelection: () -> Unit) {
    onClosed()
    clearSelection()
}

@Composable
private fun NoDetailSelected() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Select a note, or create a new one",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun DiscardChangesDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = { Text("You have unsaved edits to this note.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Keep editing") }
        },
    )
}
