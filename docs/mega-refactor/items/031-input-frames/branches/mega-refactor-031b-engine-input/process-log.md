# Process log: mega-refactor-031b-engine-input

## Status

- State: planned
- Goal: input frames as an engine mechanism (`InputButton`, `InputFrame`, `InputSource`); `ControllerSystem` dispatches from each entity's own input source
- Done: `docs/SCOPE.md` and `docs/CONTRACT.md` approved; draft `docs/PLAN.md`
- Next: the owner approves `docs/PLAN.md`; then step 1 (branch start)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `aa97e6c68` plus the notes commit).
- Scan: item `031` `NOTES.md`, "Who drives an entity: driver, slot view and side" and "Long-term shape", plus a read
  of `ControllerSystem`, `ControllerComponent`, `IButtonActuator`, `ButtonActuator`, `ControllerSystemTest`, the
  Megaman controller component, `InputFrame`, `PlayerInputs`, `InputSource`, `MegaPlayer(s)`, `MegamanInput.kt`,
  `MegaMenuScreen` and the frame builder in `MegamanMaverickGame.render`. Draft `docs/SCOPE.md` written.
- The owner answered the four open questions in the draft scope, all as proposed. `docs/SCOPE.md` updated (in scope
  item 6, out of scope, overlap, decisions); `docs/NOTES.md` has the decisions. The scope waits for the owner's
  approval.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written. No new test rooms: `REFACTOR_TEST_029`
  `movement`, `REFACTOR_TEST_031` `spring_bouncer` and `pipe_portal`, and `INTRO_STAGE` `room1` and `room12` (the room
  that holds the boss `Gate`, player spawn `6`).
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (7 steps: branch start, baseline, engine input
  types, the entity's source bound by the slot view, `ControllerSystem` on the source with actuators by button,
  contract checks, play test and merge).
