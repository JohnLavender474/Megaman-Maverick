# 157: D-1000 overdrive on the shared boss health phases

- Status: idea
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `077`
- Branches: none yet

## Goal

`D1000` has its own half-health overdrive (`D1000.kt`, the `HALF_HEALTH` check) and calls `translateHealth` directly. Branch `077a` added shared health phases to `AbstractBoss` (`healthPhases`, roll-over, a bar with one color per phase). Decide: move the D-1000 overdrive to 2 phases (of 15 or of 30 bits), or keep it as it is.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

## Open questions

## Sources

- From the `077a` backlog candidates (2026-11-15).
