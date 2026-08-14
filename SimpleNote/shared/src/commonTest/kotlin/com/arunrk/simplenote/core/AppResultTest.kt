package com.arunrk.simplenote.core

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.core.error.isRetryable
import com.arunrk.simplenote.core.result.AppResult
import com.arunrk.simplenote.core.result.errorOrNull
import com.arunrk.simplenote.core.result.getOrNull
import com.arunrk.simplenote.core.result.map
import com.arunrk.simplenote.core.result.onFailure
import com.arunrk.simplenote.core.result.onSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppResultTest {

    @Test
    fun `map transforms a success`() {
        val result: AppResult<Int> = AppResult.Success(2)

        assertEquals(4, result.map { it * 2 }.getOrNull())
    }

    @Test
    fun `map leaves a failure untouched and does not run the transform`() {
        var transformRan = false
        val result: AppResult<Int> = AppResult.Failure(AppError.Network)

        val mapped = result.map { transformRan = true; it * 2 }

        assertFalse(transformRan)
        assertEquals(AppError.Network, mapped.errorOrNull())
    }

    @Test
    fun `onSuccess and onFailure fire only for their own case`() {
        var successes = 0
        var failures = 0

        AppResult.Success(1).onSuccess { successes++ }.onFailure { failures++ }
        AppResult.Failure(AppError.Network).onSuccess { successes++ }.onFailure { failures++ }

        assertEquals(1, successes)
        assertEquals(1, failures)
    }

    @Test
    fun `getOrNull and errorOrNull return null for the other case`() {
        assertNull(AppResult.Failure(AppError.Network).getOrNull())
        assertNull(AppResult.Success(1).errorOrNull())
    }
}

class AppErrorTest {

    @Test
    fun `transient failures are worth retrying`() {
        assertTrue(AppError.Network.isRetryable)
        assertTrue(AppError.Server(status = 503, serverMessage = null).isRetryable)
        assertTrue(AppError.Unknown(cause = "boom").isRetryable)
    }

    @Test
    fun `failures that would repeat identically are not worth retrying`() {
        assertFalse(AppError.NotFound(serverMessage = null).isRetryable)
        assertFalse(AppError.Validation(serverMessage = "bad").isRetryable)
        assertFalse(AppError.Http(status = 405, code = null, serverMessage = null).isRetryable)
    }
}
