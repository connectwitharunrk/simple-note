# 0003 — Errors as values on the client, exceptions on the server

**Status:** Accepted

## Context

Failure is routine for a networked client: no connectivity, a slow server, a rejected edit.
It is exceptional for a request handler, where a failure ends the request.

## Decision

The two sides use different mechanisms, deliberately.

**Client** — every repository and use case returns
`AppResult<T> = Success(T) | Failure(AppError)`. The data layer catches every throwable at its
boundary (`ErrorMapper`) and converts it. Nothing above the data layer handles a `Throwable`.

**Backend** — use cases throw `DomainException` subtypes, and a single
`@RestControllerAdvice` converts them to the `ErrorResponse` envelope.

## Rationale

**Why values on the client.** A `when` over a sealed `AppError` is exhaustive: adding a new
error case becomes a compile error at every site that must handle it. With exceptions, a
forgotten `catch` is invisible until it crashes in someone's hands. Kotlin's own `Result` was
rejected because its failure type is `Throwable`, which reopens the same hole.

**Why exceptions on the backend.** A handler has exactly one way to fail — return an error
response — and Spring already provides a centralised place to do it. Threading a result type
through eight use cases would add plumbing that buys nothing, because there is no caller who
might reasonably continue after the failure.

**Structured, not stringly-typed.** `AppError` carries facts (`status`, `fieldErrors`,
`serverMessage`), never display text. Turning an error into a sentence happens in exactly one
file, `presentation/error/ErrorCopy.kt`. That is what allows the same failure to read
differently on the list and in the editor, and where localisation would slot in.

`DomainException` carries a stable `errorCode` so the exception handler maps failures by data
rather than a hand-maintained `when` that must be updated for each new subtype.

## Consequences

- `isRetryable` is derived from the error type in one place, so a Retry button never appears
  on a validation failure that would fail identically.
- Internal details never reach clients: the 500 handler logs the cause with a stack trace and
  returns only "An unexpected error occurred". A test asserts a leaked host address does not
  appear in the response body.
- A malformed response body becomes `AppError.Unknown` rather than crashing the app — also
  covered by a test.
