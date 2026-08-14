package com.arunrk.simplenote.network

/**
 * Where the backend lives.
 *
 * Injected rather than read from a constant so each platform can supply its own default and a
 * caller can override it — pointing a device at a laptop on the same LAN, or a build at a
 * staging server — without touching any code that makes requests.
 */
data class ApiConfig(
    val baseUrl: String,
) {
    /** Trailing slashes are trimmed so callers can always append "/api/notes" safely. */
    val normalizedBaseUrl: String = baseUrl.trimEnd('/')

    fun notesUrl(): String = "$normalizedBaseUrl/api/notes"

    fun noteUrl(id: Long): String = "${notesUrl()}/$id"

    fun searchUrl(): String = "${notesUrl()}/search"

    fun pinUrl(id: Long): String = "${noteUrl(id)}/pin"

    fun archiveUrl(id: Long): String = "${noteUrl(id)}/archive"

    companion object {
        fun default(): ApiConfig = ApiConfig(defaultBaseUrl())
    }
}

/**
 * The backend URL that works out of the box for this platform.
 *
 * Android differs because the emulator runs behind its own NAT: `localhost` there is the
 * emulated device itself, and `10.0.2.2` is the loopback address of the host machine.
 */
expect fun defaultBaseUrl(): String
