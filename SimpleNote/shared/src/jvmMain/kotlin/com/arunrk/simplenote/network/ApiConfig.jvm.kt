package com.arunrk.simplenote.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

/** The desktop app runs on the same machine as the backend during development. */
actual fun defaultBaseUrl(): String = "http://localhost:8080"

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) { block() }
