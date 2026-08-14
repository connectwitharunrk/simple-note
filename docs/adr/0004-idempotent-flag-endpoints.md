# 0004 — Pin and archive take a value, not a toggle

**Status:** Accepted

## Context

The obvious API for a pin button is `POST /api/notes/{id}/toggle-pin`: the client knows the
user tapped, the server knows the current state, and no request body is needed.

## Decision

`PATCH /api/notes/{id}/pin` with body `{"pinned": true}` — the caller states the desired
state. Same for archive.

## Rationale

A toggle is **not idempotent**, and that breaks in two ordinary situations:

1. **A retry.** The request succeeds, the response is lost to a dropped connection, the client
   retries — and the note flips back. The user tapped once and ended where they started.
2. **Two devices.** Both see an unpinned note, both toggle. With a toggle the result depends on
   arrival order and lands on "unpinned". With an explicit value both say `pinned: true` and
   the result is `pinned: true` regardless of order. This matters directly for the
   multi-device support named as a future feature.

Setting a value it already has is a no-op server-side: `updatedAt` does not move, so a
redundant retry cannot reshuffle the list.

## Consequences

- The client must know the current state to compute the new one. It already does — it is
  rendering it.
- The same reasoning is mirrored in the client use cases, which also take the value rather
  than toggling, so the ViewModel and the API agree.
- Archiving also unpins, decided at the same time: a note filed away should not reclaim the
  top of the list when restored.
