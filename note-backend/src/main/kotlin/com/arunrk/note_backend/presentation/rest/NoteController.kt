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
import com.arunrk.note_backend.presentation.rest.dto.ArchiveRequest
import com.arunrk.note_backend.presentation.rest.dto.CreateNoteRequest
import com.arunrk.note_backend.presentation.rest.dto.NoteResponse
import com.arunrk.note_backend.presentation.rest.dto.PinRequest
import com.arunrk.note_backend.presentation.rest.dto.UpdateNoteRequest
import com.arunrk.note_backend.presentation.rest.mapper.toResponse
import com.arunrk.note_backend.presentation.rest.mapper.toResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * HTTP entry point for notes.
 *
 * Every method does the same three things and nothing else: turn the request into a command,
 * call one use case, map the result to a DTO. There is no branching, no validation logic and
 * no error handling here — invalid input is rejected by Bean Validation or by the domain, and
 * failures are turned into responses by [error.GlobalExceptionHandler]. That is what keeps
 * the business rules testable without a servlet.
 *
 * The eight injected use cases are deliberate: each endpoint depends on exactly the one
 * operation it performs, so adding a use case never widens what the others can reach.
 */
@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val listNotes: ListNotesUseCase,
    private val searchNotes: SearchNotesUseCase,
    private val getNote: GetNoteUseCase,
    private val createNote: CreateNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setNotePinned: SetNotePinnedUseCase,
    private val setNoteArchived: SetNoteArchivedUseCase,
) {

    /** `GET /api/notes?archived=false` — active notes by default, pinned first. */
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "false") archived: Boolean,
    ): List<NoteResponse> = listNotes(archived).toResponses()

    /**
     * `GET /api/notes/search?query=milk&archived=false`
     *
     * Mapped before `/{id}` is considered: Spring prefers a literal path segment over a
     * template variable, so this never arrives as a note with the id "search".
     */
    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "false") archived: Boolean,
    ): List<NoteResponse> = searchNotes(query, archived).toResponses()

    /** `GET /api/notes/{id}` */
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): NoteResponse = getNote(id).toResponse()

    /** `POST /api/notes` — 201 with a `Location` header pointing at the new note. */
    @PostMapping
    fun create(@Valid @RequestBody request: CreateNoteRequest): ResponseEntity<NoteResponse> {
        val created = createNote(CreateNoteCommand(title = request.title, content = request.content))
        return ResponseEntity
            .created(URI.create("/api/notes/${created.id}"))
            .body(created.toResponse())
    }

    /** `PUT /api/notes/{id}` — replaces title and content; leaves pin and archive alone. */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateNoteRequest,
    ): NoteResponse = updateNote(
        UpdateNoteCommand(id = id, title = request.title, content = request.content),
    ).toResponse()

    /** `DELETE /api/notes/{id}` — 204 with no body. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteNote(id)
    }

    /** `PATCH /api/notes/{id}/pin` with `{"pinned": true}` */
    @PatchMapping("/{id}/pin")
    fun setPinned(
        @PathVariable id: Long,
        @RequestBody request: PinRequest,
    ): NoteResponse = setNotePinned(id, request.pinned).toResponse()

    /** `PATCH /api/notes/{id}/archive` with `{"archived": true}` */
    @PatchMapping("/{id}/archive")
    fun setArchived(
        @PathVariable id: Long,
        @RequestBody request: ArchiveRequest,
    ): NoteResponse = setNoteArchived(id, request.archived).toResponse()
}
