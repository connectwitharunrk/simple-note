package com.arunrk.simplenote.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * JSON settings shared by the client and its tests.
 *
 * `ignoreUnknownKeys` is the important one: the backend can add a field to its responses
 * without every older client instantly failing to parse them.
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = false
    encodeDefaults = true
}

/**
 * The client configuration, kept separate from client creation so tests can apply exactly the
 * same setup to Ktor's `MockEngine`. If timeouts or serialization were configured inside the
 * platform factories instead, the tests would be exercising a different client than ships.
 */
fun HttpClientConfig<*>.configureNoteClient(enableLogging: Boolean) {
    install(ContentNegotiation) {
        json(AppJson)
    }

    /**
     * Without explicit timeouts a request to an unreachable host can hang indefinitely, which
     * shows up as a spinner that never resolves rather than an error the user can act on.
     */
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }

    if (enableLogging) {
        install(Logging) { level = LogLevel.INFO }
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    // Non-2xx responses are handled explicitly and turned into AppError, so Ktor must not
    // raise them as exceptions first.
    expectSuccess = false
}

fun createNoteHttpClient(enableLogging: Boolean = false): HttpClient =
    createPlatformHttpClient { configureNoteClient(enableLogging) }

/**
 * Each platform supplies its own Ktor engine: OkHttp on Android and desktop, Darwin
 * (NSURLSession) on iOS. Everything above this line is shared.
 */
expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
