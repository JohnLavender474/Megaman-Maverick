# Process log: mega-refactor-031b-engine-input

## Status

- State: planned
- Goal: input frames as an engine mechanism (`InputButton`, `InputFrame`, `InputSource`); `ControllerSystem` dispatches from each entity's own input source
- Done: draft `docs/SCOPE.md`
- Next: the owner reviews the draft scope and its open questions
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `aa97e6c68` plus the notes commit).
- Scan: item `031` `NOTES.md`, "Who drives an entity: driver, slot view and side" and "Long-term shape", plus a read
  of `ControllerSystem`, `ControllerComponent`, `IButtonActuator`, `ButtonActuator`, `ControllerSystemTest`, the
  Megaman controller component, `InputFrame`, `PlayerInputs`, `InputSource`, `MegaPlayer(s)`, `MegamanInput.kt`,
  `MegaMenuScreen` and the frame builder in `MegamanMaverickGame.render`. Draft `docs/SCOPE.md` written.
