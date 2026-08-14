package com.arunrk.note_backend.domain.exception

/**
 * Base type for every error the domain can raise.
 *
 * Each subtype carries a stable [errorCode] so the REST layer can translate a domain failure
 * into an error response without a `when` chain that has to be kept in sync by hand.
 */
sealed class DomainException(message: String) : RuntimeException(message) {
    abstract val errorCode: String
}

class NoteNotFoundException(val noteId: Long) :
    DomainException("Note not found with id $noteId") {
    override val errorCode: String = "NOTE_NOT_FOUND"
}

class InvalidNoteException(message: String) : DomainException(message) {
    override val errorCode: String = "INVALID_NOTE"
}

class InvalidSearchQueryException(message: String) : DomainException(message) {
    override val errorCode: String = "INVALID_SEARCH_QUERY"
}
