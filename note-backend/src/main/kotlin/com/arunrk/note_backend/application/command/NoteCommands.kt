package com.arunrk.note_backend.application.command

/**
 * Inputs to the write use cases.
 *
 * Commands exist so the application layer has its own vocabulary and does not depend on the
 * REST DTOs. A second entry point (a CLI, a sync job) would build the same commands.
 */
data class CreateNoteCommand(
    val title: String,
    val content: String,
)

data class UpdateNoteCommand(
    val id: Long,
    val title: String,
    val content: String,
)
