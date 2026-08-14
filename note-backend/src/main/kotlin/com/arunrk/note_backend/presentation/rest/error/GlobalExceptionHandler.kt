package com.arunrk.note_backend.presentation.rest.error

import com.arunrk.note_backend.domain.exception.DomainException
import com.arunrk.note_backend.domain.exception.NoteNotFoundException
import com.arunrk.note_backend.presentation.rest.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Clock

/**
 * Turns every failure into the one [ErrorResponse] shape.
 *
 * Centralising this is what lets controllers contain no error handling at all, and it
 * guarantees a client never receives an unexpected body — including for failures Spring
 * raises before a controller is ever reached, such as malformed JSON or a bad path variable.
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NoteNotFoundException::class)
    fun handleNoteNotFound(
        exception: NoteNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, exception.errorCode, exception.message, request)

    /**
     * Any other domain failure is a bad request: the caller asked for something the business
     * rules do not allow. New [DomainException] subtypes are covered automatically, and carry
     * their own error code.
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainRuleViolation(
        exception: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, exception.errorCode, exception.message, request)

    /** Bean Validation failures, reported per field so the client can highlight the input. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationFailure(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors.associate { field ->
            field.field to (field.defaultMessage ?: "is invalid")
        }
        return respond(
            status = HttpStatus.BAD_REQUEST,
            errorCode = "VALIDATION_FAILED",
            message = "The request contains invalid fields",
            request = request,
            fieldErrors = fieldErrors,
        )
    }

    /** Unparseable body, wrong JSON type, or a missing non-nullable Kotlin property. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST_BODY",
            "Request body is missing or malformed",
            request,
        )

    /** e.g. `/api/notes/abc`, where `abc` cannot become a Long. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "Parameter '${exception.name}' has an invalid value",
            request,
        )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        exception: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "Required parameter '${exception.parameterName}' is missing",
            request,
        )

    /**
     * An unknown path is a 404 in the standard envelope, not a 500.
     *
     * Both exception types are needed: with static resource mappings disabled the dispatcher
     * raises [NoHandlerFoundException], while a request that reaches the resource handler
     * raises [NoResourceFoundException].
     */
    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleUnknownPath(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "No endpoint for this path", request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleUnsupportedMethod(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        respond(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "${exception.method} is not supported for this endpoint",
            request,
        )

    /**
     * Anything unforeseen. The real cause is logged with a stack trace; the client is told
     * only that something failed, so internal details never leak through the API.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedFailure(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception for {} {}", request.method, request.requestURI, exception)
        return respond(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request,
        )
    }

    private fun respond(
        status: HttpStatus,
        errorCode: String,
        message: String?,
        request: HttpServletRequest,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(
            status = status.value(),
            error = errorCode,
            message = message ?: status.reasonPhrase,
            path = request.requestURI,
            timestamp = clock.instant(),
            fieldErrors = fieldErrors,
        ),
    )
}
