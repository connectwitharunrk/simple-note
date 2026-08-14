package com.arunrk.note_backend.infrastructure.persistence

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.model.Note
import com.arunrk.note_backend.domain.repository.NoteRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Implements the domain's persistence port on top of JPA.
 *
 * This class is the only place that knows notes live in MySQL. Transactions are declared here
 * rather than in the use cases so the application layer stays free of Spring annotations;
 * every operation in this version is a single unit of work, so nothing needs a wider boundary.
 */
@Repository
@Transactional(readOnly = true)
class NoteRepositoryAdapter(
    private val noteJpaRepository: NoteJpaRepository,
) : NoteRepository {

    @Transactional
    override fun create(note: NewNote): Note =
        noteJpaRepository.save(note.toEntity()).toDomain()

    @Transactional
    override fun update(note: Note): Note {
        val entity = noteJpaRepository.findById(note.id)
            .orElseThrow { NoteNotFoundException(note.id) }
        entity.applyChangesFrom(note)
        return noteJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Note? =
        noteJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findAll(archived: Boolean): List<Note> =
        noteJpaRepository.findByArchivedOrderByPinnedDescUpdatedAtDesc(archived)
            .map { it.toDomain() }

    override fun search(query: String, archived: Boolean): List<Note> =
        noteJpaRepository.search(pattern = "%${escapeLikeWildcards(query)}%", archived = archived)
            .map { it.toDomain() }

    @Transactional
    override fun deleteById(id: Long): Boolean {
        if (!noteJpaRepository.existsById(id)) return false
        noteJpaRepository.deleteById(id)
        return true
    }

    /**
     * Neutralises LIKE wildcards so a search for "50%" or "snake_case" matches literally
     * instead of matching everything. Pairs with `ESCAPE '!'` in [NoteJpaRepository.search];
     * the escape character itself is escaped first, or the replacements would corrupt it.
     */
    private fun escapeLikeWildcards(query: String): String =
        query.replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
}
