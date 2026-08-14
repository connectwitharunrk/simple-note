package com.arunrk.simplenote.testsupport

import com.arunrk.simplenote.data.remote.NoteApi
import com.arunrk.simplenote.data.repository.NoteRepositoryImpl
import com.arunrk.simplenote.network.ApiConfig
import com.arunrk.simplenote.network.configureNoteClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel

const val TEST_BASE_URL = "http://test.local:8080"

/** Records what the client actually sent, so tests can assert on URLs, methods and bodies. */
class RecordedRequests {
    val requests = mutableListOf<HttpRequestData>()
    val last: HttpRequestData get() = requests.last()
    val lastUrl: String get() = last.url.toString()
    val lastMethod: String get() = last.method.value
}

/**
 * Builds a repository backed by Ktor's `MockEngine`.
 *
 * The client is configured with the production [configureNoteClient], so these tests exercise
 * the real serialization, timeout and content-negotiation setup — only the transport is
 * replaced. A test that configured its own client could pass while the shipped one failed.
 */
fun mockRepository(
    recorded: RecordedRequests = RecordedRequests(),
    handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): NoteRepositoryImpl {
    val engine = MockEngine { request ->
        recorded.requests += request
        handler(request)
    }
    val client = HttpClient(engine) { configureNoteClient(enableLogging = false) }
    return NoteRepositoryImpl(NoteApi(client, ApiConfig(TEST_BASE_URL)))
}

fun MockRequestHandleScope.jsonResponse(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = ByteReadChannel(body),
    status = status,
    headers = headersOf("Content-Type", "application/json"),
)

fun MockRequestHandleScope.noContent(): HttpResponseData = respond(
    content = ByteReadChannel(ByteArray(0)),
    status = HttpStatusCode.NoContent,
)

/** A note exactly as the backend serialises it, including microsecond timestamps. */
fun noteJson(
    id: Long = 1,
    title: String = "Groceries",
    content: String = "Milk, eggs",
    pinned: Boolean = false,
    archived: Boolean = false,
    createdAt: String = "2026-08-14T10:00:00Z",
    updatedAt: String = "2026-08-14T10:00:00Z",
): String = """
    {
      "id": $id,
      "title": "$title",
      "content": "$content",
      "pinned": $pinned,
      "archived": $archived,
      "createdAt": "$createdAt",
      "updatedAt": "$updatedAt"
    }
""".trimIndent()

fun errorJson(
    status: Int,
    error: String,
    message: String,
    path: String = "/api/notes",
    fieldErrors: String? = null,
): String = buildString {
    append("""{"status":$status,"error":"$error","message":"$message","path":"$path",""")
    append(""""timestamp":"2026-08-14T10:00:00Z"""")
    if (fieldErrors != null) append(""","fieldErrors":$fieldErrors""")
    append("}")
}
