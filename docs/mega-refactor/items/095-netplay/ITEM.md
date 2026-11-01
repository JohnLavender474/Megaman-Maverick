# 095: Netplay with lockstep (`O2` phase 3)

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `094`, `010`, `011`, `055`
- Related: `031`, `047`, `012`, `013`, `097`
- Branches: none yet

## Goal

Two machines play one session through lockstep. Players are fixed at session start. This is phase 3 of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- The session setup: settings that change gameplay go to all peers.
- Input frames as the payload, with an input delay buffer.
- Pause and pause-menu actions as inputs (`LevelPauseScreen.kt:562`, `:578`).
- A desync check that compares state for each tick.
- The review of the 34 hash-collection declarations and the 17 animation reads in `O2`.
- The transport. Web needs its own transport (see `O3`).

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
