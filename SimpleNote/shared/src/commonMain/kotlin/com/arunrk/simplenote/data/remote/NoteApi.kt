package com.arunrk.simplenote.data.remote

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.data.error.toAppError
import com.arunrk.simplenote.data.remote.dto.ArchiveRequestDto
import com.arunrk.simplenote.data.remote.dto.NoteDraftDto
import com.arunrk.simplenote.data.remote.dto.NoteDto
import com.arunrk.simplenote.data.remote.dto.PinRequestDto
import com.arunrk.simplenote.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * The REST calls, one method per endpoint.
 *
 * Concrete rather than an interface with a Ktor implementation: the seam the app needs
 * already exists one level up at `NoteRepository`, and Ktor's `MockEngine` substitutes the
 * transport in tests. An extra interface here would add a layer without adding a seam.
 */
class NoteApi(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig,
) {

    suspend fun getNotes(archived: Boolean): AppResult<List<NoteDto>> = execute {
        method = HttpMethod.Get
        url(apiConfig.notesUrl())
        parameter("archived", archived)
    }

    suspend fun searchNotes(query: String, archived: Boolean): AppResult<List<NoteDto>> = execute {
        method = HttpMethod.Get
        url(apiConfig.searchUrl())
        parameter("query", query)
        parameter("archived", archived)
    }

    suspend fun getNote(id: Long): AppResult<NoteDto> = execute {
        method = HttpMethod.Get
        url(apiConfig.noteUrl(id))
    }

    suspend fun createNote(draft: NoteDraftDto): AppResult<NoteDto> = execute {
        method = HttpMethod.Post
        url(apiConfig.notesUrl())
        setBody(draft)
    }

    suspend fun updateNote(id: Long, draft: NoteDraftDto): AppResult<NoteDto> = execute {
        method = HttpMethod.Put
        url(apiConfig.noteUrl(id))
        setBody(draft)
    }

    suspend fun deleteNote(id: Long): AppResult<Unit> = executeWithoutResponseBody {
        method = HttpMethod.Delete
        url(apiConfig.noteUrl(id))
    }

    suspend fun setPinned(id: Long, pinned: Boolean): AppResult<NoteDto> = execute {
        method = HttpMethod.Patch
        url(apiConfig.pinUrl(id))
        setBody(PinRequestDto(pinned))
    }

    suspend fun setArchived(id: Long, archived: Boolean): AppResult<NoteDto> = execute {
        method = HttpMethod.Patch
        url(apiConfig.archiveUrl(id))
        setBody(ArchiveRequestDto(archived))
    }

    /**
     * Sends the request and decodes the response body.
     *
     * The configuration lambda is handed straight to Ktor rather than invoked inside another
     * lambda. Naming it `build` and calling `build()` inside `request { }` would silently
     * resolve to `HttpRequestBuilder.build()` — a real member with that exact name — leaving
     * every request as a default GET to localhost. Passing it as a value removes the
     * ambiguity entirely.
     *
     * The `catch` for [CancellationException] must come first and must rethrow. The search
     * box cancels its in-flight request on every keystroke; swallowing that would turn normal
     * typing into a flash of error state, and would break structured concurrency besides.
     */
    private suspend inline fun <reified T> execute(
        noinline configure: HttpRequestBuilder.() -> Unit,
    ): AppResult<T> = try {
        val response = httpClient.request(configure)
        if (response.status.isSuccess()) {
            AppResult.Success(response.body<T>())
        } else {
            AppResult.Failure(response.toAppError())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable.toAppError())
    }

    /** For 204 responses, where trying to read a body would fail because there is not one. */
    private suspend fun executeWithoutResponseBody(
        configure: HttpRequestBuilder.() -> Unit,
    ): AppResult<Unit> = try {
        val response = httpClient.request(configure)
        if (response.status.isSuccess()) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(response.toAppError())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        AppResult.Failure(throwable.toAppError())
    }
}
