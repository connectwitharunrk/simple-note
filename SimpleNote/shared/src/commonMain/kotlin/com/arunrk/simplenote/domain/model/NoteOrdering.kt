package com.arunrk.simplenote.domain.model

/**
 * The order notes are shown in: pinned first, then most recently updated.
 *
 * The backend already returns lists in this order. This exists because the client also
 * reorders locally — when a pin is toggled optimistically, the note has to move immediately
 * rather than after a refresh — and both places must agree on what the order is.
 */
val NoteListOrder: Comparator<Note> =
    compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt }

fun List<Note>.inListOrder(): List<Note> = sortedWith(NoteListOrder)
