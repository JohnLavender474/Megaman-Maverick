# Scope: mega-refactor-019a-remove-second-spawn-pass

What this branch changes and why. This is a draft written ahead of time on `master`. The branch
does not exist yet, and the user has not approved this scope.

- Status: approved
- Approved: 2026-10-22

## Goal

Remove the second `spawnsMan.update(delta / 2f)` call in `MegaLevelScreen.render()`, and run one
`spawnsMan.update(delta)` before `engine.update`. This is the first, small step of `019`.

## Item

`019` (one spawn phase per tick with destroy notifications), the part: "Replace the two
`spawnsMan.update(delta / 2f)` passes with one spawn phase."

## Current state

From the decision K research (`items/019-spawn-phase/NOTES.md`):

- Pass 1 runs before `engine.update` (`MegaLevelScreen.kt:897`). Pass 2 runs after it
  (`:922`), then `playerSpawnsMan.run()` and a second drain of `spawns` (`:924-930`).
- Spawns queued in pass 2 are added at the start of the next `engine.update`. Pass 1 of the next
  frame queues before the same `engine.update`, so removing pass 2 delays no spawn.
- Pass 2 only refreshes spawner state one frame early: an event spawner whose entity died in this
  frame's `engine.update` can accept this frame's events, and a bounds spawner refreshes its
  stale "entered" flag.
- No spawner reads `delta`, so `delta / 2f` does nothing.
- No entity in the 18 live maps was found that depends on pass 2 in normal play.

## In scope

- Remove the `spawnsMan.update(delta / 2f)` call after `engine.update`.
- Add a `TODO` comment at the remaining spawn pass that names the two rare cases and points to
  `019b`.
- Change the call before `engine.update` to `spawnsMan.update(delta)`.
- Keep `playerSpawnsMan.run()` and the second drain loop where they are.

## Out of scope

- The destroy notification, the merge of the spawn queues and the spatial index. These are later
  branches of `019`.

## Affected areas and risks

- One file: `MegaLevelScreen.kt`.
- Two rare cases can change (inferred, not tested in the game):
  - A `spawn_room` entity dies in `engine.update` in the same frame as an accepted event for its
    own room. Without pass 2, the spawner drops the event, and the entity stays missing until the
    next room entry or respawn.
  - A bounds-spawned entity dies off screen in the frame just before its spawn point scrolls into
    view. Without pass 2, it respawns only after the camera leaves its spawn area and comes back.

## Overlap with other branches

Wave 1 (2026-10-22) runs `002a` (build and CI files only) and `008a` (the launcher, the boot code,
and maybe the level start code for `--room`). `008a` may touch `MegaLevelScreen.kt`, but not
`render()`, which is the only code this branch changes. The second branch to merge checks the
merge.

## Open questions

None open.

- (2026-10-22) The owner accepts the two rare cases. They go in `CONTRACT.md` under "Intended
  changes", and a `TODO` comment at the changed code names them and points to `019b`. The
  destroy notification comes in a later branch (`019b`).
