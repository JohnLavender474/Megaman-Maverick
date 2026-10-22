# 012: Named, composable freezes with a reason table

- Status: planned
- Type: refactor
- Depends on: none
- Related: `010`, `013`, `031`, `038`
- Branches: none yet

## Goal

Replace the direct `system.on` writes (last writer wins) with named freeze reasons that compose.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A `FreezeController` with named reasons and a reference count. Each reason stops a declared set of system groups and the input.
- Replace all direct `system.on` writes and the `TURN_CONTROLLER_*` events with push and pop.
- Exempt entities (like the X6 `0x08` flag).
- A debug overlay and log that show the active reasons.
- Fix the pause-restore bug and the stats-fill-while-paused bug first, as separate bug fixes (see `000`).

## Owner's input

Answer to review item 11 (2026-10-22, verbatim):

> I assume this is in reference to certain systems and tools being paused in runtime, e.g. behaviors and physics system when Mega is transitioning from one room to another, or switching on/off the controller during/after certain events? The X6 game utilizes global flags to handle this. I like the ability to implement granular switches which the current Maverick architecture at least somewhat supports, but having it cleaner, more modular, and also more maintainable is the goal.

## Open questions

- The list of reasons and the groups each one stops (for example, must animations run during a boss intro?).
- Must the pause stop audio and cosmetic animations?
- An X6-style "exempt" property in Tiled for each entity?

## Sources

- `NOTES.md` in this folder.
