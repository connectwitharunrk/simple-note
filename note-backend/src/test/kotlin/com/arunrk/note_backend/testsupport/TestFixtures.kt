package com.arunrk.note_backend.testsupport

import com.arunrk.note_backend.domain.model.Note
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A fixed point in time so timestamp assertions can be exact. */
val TEST_NOW: Instant = Instant.parse("2026-08-14T10:00:00Z")

/**
 * A [Clock] the test drives by hand.
 *
 * Lets a test assert that `updatedAt` actually moved after a mutation, which a fixed clock
 * cannot distinguish from "the field was never touched".
 */
class MutableClock(private var now: Instant = TEST_NOW) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }
}

fun note(
    id: Long,
    title: String = "Title $id",
    content: String = "Content $id",
    pinned: Boolean = false,
    archived: Boolean = false,
    createdAt: Instant = TEST_NOW,
    updatedAt: Instant = createdAt,
): Note = Note(
    id = id,
    title = title,
    content = content,
    pinned = pinned,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
