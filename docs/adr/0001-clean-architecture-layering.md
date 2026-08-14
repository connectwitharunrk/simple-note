# 0001 — Clean Architecture layering on both sides

**Status:** Accepted

## Context

The app is small — one entity, eight operations. Layering can easily become ceremony at this
size, and the brief explicitly warns against unnecessary complexity while also asking for
Clean Architecture and room to grow.

## Decision

Both client and backend use the same dependency rule: the domain layer depends on nothing, and
every other layer points inward. The domain declares ports; outer layers implement them.

Concretely, the only interfaces that exist are the ones representing a **volatile boundary**:

- `NoteRepository` on the backend, implemented by `NoteRepositoryAdapter` over JPA
- `NoteRepository` on the client, implemented by `NoteRepositoryImpl` over Ktor

Use cases are concrete classes with a single `operator fun invoke`. There is no
`CreateNoteUseCase` interface with a `CreateNoteUseCaseImpl` behind it.

## Alternatives considered

**An interface for every class.** This is often presented as "proper" Dependency Inversion. It
would add sixteen files that never have a second implementation, and every one would have to
be kept in sync with its only implementor. DIP is about depending on abstractions *at the
points where the implementation is likely to change* — persistence and network. A use case's
implementation changing means its behaviour changed, and its tests should change with it.

**No layering, service classes calling Spring Data directly.** Faster to write, and defensible
for something this size. Rejected because the brief names extension points — auth, sync,
offline-first — that all land exactly on the boundaries this layering creates.

## Consequences

- Business rules are testable without a servlet, a database, or a network. 84 backend and 108
  client tests run in seconds.
- Adding an offline cache means writing one new `NoteRepository` implementation and changing
  one Koin binding. Nothing above the data layer knows.
- The cost is real: a create operation touches a DTO, a command, a use case, a domain model,
  an entity, and two mappers. That is the price paid for the isolation above.

## Reversibility

Cheap to collapse (delete the ports, call Spring Data / Ktor directly from the services),
expensive to introduce later once call sites have multiplied.
