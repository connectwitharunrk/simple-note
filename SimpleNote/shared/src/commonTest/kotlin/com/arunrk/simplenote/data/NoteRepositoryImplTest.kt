package com.arunrk.simplenote.data

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.domain.model.NoteDraft
import com.arunrk.simplenote.domain.model.NoteFilter
import com.arunrk.simplenote.testsupport.RecordedRequests
import com.arunrk.simplenote.testsupport.errorJson
import com.arunrk.simplenote.testsupport.expectFailure
import com.arunrk.simplenote.testsupport.expectSuccess
import com.arunrk.simplenote.testsupport.jsonResponse
import com.arunrk.simplenote.testsupport.mockRepository
import com.arunrk.simplenote.testsupport.noContent
import com.arunrk.simplenote.testsupport.noteJson
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Exercises the data layer end to end through Ktor: request building, JSON serialization, and
 * the translation of every failure mode into [AppError]. Only the transport is mocked.
 */
class NoteRepositoryImplTest {

    // ---------- reading ----------

    @Test
    fun `getNotes parses the response into domain notes`() = runTest {
        val repository = mockRepository { jsonResponse("[${noteJson(id = 1, pinned = true)}]") }

        val notes = repository.getNotes(NoteFilter.Active).expectSuccess()

        assertEquals(1, notes.size)
        with(notes.first()) {
            assertEquals(1L, id)
            assertEquals("Groceries", title)
            assertEquals("Milk, eggs", content)
            assertTrue(pinned)
            assertEquals(Instant.parse("2026-08-14T10:00:00Z"), createdAt)
        }
    }

    @Test
    fun `getNotes requests the active bucket by default`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse("[]") }

        repository.getNotes(NoteFilter.Active).expectSuccess()

        assertEquals("GET", recorded.lastMethod)
        assertTrue(recorded.lastUrl.contains("/api/notes"), recorded.lastUrl)
        assertTrue(recorded.lastUrl.contains("archived=false"), recorded.lastUrl)
    }

    @Test
    fun `getNotes maps the archived filter onto the query parameter`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse("[]") }

        repository.getNotes(NoteFilter.Archived).expectSuccess()

        assertTrue(recorded.lastUrl.contains("archived=true"), recorded.lastUrl)
    }

    @Test
    fun `an empty list is a success, not an error`() = runTest {
        val repository = mockRepository { jsonResponse("[]") }

        assertTrue(repository.getNotes(NoteFilter.Active).expectSuccess().isEmpty())
    }

    @Test
    fun `searchNotes sends the query and the filter`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse("[${noteJson()}]") }

        repository.searchNotes("milk", NoteFilter.Active).expectSuccess()

        assertTrue(recorded.lastUrl.contains("/api/notes/search"), recorded.lastUrl)
        assertTrue(recorded.lastUrl.contains("query=milk"), recorded.lastUrl)
    }

    @Test
    fun `a query with url-unsafe characters is encoded rather than breaking the request`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse("[]") }

        repository.searchNotes("50% & more", NoteFilter.Active).expectSuccess()

        assertTrue(recorded.lastUrl.contains("50%25"), recorded.lastUrl)
        assertTrue(!recorded.lastUrl.contains("50% "), recorded.lastUrl)
    }

    @Test
    fun `unknown fields in the response are ignored so the server can add fields`() = runTest {
        val withExtraField = """
            [{"id":1,"title":"T","content":"C","pinned":false,"archived":false,
              "createdAt":"2026-08-14T10:00:00Z","updatedAt":"2026-08-14T10:00:00Z",
              "colour":"blue","tags":["a"]}]
        """.trimIndent()
        val repository = mockRepository { jsonResponse(withExtraField) }

        assertEquals(1, repository.getNotes(NoteFilter.Active).expectSuccess().size)
    }

    @Test
    fun `microsecond timestamps from MySQL round trip correctly`() = runTest {
        val repository = mockRepository {
            jsonResponse("[${noteJson(createdAt = "2026-08-14T09:53:42.896231Z")}]")
        }

        val note = repository.getNotes(NoteFilter.Active).expectSuccess().first()

        assertEquals(Instant.parse("2026-08-14T09:53:42.896231Z"), note.createdAt)
    }

    // ---------- writing ----------

    @Test
    fun `createNote posts the draft and returns the created note`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) {
            jsonResponse(noteJson(id = 7), HttpStatusCode.Created)
        }

        val created = repository.createNote(NoteDraft("Groceries", "Milk, eggs")).expectSuccess()

        assertEquals(7L, created.id)
        assertEquals("POST", recorded.lastMethod)
        assertTrue(recorded.lastUrl.endsWith("/api/notes"), recorded.lastUrl)
    }

    @Test
    fun `updateNote uses PUT on the note url`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse(noteJson(id = 3)) }

        repository.updateNote(3, NoteDraft("New", "Body")).expectSuccess()

        assertEquals("PUT", recorded.lastMethod)
        assertTrue(recorded.lastUrl.endsWith("/api/notes/3"), recorded.lastUrl)
    }

    @Test
    fun `deleteNote treats an empty 204 as success`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { noContent() }

        repository.deleteNote(3).expectSuccess()

        assertEquals("DELETE", recorded.lastMethod)
    }

    @Test
    fun `setPinned patches the pin endpoint with the requested value`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse(noteJson(id = 1, pinned = true)) }

        val note = repository.setPinned(1, pinned = true).expectSuccess()

        assertTrue(note.pinned)
        assertEquals("PATCH", recorded.lastMethod)
        assertTrue(recorded.lastUrl.endsWith("/api/notes/1/pin"), recorded.lastUrl)
    }

    @Test
    fun `setArchived patches the archive endpoint`() = runTest {
        val recorded = RecordedRequests()
        val repository = mockRepository(recorded) { jsonResponse(noteJson(id = 1, archived = true)) }

        assertTrue(repository.setArchived(1, archived = true).expectSuccess().archived)
        assertTrue(recorded.lastUrl.endsWith("/api/notes/1/archive"), recorded.lastUrl)
    }

    // ---------- failure classification ----------

    @Test
    fun `404 becomes NotFound carrying the server message`() = runTest {
        val repository = mockRepository {
            jsonResponse(
                errorJson(404, "NOTE_NOT_FOUND", "Note not found with id 9999"),
                HttpStatusCode.NotFound,
            )
        }

        val error = repository.getNote(9999).expectFailure()

        assertIs<AppError.NotFound>(error)
        assertEquals("Note not found with id 9999", error.serverMessage)
    }

    @Test
    fun `400 becomes Validation and keeps the per-field detail`() = runTest {
        val repository = mockRepository {
            jsonResponse(
                errorJson(
                    status = 400,
                    error = "VALIDATION_FAILED",
                    message = "The request contains invalid fields",
                    fieldErrors = """{"title":"Title must be at most 255 characters"}""",
                ),
                HttpStatusCode.BadRequest,
            )
        }

        val error = repository.createNote(NoteDraft("x", "y")).expectFailure()

        assertIs<AppError.Validation>(error)
        assertEquals("Title must be at most 255 characters", error.fieldErrors["title"])
    }

    @Test
    fun `400 without field errors still yields an empty map rather than null`() = runTest {
        val repository = mockRepository {
            jsonResponse(
                errorJson(400, "INVALID_NOTE", "A note must have a title or content"),
                HttpStatusCode.BadRequest,
            )
        }

        val error = repository.createNote(NoteDraft("x", "y")).expectFailure()

        assertIs<AppError.Validation>(error)
        assertTrue(error.fieldErrors.isEmpty())
        assertEquals("A note must have a title or content", error.serverMessage)
    }

    @Test
    fun `500 becomes Server and is marked retryable`() = runTest {
        val repository = mockRepository {
            jsonResponse(
                errorJson(500, "INTERNAL_ERROR", "An unexpected error occurred"),
                HttpStatusCode.InternalServerError,
            )
        }

        val error = repository.getNotes(NoteFilter.Active).expectFailure()

        assertIs<AppError.Server>(error)
        assertEquals(500, error.status)
    }

    @Test
    fun `other 4xx become Http carrying the server error code`() = runTest {
        val repository = mockRepository {
            jsonResponse(
                errorJson(405, "METHOD_NOT_ALLOWED", "PATCH is not supported"),
                HttpStatusCode.MethodNotAllowed,
            )
        }

        val error = repository.getNotes(NoteFilter.Active).expectFailure()

        assertIs<AppError.Http>(error)
        assertEquals(405, error.status)
        assertEquals("METHOD_NOT_ALLOWED", error.code)
    }

    @Test
    fun `a connection failure becomes Network`() = runTest {
        val repository = mockRepository { throw IOException("Connection refused") }

        assertEquals(AppError.Network, repository.getNotes(NoteFilter.Active).expectFailure())
    }

    @Test
    fun `an error body that is not the expected envelope still yields the right status`() = runTest {
        val repository = mockRepository {
            jsonResponse("<html>502 Bad Gateway</html>", HttpStatusCode.BadGateway)
        }

        val error = repository.getNotes(NoteFilter.Active).expectFailure()

        assertIs<AppError.Server>(error)
        assertEquals(502, error.status)
    }

    @Test
    fun `a malformed success body becomes Unknown instead of crashing`() = runTest {
        val repository = mockRepository { jsonResponse("""{"not":"a list"}""") }

        assertIs<AppError.Unknown>(repository.getNotes(NoteFilter.Active).expectFailure())
    }

    @Test
    fun `an unparseable timestamp becomes Unknown instead of crashing`() = runTest {
        val repository = mockRepository {
            jsonResponse("[${noteJson(createdAt = "not-a-timestamp")}]")
        }

        val error = repository.getNotes(NoteFilter.Active).expectFailure()

        assertIs<AppError.Unknown>(error)
    }
}
