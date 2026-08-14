package com.arunrk.simplenote.testsupport

import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.domain.model.Note
import kotlin.test.assertTrue
import kotlin.time.Instant

val TEST_NOW: Instant = Instant.parse("2026-08-14T10:00:00Z")

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

/** Unwraps a success, failing the test with the actual error if it was a failure. */
fun <T> AppResult<T>.expectSuccess(): T {
    assertTrue(this is AppResult.Success, "Expected success but was $this")
    return data
}

/** Unwraps a failure, failing the test with the actual value if it succeeded. */
fun <T> AppResult<T>.expectFailure(): com.arunrk.simplenote.core.error.AppError {
    assertTrue(this is AppResult.Failure, "Expected failure but was $this")
    return error
}
