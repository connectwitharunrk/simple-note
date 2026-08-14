package com.arunrk.simplenote.data.error

import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.data.remote.dto.ErrorResponseDto
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException

/**
 * The one place in the app where a `Throwable` or an HTTP status is interpreted.
 *
 * Everything above the data layer works with [AppError], so this file is the boundary. If a
 * new failure mode needs distinct handling in the UI, it gets classified here rather than by
 * scattering `catch` blocks through the ViewModels.
 */

/** Classifies a non-2xx response, using the backend's error envelope when it is readable. */
suspend fun HttpResponse.toAppError(): AppError {
    val body = runCatching { body<ErrorResponseDto>() }.getOrNull()
    val serverMessage = body?.message
    val serverCode = body?.error

    return when {
        status == HttpStatusCode.NotFound -> AppError.NotFound(serverMessage)

        status == HttpStatusCode.BadRequest -> AppError.Validation(
            serverMessage = serverMessage,
            fieldErrors = body?.fieldErrors.orEmpty(),
        )

        status.value in 500..599 -> AppError.Server(status.value, serverMessage)

        status.value in 400..499 -> AppError.Http(status.value, serverCode, serverMessage)

        // A 1xx or 3xx reaching here means something unexpected sits between us and the API.
        else -> AppError.Unknown("Unexpected HTTP status ${status.value}")
    }
}

/**
 * Classifies a thrown failure.
 *
 * The distinction that matters to the user is "I could not reach the server" — worth a retry
 * button — versus anything else. [UnresolvedAddressException] covers a hostname that will not
 * resolve, which is what a wrong `baseUrl` or a device with no DNS looks like.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is UnresolvedAddressException -> AppError.Network
    is IOException -> AppError.Network
    else -> if (isTimeout()) AppError.Network else AppError.Unknown(message ?: this::class.simpleName)
}

/**
 * Ktor's timeout exceptions are platform-specific types that do not share a common supertype
 * across targets, so they are matched by name rather than with `is`.
 */
private fun Throwable.isTimeout(): Boolean {
    val name = this::class.simpleName.orEmpty()
    return name.contains("Timeout", ignoreCase = true) ||
        message?.contains("timeout", ignoreCase = true) == true
}
