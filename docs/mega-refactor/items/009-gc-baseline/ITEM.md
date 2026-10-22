# 009: Allocation and GC baseline per branch

- Status: planned
- Type: tooling
- Depends on: none
- Related: `006`, `008`, `010`, `011`, `026`, `032`, `044`
- Branches: none yet

## Goal

Measure allocations and GC per frame with almost no cost, and compare two branches.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Add per-frame allocated bytes and GC count and time to `RuntimeDiagnostics` through a platform-neutral probe.
- Opt-in JFR recording with a custom frame and room event, and `-Xlog:gc` beside it.
- The same collector and heap flags for benchmark runs, and a warm-up rule.
- An `analyze.py` compare mode for two captures.
- Later: a CI gate on bytes per frame for a fixed headless scenario.

## Owner's input

Answer to review item 8 (2026-10-22, verbatim):

> Yes, GC has been a bain for certain hits to performance, so being able to easily record and benchmark that (without causing slowdown to the game runtime) would be ideal

## Open questions

- Does the shipped JRE include JFR? Move to JDK 21 (generational ZGC)?
- Which scenarios make the baseline (Wily 3 background, a gauntlet, a boss fight)?

## Sources

- `NOTES.md` in this folder.
