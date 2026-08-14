package com.arunrk.note_backend.infrastructure.persistence

import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.model.NewNote
import com.arunrk.note_backend.domain.repository.NoteRepository
import com.arunrk.note_backend.testsupport.TEST_NOW
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the adapter that satisfies the domain port, not Spring Data itself.
 *
 * Testing through [NoteRepository] means these tests assert the contract the use cases rely
 * on — including the ordering promise and LIKE escaping — rather than the shape of a
 * generated query.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NoteRepositoryAdapter::class)
class NoteRepositoryAdapterTest {

    @Autowired
    private lateinit var repository: NoteRepositoryAdapter

    @Test
    fun `create assigns an id and reads back identically`() {
        val created = repository.create(newNote(title = "Groceries", content = "Milk"))

        assertTrue(created.id > 0)
        assertEquals(created, repository.findById(created.id))
    }

    @Test
    fun `timestamps survive the round trip`() {
        val createdAt = TEST_NOW
        val updatedAt = TEST_NOW.plus(Duration.ofMinutes(90))

        val created = repository.create(newNote(createdAt = createdAt, updatedAt = updatedAt))
        val reloaded = repository.findById(created.id)!!

        assertEquals(createdAt, reloaded.createdAt)
        assertEquals(updatedAt, reloaded.updatedAt)
    }

    @Test
    fun `findById returns null for an unknown id`() {
        assertNull(repository.findById(404))
    }

    @Test
    fun `update persists changes but never moves createdAt`() {
        val created = repository.create(newNote(title = "Old", content = "Old body"))

        val updated = repository.update(
            created.copy(
                title = "New",
                content = "New body",
                pinned = true,
                updatedAt = TEST_NOW.plus(Duration.ofHours(1)),
            ),
        )

        assertEquals("New", updated.title)
        assertEquals("New body", updated.content)
        assertTrue(updated.pinned)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(updated, repository.findById(created.id))
    }

    @Test
    fun `update fails for a note that is not there`() {
        val detached = repository.create(newNote()).copy(id = 999)

        assertFailsWith<NoteNotFoundException> { repository.update(detached) }
    }

    @Test
    fun `findAll orders pinned first then most recently updated`() {
        val oldest = repository.create(newNote(title = "oldest", updatedAt = TEST_NOW))
        val newest = repository.create(
            newNote(title = "newest", updatedAt = TEST_NOW.plus(Duration.ofHours(2))),
        )
        val pinned = repository.create(newNote(title = "pinned", pinned = true, updatedAt = TEST_NOW))

        assertEquals(
            listOf(pinned.id, newest.id, oldest.id),
            repository.findAll(archived = false).map { it.id },
        )
    }

    @Test
    fun `findAll separates active from archived notes`() {
        val active = repository.create(newNote(title = "active"))
        val archived = repository.create(newNote(title = "archived", archived = true))

        assertEquals(listOf(active.id), repository.findAll(archived = false).map { it.id })
        assertEquals(listOf(archived.id), repository.findAll(archived = true).map { it.id })
    }

    @Test
    fun `search matches title and content case-insensitively`() {
        val byTitle = repository.create(newNote(title = "Groceries", content = "nothing here"))
        val byContent = repository.create(newNote(title = "Shopping", content = "buy GROCERIES"))
        repository.create(newNote(title = "Unrelated", content = "nothing here"))

        val matches = repository.search("grocer", archived = false).map { it.id }

        assertEquals(setOf(byTitle.id, byContent.id), matches.toSet())
    }

    @Test
    fun `search ignores archived notes unless asked for them`() {
        repository.create(newNote(title = "Groceries", content = "x"))
        val archived = repository.create(newNote(title = "Groceries", content = "x", archived = true))

        assertEquals(1, repository.search("groceries", archived = false).size)
        assertEquals(listOf(archived.id), repository.search("groceries", archived = true).map { it.id })
    }

    @Test
    fun `search treats a percent sign as a literal, not a wildcard`() {
        val withPercent = repository.create(newNote(title = "Battery at 50%", content = "x"))
        repository.create(newNote(title = "Unrelated", content = "y"))

        assertEquals(listOf(withPercent.id), repository.search("50%", archived = false).map { it.id })
        // A bare "%" must match only the note that literally contains one, not every row.
        assertEquals(listOf(withPercent.id), repository.search("%", archived = false).map { it.id })
    }

    @Test
    fun `search treats an underscore as a literal, not a single-character wildcard`() {
        val withUnderscore = repository.create(newNote(title = "snake_case", content = "x"))
        repository.create(newNote(title = "snakeXcase", content = "y"))

        assertEquals(
            listOf(withUnderscore.id),
            repository.search("snake_case", archived = false).map { it.id },
        )
    }

    @Test
    fun `search treats the escape character itself as a literal`() {
        val withBang = repository.create(newNote(title = "Important!", content = "x"))
        repository.create(newNote(title = "Ordinary", content = "y"))

        assertEquals(listOf(withBang.id), repository.search("Important!", archived = false).map { it.id })
    }

    @Test
    fun `search returns nothing when there is no match`() {
        repository.create(newNote(title = "Groceries", content = "Milk"))

        assertTrue(repository.search("zzz", archived = false).isEmpty())
    }

    @Test
    fun `deleteById reports whether anything was removed`() {
        val created = repository.create(newNote())

        assertTrue(repository.deleteById(created.id))
        assertNull(repository.findById(created.id))
        assertTrue(!repository.deleteById(created.id))
    }

    @Test
    fun `unicode content survives the round trip`() {
        val created = repository.create(newNote(title = "Café ☕", content = "naïve — 日本語"))

        val reloaded = repository.findById(created.id)!!

        assertEquals("Café ☕", reloaded.title)
        assertEquals("naïve — 日本語", reloaded.content)
    }

    private fun newNote(
        title: String = "Title",
        content: String = "Content",
        pinned: Boolean = false,
        archived: Boolean = false,
        createdAt: Instant = TEST_NOW,
        updatedAt: Instant = createdAt,
    ) = NewNote(
        title = title,
        content = content,
        pinned = pinned,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
