package com.arunrk.simplenote.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

/**
 * The iOS Simulator shares the host's network stack, so `localhost` reaches a backend running
 * on the same Mac. A physical iPhone does not — use the Mac's LAN address there.
 */
actual fun defaultBaseUrl(): String = "http://localhost:8080"

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) { block() }
