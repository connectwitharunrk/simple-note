package com.arunrk.simplenote.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

/**
 * 10.0.2.2 is the Android emulator's alias for the host machine's loopback interface.
 * `localhost` from inside the emulator would mean the emulated device itself.
 *
 * On a physical device, override this with the host's LAN address when creating the Koin
 * module — see the README.
 */
actual fun defaultBaseUrl(): String = "http://192.168.0.126:8080"

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) { block() }
