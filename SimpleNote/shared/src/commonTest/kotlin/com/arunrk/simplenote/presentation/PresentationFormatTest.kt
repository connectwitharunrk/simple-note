package com.arunrk.simplenote.presentation

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.presentation.error.displayMessage
import com.arunrk.simplenote.presentation.error.displayTitle
import com.arunrk.simplenote.presentation.format.formatForDisplay
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimestampFormatTest {

    @Test
    fun `formats a UTC instant in the requested zone`() {
        val instant = Instant.parse("2026-08-14T10:04:01Z")

        assertEquals("14 Aug 2026 · 10:04", instant.formatForDisplay(TimeZone.UTC))
    }

    @Test
    fun `converts into the viewer's zone rather than showing UTC`() {
        val instant = Instant.parse("2026-08-14T22:30:00Z")

        // +05:30 pushes this past midnight, so both the date and the time must shift.
        assertEquals(
            "15 Aug 2026 · 04:00",
            instant.formatForDisplay(TimeZone.of("Asia/Kolkata")),
        )
    }

    @Test
    fun `pads single digit days hours and minutes`() {
        val instant = Instant.parse("2026-01-05T09:07:00Z")

        assertEquals("05 Jan 2026 · 09:07", instant.formatForDisplay(TimeZone.UTC))
    }

    @Test
    fun `handles the microsecond precision the backend sends`() {
        val instant = Instant.parse("2026-08-14T09:53:42.896231Z")

        assertEquals("14 Aug 2026 · 09:53", instant.formatForDisplay(TimeZone.UTC))
    }

    @Test
    fun `names every month correctly at the boundaries`() {
        assertTrue(Instant.parse("2026-01-01T00:00:00Z").formatForDisplay(TimeZone.UTC).contains("Jan"))
        assertTrue(Instant.parse("2026-12-31T23:59:00Z").formatForDisplay(TimeZone.UTC).contains("Dec"))
    }
}

class ErrorCopyTest {

    @Test
    fun `a network failure explains what the user can do`() {
        assertEquals("No connection", AppError.Network.displayTitle())
        assertTrue(AppError.Network.displayMessage().contains("connection"))
    }

    @Test
    fun `the server's own message is preferred when there is one`() {
        val error = AppError.NotFound(serverMessage = "Note not found with id 42")

        assertEquals("Note not found with id 42", error.displayMessage())
    }

    @Test
    fun `there is a fallback when the server said nothing`() {
        assertEquals("That note no longer exists.", AppError.NotFound(null).displayMessage())
    }

    @Test
    fun `a server fault never leaks internals to the user`() {
        val error = AppError.Server(status = 500, serverMessage = "NPE at com.internal.Thing:42")

        assertTrue(!error.displayMessage().contains("com.internal"))
    }

    @Test
    fun `every error kind produces non-empty copy`() {
        val errors = listOf(
            AppError.Network,
            AppError.NotFound(null),
            AppError.Validation(null),
            AppError.Http(status = 405, code = null, serverMessage = null),
            AppError.Server(status = 500, serverMessage = null),
            AppError.Unknown(null),
        )

        errors.forEach { error ->
            assertTrue(error.displayTitle().isNotBlank(), "blank title for $error")
            assertTrue(error.displayMessage().isNotBlank(), "blank message for $error")
        }
    }
}
