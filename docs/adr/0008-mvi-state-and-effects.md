# 0008 — MVI with State separate from Effects

**Status:** Accepted

## Context

MVI is often described as "one state object drives the screen". Taken literally, one-shot
events — a snackbar, a navigation, a confirmation prompt — end up as fields on that state.

## Decision

`MviViewModel<Intent, State, Effect>` exposes two streams:

- `StateFlow<State>` — everything currently true about the screen. Durable, replayable.
- `Flow<Effect>` from a `Channel(BUFFERED)` — things that happen once.

## Rationale

**Why the split.** A snackbar modelled as `state.message: String?` reappears on every
recomposition until something explicitly clears it, and it re-fires after a rotation or a
window resize because the new collector replays the current state. A `Channel` is consumed
exactly once, which is the actual semantics of "show a snackbar".

**Why `BUFFERED`.** Effects emitted before the UI subscribes are held rather than dropped
(`CONFLATED` would discard all but the last, `RENDEZVOUS` would suspend the emitter).

**Effects carry facts, not sentences.** `NoteDeleted`, `NoteArchiveChanged(archived)`,
`ShowError(error)` — the UI decides the wording, consistent with ADR 0003.

## Supporting decisions

**Loading is one flow, not scattered `launch` calls.** Every reason to reload — first load,
keystroke, filter switch, refresh, retry — becomes a `LoadRequest` on a `MutableStateFlow`,
and `flatMapLatest` cancels whatever was in flight. This is what makes fast typing correct:
the response for "mil" cannot arrive after "milk" and overwrite it. Debounce applies **only**
to typing; filter switches and refreshes skip it so they feel instant.

**Pin is optimistic.** State updates before the request runs, and a failure restores the
*captured* previous list rather than guessing the inverse. Waiting for a round trip would make
the list feel dead on a slow connection.

**`isEmpty` is not `notes.isEmpty()`.** It is
`notes.isEmpty() && !isLoading && error == null`, so the UI never claims "no notes yet" while
still loading or when a request failed. Two tests exist purely to pin this down.

## Consequences

- Screens are stateless functions of state, so 43 ViewModel tests cover the behaviour without
  a single UI test.
- `CancellationException` must be caught first and rethrown in the data layer — the search box
  cancels its in-flight request on every keystroke, and swallowing that would flash an error
  at the user and break structured concurrency.
- Testing effects requires care: `advanceUntilIdle()` deliberately stops advancing once only
  `backgroundScope` coroutines remain, so the test helper drains with `runCurrent()` before
  each read.
