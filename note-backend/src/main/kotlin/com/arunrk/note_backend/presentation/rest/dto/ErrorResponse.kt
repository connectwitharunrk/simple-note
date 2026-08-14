package com.arunrk.note_backend.presentation.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * The single error shape for every non-2xx response.
 *
 * One envelope for all failures means the client needs exactly one error parser. [error] is a
 * stable machine-readable code — clients should branch on it rather than on [message], which
 * is human-facing and may be reworded.
 *
 * [fieldErrors] is present only for validation failures and is omitted from the JSON
 * otherwise, so a client can treat its presence as "this was a per-field problem".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant,
    val fieldErrors: Map<String, String>? = null,
)
