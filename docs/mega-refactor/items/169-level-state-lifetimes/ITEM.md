# 169: `LevelState` by lifetime and owner

- Status: idea
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: none
- Related: `013`, `016`, `047`, `109`, `130`
- Branches: none yet

## Goal

`LevelState` (`core/.../levels/LevelState.kt`) holds values of 6 lifetimes in one session-lived class with no
`reset()`, and each value is cleared by hand at scattered sites. Give each lifetime one owner with one reset, and stop
copying state that another type owns, so that a missed clear cannot leak a value into the next level or transition.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the `047i` review (2026-11-17), the moves called
worth doing:

1. **Transition state:** `roomTransition` copies `CameraRoomTransition.transitioning` and needs manual clears at
   2 `MegaLevelScreen` reset sites, because `cancel()` does not call `onEnd`. Give it one owner (the transition, as
   `047i` did for its axis-only mark, or one `RoomTransitionState` with one `reset()`). About 35 reader lines.
2. **Level run:** `coins`, `levelPoints`, `timeSinceLastFloatingPoints` and `lastFloatingPointsType` in one object
   with one `reset()` at the level start. Fixes `docs/issues/2026-11-17-floating-points-combo-carries-over.md`.
3. **Beam:** `megamanBeamCenter` holds a reference to another object's `Vector2`; copy it, or let the beam steps give
   a read-only beam view. `megamanBeam` is never cleared at a level end (see also
   `docs/issues/2026-10-28-end-level-beam-flag-left-true.md`).

Called not worth doing now: a separate object for the world and the map; a split of `currentLevel` into a staged
level and a level in play; a move of `weaponsAttained`.

## Owner's input

- 2026-11-17, at the `047i` review of `LevelState.gatheredExitSide`: "Fan out to other subagents to probe the current
  `LevelState` design as it stands as a whole, probing into whether there is a better design for specific parts or
  the class as a whole." Decision D4 of `047i`: the findings become new items and a bug file after `047i`.

## Open questions

- Coins, points and the combo are one shared value for both slots. Shared or per slot in co-op and PvP (`109`)?
- Item `016` chose a session-lived `LevelState` with no reset as a whole (decision 3; `016a` `SCOPE.md`, out of
  scope). Does a level-run object with its own reset revise that decision, or fit inside it?
- `013` (the level-flow state machine) owns the level reset. Should this item wait for `013`, or come first?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/047-co-op/branches/mega-refactor-047i-transition-path/docs/SCOPE.md`, decisions D3, D4.
