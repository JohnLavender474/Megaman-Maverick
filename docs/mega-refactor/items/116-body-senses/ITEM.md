# 116: Body senses as typed state, then a sense history

- Status: idea
- Type: refactor
- Objectives: `O1`, `O5`
- Depends on: none
- Related: `010`, `017`, `048`, `087`, `098`
- Branches: none yet

## Goal

A body holds its senses as typed state, not as one property bag entry for each sense. After that, a body can answer what it sensed in the last frame. Code then needs no hand-written `was` flags, and a read after the reset before `onDestroy` works.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Each branch needs an `O5-R2` measurement, because senses are hot reads and writes.

1. First branch: `BodySenses`, one object on each engine `Body`, with a bitmask and an index from `core`, as `LocksComponent` does. The extensions `isSensing` and `setBodySense` keep their signatures, so 0 of the 436 read lines change. Remove `IBodySenseListener`, which has 0 callers.
2. Later branch, only with real callers in the same branch: a fixed-size frame history. A frame entry holds "any sense in this frame" and "senses at the end of this frame". The body also holds the senses of the prior step.

Rules for the history, from the research in `NOTES.md`:

- A step is a world cycle. A reset is not a step: it happens once for each entity life.
- A query reads the last finished frame, because the open frame is only partly filled.
- A frame with no step carries the last value forward, or a grounded body looks airborne.
- `WorldSystem` marks the frames, before its `if (!on) return`, so that it reaches each body.
- Review this against `010` first: a fixed tick removes most frames with no step.

First callers: the sand drop rule in `AbstractEnemy`, the `BODY_TOUCHING_BLOCK` drop check (`docs/issues/2026-11-02-enemy-drop-block-check-never-true.md`), `BabySpider` (prior step) and `SuicideBummer` (frame edge). Leave `Petaspark` and `SuctionRoller` as they are: their state comes from `facing`, and a swap changes their wall hop.

## Owner's input

2026-11-02 (verbatim): "It seems better to have a fixed-size queue instead, maybe of only the past 1 or two resets? That way, each body automatically has a record of what is was sensing in previous frames."

2026-11-02 (verbatim): "each frame gets an entry; each frame entry has one entry for each step (reset). A simple query for a frame returns the set of all senses recorded in its steps."

## Decisions

- 2026-11-02 (owner): for a read at destroy, a sense counts if it was true in any step of the last frame.
- 2026-11-02 (owner): `fix/quicksand-two-players` adds no history and no sense object. Its sand drop rule turns off `dropItemOnDeath` when the destroy is queued, with a `TODO` that links this item and `048`.

## Open questions

- Does `O1-R4` cover typed keys in a bag (`098`)?
- Which new features use edges, for example coyote time or landing dust? They decide between a per-step and a per-frame read.
- Do the 3 `Megaman` reads after reset (`ClimbBehavior`, `WallSlideBehavior`, `AirDashBehavior`) stay under `087`?

## Sources

- `NOTES.md` in this folder.
