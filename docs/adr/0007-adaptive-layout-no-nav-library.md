# 0007 — Adaptive layout without a navigation library

**Status:** Accepted

## Context

The app must run on phone, tablet, iPad and desktop from one codebase, and has two
destinations: a list and an editor. Compose Multiplatform has a navigation library
(`androidx.navigation`) and an adaptive layout library
(`material3-adaptive`) that would each handle part of this.

## Decision

Neither. Layout switches on a single `BoxWithConstraints` breakpoint at **840dp**, and
navigation is one boolean (`isEditorOpen`) plus the list's `selectedNoteId`, both owned by
`HomeScreen`.

```kotlin
val isTwoPane = maxWidth >= TwoPaneBreakpoint   // 840.dp
```

Below the breakpoint: list **or** editor. At or above: list **beside** editor.

## Rationale

**On the navigation library.** A back stack models "I went somewhere, I can come back". In the
two-pane layout both destinations are on screen simultaneously, which is not a stack at all —
it is a selection. Adopting a nav library would mean either maintaining two navigation models
or contorting the two-pane case into a stack. With two destinations, the state is genuinely
two variables.

**On 840dp.** This is Material's medium/expanded boundary. A phone stays below it in both
orientations; a tablet in landscape, an iPad and any reasonable desktop window sit above. One
number, no device-class detection, no platform checks.

**On `BoxWithConstraints` over `WindowSizeClass`.** It reacts to the *container*, not the
window, so a desktop window being dragged narrower switches to single-pane live — verified by
resizing the running app.

## Consequences

- Zero navigation dependencies, and the same screen composables serve both layouts —
  `NotesListScreen` takes a `showSelection` flag, `NoteEditorScreen` takes `showCloseAction`.
- A third destination (settings, a tag manager) would strain this. That is the point at which
  a navigation library earns its place, and adopting one then is a contained change because
  navigation state already lives in exactly one composable.
- Related: `NotesListScreen` and `NoteEditorScreen` do **not** size themselves; the caller
  passes a modifier. A child forcing `fillMaxSize()` on a modifier the parent has already
  constrained is how adaptive layouts quietly break.
