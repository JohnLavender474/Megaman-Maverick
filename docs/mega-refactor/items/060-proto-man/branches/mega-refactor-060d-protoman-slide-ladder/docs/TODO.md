# TODO: mega-refactor-060d-protoman-slide-ladder

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- The ladder shot at a ladder foot: with the feet over the floor the sprite has no air drop, so the shot leaves 0.25
  tile under the muzzle (Proto Man, `ProtomanBuster.kt` `climbOffset`, and Megaman). Found in `060d` step 7.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- X's landing probe still skips one-way blocks while `DOWN` is held (`entities/megamanx/MegamanXMoves.kt:110`), but
  since `081g` D4 `DOWN` no longer passes X through them (scan inference, 2026-11-14).
