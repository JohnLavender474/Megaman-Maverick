# TODO: mega-refactor-031d-devices

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- For `031e`: a readable label for a bound trigger on the gamepad bindings screen. The screen shows the raw code, so a
  bound trigger shows as, for example, `B: 1005` (owner, 2026-10-30: accepted for `031d`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `RobotMasterIntroScreen.dispose` (`:639`) logs an error at shutdown after an uncaught exception: `stopMusic` runs
  after the audio is gone (`this.sourceToSoundId` is null, in `OpenALLwjgl3Audio.freeSource`). Seen in the `D3` crash
  log. Not from this branch: only the shutdown after the crash showed it.
