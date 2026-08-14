package com.arunrk.simplenote.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.arunrk.simplenote.presentation.components.ErrorState
import com.arunrk.simplenote.presentation.components.LoadingState
import com.arunrk.simplenote.presentation.error.displayMessage
import com.arunrk.simplenote.presentation.format.formatForDisplay

/**
 * The note editor, used for both new and existing notes.
 *
 * Like the list, it holds no logic of its own beyond dialog visibility — everything else is
 * read from [NoteEditorState] and reported through [onIntent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    state: NoteEditorState,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    showCloseAction: Boolean = true,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    // Sized by the caller, for the same reason as NotesListScreen.
    Column(modifier = modifier) {
        TopAppBar(
            title = { Text(if (state.isNewNote) "New note" else "Edit note") },
            navigationIcon = {
                if (showCloseAction) {
                    IconButton(onClick = { onIntent(NoteEditorIntent.CloseRequested) }) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            actions = {
                if (state.canChangeFlags) {
                    IconButton(onClick = { onIntent(NoteEditorIntent.PinChanged(!state.pinned)) }) {
                        Text(
                            text = if (state.pinned) "★" else "☆",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (state.pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                    }
                    IconButton(
                        onClick = { onIntent(NoteEditorIntent.ArchiveChanged(!state.archived)) },
                    ) {
                        Text(
                            text = if (state.archived) "⇱" else "⇲",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Text(
                            text = "🗑",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                } else {
                    TextButton(
                        onClick = { onIntent(NoteEditorIntent.Save) },
                        enabled = state.canSave && state.hasUnsavedChanges,
                    ) {
                        Text("Save")
                    }
                }
            },
        )

        when {
            state.isLoading -> LoadingState()

            // Only a load failure blanks the screen. A failed *save* keeps the editor and its
            // text visible, because throwing away what the user typed would be far worse than
            // the failure itself.
            state.error != null && state.noteId != null && !state.hasContent -> ErrorState(
                error = state.error,
                onRetry = { onIntent(NoteEditorIntent.Retry) },
            )

            else -> EditorFields(state = state, onIntent = onIntent)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onIntent(NoteEditorIntent.Delete)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditorFields(
    state: NoteEditorState,
    onIntent: (NoteEditorIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.draft.title,
            onValueChange = { onIntent(NoteEditorIntent.TitleChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Title") },
            textStyle = MaterialTheme.typography.titleLarge,
            singleLine = true,
            isError = state.fieldErrors.containsKey("title"),
            supportingText = state.fieldErrors["title"]?.let { { Text(it) } },
        )

        OutlinedTextField(
            value = state.draft.content,
            onValueChange = { onIntent(NoteEditorIntent.ContentChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write something…") },
            textStyle = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            minLines = 8,
            isError = state.fieldErrors.containsKey("content"),
            supportingText = state.fieldErrors["content"]?.let { { Text(it) } },
        )

        // A save failure is reported here rather than replacing the screen, so the text the
        // user typed stays in front of them.
        state.error?.let { error ->
            if (state.fieldErrors.isEmpty()) {
                Text(
                    text = error.displayMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.createdAt != null && state.updatedAt != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Created ${state.createdAt.formatForDisplay()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "Updated ${state.updatedAt.formatForDisplay()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
