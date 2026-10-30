# Notes: mega-refactor-051a-camera-read-interface

## Decisions

- 2026-10-30 (owner): `SCOPE.md` approved with the recommended answers to its 3 open questions:
  1. The interface is `ICameraView`, in a new package `engine/.../camera/`.
  2. `val direction` stays on the interface for now; `051b` moves it to the rotation behavior.
  3. The holder property is `gameCamera`, and it keeps that name after `051b`.
- 2026-10-30: the 2 Darkness sites are out of scope. `getRotatedBounds()` returns a new pooled rectangle on each
  call, so their `translate` calls are safe with live values (the 051 split had listed them).

## Overlap with other branches

- 2026-10-30: `001b-host-contract` (blocked, no branch): its draft scope puts "cameras and viewports" in the host
  contract (`items/001-maverick-engine/branches/mega-refactor-001b-host-contract/docs/SCOPE.md:20`). This branch adds
  the host property `gameCamera` and the engine interface; `001b` takes them as inputs. No file conflict now.
- 2026-10-30: `031d-devices` (planned) and `031f-menu-input` (blocked): input only. No overlap.
- 2026-10-30 (before the merge): `084a-player-state` (active) changes `Megaman.kt` (typed player state, teleport
  hooks). 051a changes `Megaman.kt:214` only (`services.gameCamera.direction`); `084a` records the same in its
  `SCOPE.md` ("Overlap"). Whichever merges second gets a one-line rename at most.
- 2026-10-30 (before the merge): `031d-devices` (active) changes input and controller files only. No overlap.
