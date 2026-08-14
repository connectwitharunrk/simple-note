package com.arunrk.simplenote.presentation.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.presentation.components.EmptyState
import com.arunrk.simplenote.presentation.components.ErrorState
import com.arunrk.simplenote.presentation.components.LoadingState
import com.arunrk.simplenote.presentation.components.NoteCard

/**
 * The notes list.
 *
 * Stateless with respect to the store: it receives a [NotesListState] and reports back through
 * [onIntent]. That is what lets every behaviour be covered by ViewModel tests without a UI
 * test, and it means this file contains no logic beyond choosing what to draw.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    state: NotesListState,
    onIntent: (NotesListIntent) -> Unit,
    modifier: Modifier = Modifier,
    showSelection: Boolean = false,
) {
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.filter == NoteFilter.Archived) "Archived" else "Notes") },
            actions = {
                IconButton(
                    onClick = { onIntent(NotesListIntent.Refresh) },
                    enabled = !state.isRefreshing,
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    } else {
                        Text("⟳", style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = { onIntent(NotesListIntent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search notes") },
            singleLine = true,
            trailingIcon = {
                if (state.isSearching) {
                    IconButton(onClick = { onIntent(NotesListIntent.QueryCleared) }) {
                        Text("✕")
                    }
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NoteFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onIntent(NotesListIntent.FilterChanged(filter)) },
                    label = { Text(if (filter == NoteFilter.Active) "Active" else "Archived") },
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                // Order matters: an error must win over "empty", or a failed request would
                // be reported to the user as "you have no notes".
                state.error != null && state.notes.isEmpty() -> ErrorState(
                    error = state.error,
                    onRetry = { onIntent(NotesListIntent.Retry) },
                )

                state.isLoading -> LoadingState()

                state.isEmpty -> EmptyState(
                    title = if (state.isSearching) "No matches" else "No notes yet",
                    description = if (state.isSearching) {
                        "Nothing matches \"${state.query}\". Try a different search."
                    } else if (state.filter == NoteFilter.Archived) {
                        "Notes you archive will appear here."
                    } else {
                        "Tap + to write your first note."
                    },
                )

                else -> NotesList(
                    state = state,
                    onIntent = onIntent,
                    showSelection = showSelection,
                    onDeleteRequested = { pendingDeleteId = it },
                )
            }

            FloatingActionButton(
                onClick = { onIntent(NotesListIntent.CreateNoteRequested) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId = null
                        onIntent(NotesListIntent.DeleteRequested(id))
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun NotesList(
    state: NotesListState,
    onIntent: (NotesListIntent) -> Unit,
    showSelection: Boolean,
    onDeleteRequested: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom padding clears the floating action button so the last card is never hidden
        // behind it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Keyed so Compose reuses the right card when the list reorders after a pin toggle,
        // instead of animating content between unrelated rows.
        items(items = state.notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                isSelected = showSelection && state.selectedNoteId == note.id,
                onClick = { onIntent(NotesListIntent.NoteSelected(note.id)) },
                onPinChanged = { onIntent(NotesListIntent.PinChanged(note.id, it)) },
                onArchiveChanged = { onIntent(NotesListIntent.ArchiveChanged(note.id, it)) },
                onDelete = { onDeleteRequested(note.id) },
            )
        }
    }
}

/**
 * A banner for a failure that happened while notes are already on screen.
 *
 * A full-screen error would throw away content the user can still read and act on, so a
 * partial failure is reported without replacing the list.
 */
@Composable
fun InlineErrorBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}
