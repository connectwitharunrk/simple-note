package com.arunrk.note_backend.infrastructure.persistence

import com.arunrk.note_backend.domain.model.NoteLimits
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * The persistence shape of a note.
 *
 * Deliberately separate from the domain `Note`: JPA needs a mutable class with a no-arg
 * constructor and a nullable id before insert, none of which belong in a domain model that is
 * meant to be immutable and always complete. Keeping them apart also means a column rename
 * does not ripple into business logic.
 */
@Entity
@Table(name = "notes")
class NoteEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "title", nullable = false, length = NoteLimits.MAX_TITLE_LENGTH)
    var title: String = "",

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = "",

    @Column(name = "is_pinned", nullable = false)
    var pinned: Boolean = false,

    @Column(name = "is_archived", nullable = false)
    var archived: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)
