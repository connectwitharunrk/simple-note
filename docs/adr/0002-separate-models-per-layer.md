# 0002 — A separate model per layer, not one shared class

**Status:** Accepted

## Context

A note is the same idea everywhere, so a single `Note` class used as JPA entity, REST DTO and
domain model is tempting — it would remove four mapper functions and three classes.

## Decision

Four distinct representations on the backend, three on the client:

| Where | Type | Shape |
|---|---|---|
| Backend domain | `Note` / `NewNote` | Immutable, invariants enforced in `init` |
| Backend persistence | `NoteEntity` | Mutable `var`s, nullable id, JPA annotations |
| Backend wire | `NoteResponse` / `CreateNoteRequest` | Bean Validation annotations |
| Client wire | `NoteDto` | `@Serializable`, timestamps as `String` |
| Client domain | `Note` / `NoteDraft` | Immutable, no serialization annotations |

Additionally, `Note` (persisted, non-null `id`) is separate from `NewNote` (not yet persisted,
no `id`).

## Rationale

The three representations are pulled in incompatible directions:

- **JPA** requires a no-arg constructor and mutable fields to manage identity and dirty
  checking. A Kotlin `data class` as an entity is a known anti-pattern — `equals`/`hashCode`
  over mutable fields breaks inside collections during a session.
- **The wire format is a published contract.** Renaming a column should not break clients;
  clients adding a field should not require a migration. The DTOs use `pinned`/`archived`
  while the columns are `is_pinned`/`is_archived` — precisely the kind of independence this
  buys.
- **The domain wants to be immutable and always valid.** Every `Note` in the system has passed
  its invariants, because they run in `init` and therefore also on `copy()`.

Splitting `Note` from `NewNote` is what removes nullable ids: no code anywhere unwraps an id
with `!!` or handles a `Long?`, because a note that has no id is a different type.

## Consequences

- Four small hand-written mapper functions. They are ~10 lines each and read clearly, which is
  why no mapping library (MapStruct) was added.
- Client timestamps stay `String` in the DTO and are parsed in the mapper, so a malformed date
  becomes an `AppError` rather than an exception thrown from inside kotlinx-serialization.
