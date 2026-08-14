package com.arunrk.note_backend.presentation.rest

import com.arunrk.note_backend.application.command.CreateNoteCommand
import com.arunrk.note_backend.application.command.UpdateNoteCommand
import com.arunrk.note_backend.application.usecase.CreateNoteUseCase
import com.arunrk.note_backend.application.usecase.DeleteNoteUseCase
import com.arunrk.note_backend.application.usecase.GetNoteUseCase
import com.arunrk.note_backend.application.usecase.ListNotesUseCase
import com.arunrk.note_backend.application.usecase.SearchNotesUseCase
import com.arunrk.note_backend.application.usecase.SetNoteArchivedUseCase
import com.arunrk.note_backend.application.usecase.SetNotePinnedUseCase
import com.arunrk.note_backend.application.usecase.UpdateNoteUseCase
import com.arunrk.note_backend.domain.exception.InvalidNoteException
import com.arunrk.note_backend.domain.exception.InvalidSearchQueryException
import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.domain.model.NoteLimits
import com.arunrk.note_backend.presentation.rest.error.GlobalExceptionHandler
import com.arunrk.note_backend.testsupport.TEST_NOW
import com.arunrk.note_backend.testsupport.note
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import org.mockito.kotlin.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test

/**
 * Verifies the HTTP contract: status codes, response shape, and that each endpoint delegates
 * to exactly one use case with the right arguments.
 *
 * The use cases are mocked because their behaviour is already covered by their own unit
 * tests — what is under test here is the translation between HTTP and the application layer.
 */
@WebMvcTest(NoteController::class)
@Import(GlobalExceptionHandler::class, NoteControllerTest.FixedClockConfig::class)
class NoteControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockitoBean private lateinit var listNotes: ListNotesUseCase
    @MockitoBean private lateinit var searchNotes: SearchNotesUseCase
    @MockitoBean private lateinit var getNote: GetNoteUseCase
    @MockitoBean private lateinit var createNote: CreateNoteUseCase
    @MockitoBean private lateinit var updateNote: UpdateNoteUseCase
    @MockitoBean private lateinit var deleteNote: DeleteNoteUseCase
    @MockitoBean private lateinit var setNotePinned: SetNotePinnedUseCase
    @MockitoBean private lateinit var setNoteArchived: SetNoteArchivedUseCase

    @org.springframework.boot.test.context.TestConfiguration
    class FixedClockConfig {
        @org.springframework.context.annotation.Bean
        fun clock(): Clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)
    }

    // ---------- GET /api/notes ----------

    @Test
    fun `list returns notes and serialises timestamps as ISO-8601 UTC`() {
        given(listNotes(false)).willReturn(listOf(note(id = 1, title = "Groceries", content = "Milk")))

        mockMvc.perform(get("/api/notes"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Groceries"))
            .andExpect(jsonPath("$[0].content").value("Milk"))
            .andExpect(jsonPath("$[0].pinned").value(false))
            .andExpect(jsonPath("$[0].archived").value(false))
            .andExpect(jsonPath("$[0].createdAt").value("2026-08-14T10:00:00Z"))
            .andExpect(jsonPath("$[0].updatedAt").value("2026-08-14T10:00:00Z"))
    }

    @Test
    fun `list defaults to active notes`() {
        given(listNotes(false)).willReturn(emptyList())

        mockMvc.perform(get("/api/notes")).andExpect(status().isOk)

        then(listNotes).should().invoke(false)
    }

    @Test
    fun `list passes the archived flag through`() {
        given(listNotes(true)).willReturn(emptyList())

        mockMvc.perform(get("/api/notes").param("archived", "true")).andExpect(status().isOk)

        then(listNotes).should().invoke(true)
    }

    @Test
    fun `an empty list is an empty array, not an error`() {
        given(listNotes(false)).willReturn(emptyList())

        mockMvc.perform(get("/api/notes"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))
    }

    @Test
    fun `a bad archived value is rejected as a bad request`() {
        mockMvc.perform(get("/api/notes").param("archived", "maybe"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
    }

    // ---------- GET /api/notes/search ----------

    @Test
    fun `search is routed as a literal path, not as a note id`() {
        given(searchNotes("milk", false)).willReturn(listOf(note(id = 2)))

        mockMvc.perform(get("/api/notes/search").param("query", "milk"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(2))

        then(searchNotes).should().invoke("milk", false)
        then(getNote).should(times(0)).invoke(any())
    }

    @Test
    fun `search without a query is a bad request`() {
        mockMvc.perform(get("/api/notes/search"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
    }

    @Test
    fun `a blank search query is reported as the domain rejects it`() {
        given(searchNotes(any(), any())).willThrow(
            InvalidSearchQueryException("Search query must not be blank"),
        )

        mockMvc.perform(get("/api/notes/search").param("query", "  "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_SEARCH_QUERY"))
            .andExpect(jsonPath("$.message").value("Search query must not be blank"))
    }

    // ---------- GET /api/notes/{id} ----------

    @Test
    fun `get returns a single note`() {
        given(getNote(1)).willReturn(note(id = 1, title = "Groceries"))

        mockMvc.perform(get("/api/notes/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Groceries"))
    }

    @Test
    fun `get returns 404 with the full error envelope`() {
        given(getNote(404)).willThrow(NoteNotFoundException(404))

        mockMvc.perform(get("/api/notes/404"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Note not found with id 404"))
            .andExpect(jsonPath("$.path").value("/api/notes/404"))
            .andExpect(jsonPath("$.timestamp").value("2026-08-14T10:00:00Z"))
            .andExpect(jsonPath("$.fieldErrors").doesNotExist())
    }

    @Test
    fun `a non-numeric id is a bad request, not a 404`() {
        mockMvc.perform(get("/api/notes/abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
    }

    // ---------- POST /api/notes ----------

    @Test
    fun `create returns 201 with a Location header`() {
        given(createNote(CreateNoteCommand("Groceries", "Milk")))
            .willReturn(note(id = 7, title = "Groceries", content = "Milk"))

        mockMvc.perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Groceries","content":"Milk"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/notes/7"))
            .andExpect(jsonPath("$.id").value(7))
    }

    @Test
    fun `create accepts a note with only content`() {
        given(createNote(CreateNoteCommand("", "Milk"))).willReturn(note(id = 8, title = ""))

        mockMvc.perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Milk"}"""),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `create rejects an over-long title with a field error`() {
        val tooLong = "a".repeat(NoteLimits.MAX_TITLE_LENGTH + 1)

        mockMvc.perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"$tooLong","content":"x"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.title").value("Title must be at most 255 characters"))

        then(createNote).should(times(0)).invoke(any())
    }

    @Test
    fun `create surfaces the domain rule when the note says nothing`() {
        given(createNote(any())).willThrow(InvalidNoteException("A note must have a title or content"))

        mockMvc.perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"   ","content":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_NOTE"))
            .andExpect(jsonPath("$.message").value("A note must have a title or content"))
    }

    @Test
    fun `create rejects malformed JSON`() {
        mockMvc.perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": """),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"))
    }

    // ---------- PUT /api/notes/{id} ----------

    @Test
    fun `update returns the updated note`() {
        given(updateNote(UpdateNoteCommand(1, "New", "New body")))
            .willReturn(note(id = 1, title = "New", content = "New body"))

        mockMvc.perform(
            put("/api/notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"New","content":"New body"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New"))
    }

    @Test
    fun `update returns 404 for an unknown note`() {
        given(updateNote(any())).willThrow(NoteNotFoundException(404))

        mockMvc.perform(
            put("/api/notes/404")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"New","content":"New body"}"""),
        ).andExpect(status().isNotFound)
    }

    // ---------- DELETE /api/notes/{id} ----------

    @Test
    fun `delete returns 204 with no body`() {
        mockMvc.perform(delete("/api/notes/1"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        then(deleteNote).should().invoke(1)
    }

    @Test
    fun `delete returns 404 for an unknown note`() {
        given(deleteNote(404)).willThrow(NoteNotFoundException(404))

        mockMvc.perform(delete("/api/notes/404"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"))
    }

    // ---------- PATCH pin / archive ----------

    @Test
    fun `pin sets the requested value rather than toggling`() {
        given(setNotePinned(1, true)).willReturn(note(id = 1, pinned = true))

        mockMvc.perform(
            patch("/api/notes/1/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"pinned":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pinned").value(true))

        then(setNotePinned).should().invoke(1, true)
    }

    @Test
    fun `unpin passes false through`() {
        given(setNotePinned(1, false)).willReturn(note(id = 1, pinned = false))

        mockMvc.perform(
            patch("/api/notes/1/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"pinned":false}"""),
        ).andExpect(status().isOk)

        then(setNotePinned).should().invoke(1, false)
    }

    @Test
    fun `pin without a body field is a bad request, not a silent unpin`() {
        mockMvc.perform(
            patch("/api/notes/1/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"))

        then(setNotePinned).should(times(0)).invoke(any(), any())
    }

    @Test
    fun `archive sets the requested value`() {
        given(setNoteArchived(1, true)).willReturn(note(id = 1, archived = true))

        mockMvc.perform(
            patch("/api/notes/1/archive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"archived":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.archived").value(true))

        then(setNoteArchived).should().invoke(1, true)
    }

    @Test
    fun `archive returns 404 for an unknown note`() {
        given(setNoteArchived(eq(404), any())).willThrow(NoteNotFoundException(404))

        mockMvc.perform(
            patch("/api/notes/404/archive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"archived":true}"""),
        ).andExpect(status().isNotFound)
    }

    // ---------- cross-cutting ----------

    @Test
    fun `an unknown path returns 404 in the same envelope, not 500`() {
        mockMvc.perform(get("/api/nope"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("ENDPOINT_NOT_FOUND"))
    }

    @Test
    fun `fieldErrors is omitted entirely when the failure is not per-field`() {
        given(getNote(404)).willThrow(NoteNotFoundException(404))

        mockMvc.perform(get("/api/notes/404"))
            .andExpect(status().isNotFound)
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("fieldErrors"))))
    }

    @Test
    fun `an unsupported method returns 405 in the same envelope`() {
        mockMvc.perform(patch("/api/notes"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
    }

    @Test
    fun `an unexpected failure is reported as 500 without leaking internals`() {
        given(listNotes(false)).willThrow(IllegalStateException("connection pool exhausted at 10.0.0.5"))

        mockMvc.perform(get("/api/notes"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("10.0.0.5"))))
    }
}
