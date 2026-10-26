# Process log: mega-refactor-031c-input-pipeline

## Status

- State: planned
- Goal: a new input pipeline with the same behavior: device adapters, one held mask per device, an engine edge builder with the lock; the poller goes
- Done: `docs/SCOPE.md` approved; draft `docs/CONTRACT.md`
- Next: the owner approves `docs/CONTRACT.md`; then the draft `docs/PLAN.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master`.
- Scan on `1df22a05c`: the engine `ControllerButton`, `ControllerButtons`, `ControllerUtils`, `ControllerPoller`,
  `IControllerPoller`; core `MegaControllerPoller`, `ControllerUtilsExtensions`, `ControllerMappingExtensions`,
  `ScreenController`, the frame builder and the lock in `MegamanMaverickGame`, the `controllerPoller` readers,
  `ControllerSettingsScreen`, `GameSettingsPanel`, `MegaControllerPollerTest`, `InputFrameTest`, the open input issues
  and item `031` `NOTES.md`. The owner chose the split and the five recommendations (item `ITEM.md`, decisions of
  2026-10-26). Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written. No new test rooms; the manual checks use
  `REFACTOR_TEST_029` `movement`, `INTRO_STAGE` `room1` and `room12`, the start screens, the settings screens and
  `--screen` for the skip screens.
