# 006: Headless boot-and-run per level (stage A)

- Status: idea
- Type: test
- Depends on: `002`, `004`
- Related: `008`, `009`, `010`, `033`, `055`
- Branches: none yet

## Goal

Boot every level headless and run it for N frames, as a coarse check for the loop and engine branches and as the runner for record and replay.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Make boot headless-safe: injectable batch and shape renderer, injectable frame delta, synchronous asset load, temp prefs dir, sync pathfinding, auto-perf off.
- A `smokeTest` spec boots every `LevelDefinition` and runs N fixed-step frames with no input. It asserts no exception, Mega alive, bounded entity and pool counts, and the expected level and room.
- Record frame time and allocations per level as a by-product (feeds `009`).
- Stage B (inputs plus trace diff) stays in `055`.

## Owner's input

Answer to review item 5 (2026-10-22, verbatim):

> What would the headless-boot-and-run setup gain us?

## Open questions

- How many frames per level, and from which spawn points?
- Tick each level at each performance tier?
- What test runtime is acceptable in CI?

## Sources

- `NOTES.md` in this folder.
