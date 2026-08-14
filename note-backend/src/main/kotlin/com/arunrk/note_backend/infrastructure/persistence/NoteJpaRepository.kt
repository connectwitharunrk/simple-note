package com.arunrk.note_backend.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Spring Data access to the `notes` table.
 *
 * This is an infrastructure detail, not the port the domain talks to — use cases depend on
 * `NoteRepository`, and [NoteRepositoryAdapter] bridges the two.
 */
interface NoteJpaRepository : JpaRepository<NoteEntity, Long> {

    fun findByArchivedOrderByPinnedDescUpdatedAtDesc(archived: Boolean): List<NoteEntity>

    /**
     * Case-insensitive substring search over title and content.
     *
     * `ESCAPE '!'` means a query containing `%` or `_` is matched literally instead of acting
     * as a wildcard — the caller escapes those with `!` first. `!` is used rather than the
     * usual backslash because a backslash is itself an escape character inside MySQL string
     * literals, which makes the pattern depend on `NO_BACKSLASH_ESCAPES`.
     */
    @Query(
        """
        SELECT n FROM NoteEntity n
        WHERE n.archived = :archived
          AND (
                LOWER(n.title)   LIKE LOWER(:pattern) ESCAPE '!'
             OR LOWER(n.content) LIKE LOWER(:pattern) ESCAPE '!'
          )
        ORDER BY n.pinned DESC, n.updatedAt DESC
        """,
    )
    fun search(
        @Param("pattern") pattern: String,
        @Param("archived") archived: Boolean,
    ): List<NoteEntity>
}
