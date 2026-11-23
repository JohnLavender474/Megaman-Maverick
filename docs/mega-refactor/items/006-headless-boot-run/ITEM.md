# 006: Headless boot-and-run per level (stage A)

- Status: idea
- Type: test
- Objectives: `O6`
- Depends on: `002`, `004`, `010a`
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

## Decisions

- B (2026-10-22): the headless runs go in the `smokeTest` suite.
- 2026-11-23 (owner rule): no stopgaps. No test-only fake `Graphics`, no GL stub so the renderers can be built, no
  `resetGlobals()`. `006` uses the real seams: the frame clock and the simulation/present split from `010a`, a public
  boot call with a synchronous asset load, and a settings and save store whose folder comes from the launch options.
- 2026-11-23 (owner accepted the recommendation): the frame clock and the split go in one branch `010a` before `006`,
  not in the `006` scope.
- 2026-11-23 (owner accepted the recommendation): add a test that runs one level twice with one seed and compares a
  hash per frame (Mega position, entity count). It does not fail the build: it shows whether the hash sets keyed on entities, bodies or fixtures
  break repeatability before `055` and `178i`.
- Scan of gaps, risks and options (2026-11-23): `temp/006-headless-scan/findings.md` (not committed). Open from it:
  pools and factories owned by the session, not a global `object`. No item covers this yet (`045` covers
  `GameObjectPools` scratch rules only).

## Open questions

- How many frames per level, and from which spawn points?
- Tick each level at each performance tier?
- What test runtime is acceptable in CI?

## Sources

- `NOTES.md` in this folder.
