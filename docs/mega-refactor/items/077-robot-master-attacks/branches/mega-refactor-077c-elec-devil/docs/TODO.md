# TODO: mega-refactor-077c-elec-devil

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Spark Serpent cubes and player shots (owner, 2026-11-18): bullets and buster shots are destroyed by a cube;
  `MoonScythe` and `Axe` cut through the cubes and become "electrified" (new state and animations in the projectile
  classes). Until then, the serpent cubes keep the current shot rule; put a `TODO` at the cube contact code.
- `Bosses3` atlas cannot be repacked with `Basic` (found 2026-11-18): `TexturePacker Bosses3.tps` fails with "No
  space left" for `D1000/face`, `D1000/glow`, `D1000/lamp`, `D1000/screw` and `D1000Core/beam`. The sheet came from
  the `081f` `MaxRects` recipe. Per `docs/ways-of-working/assets.md`, move the D-1000 frames to a new atlas (frames
  folder, `.tps`, `TextureAsset` entry). Until then, a `Bosses3` frame change is written into the sheet in place
  (as `077c` did with `temp/077c-nes-2x-art/splice_bosses3.py`). Since `077c` moved ElecDevil to `Bosses4`, `Bosses3` fails by 1 sprite, and the
  `Bosses3` sheet still holds the old ElecDevil regions (unused) until its next repack.

- Spark Serpent, Circuit Floor and Storm Rain (owner, 2026-11-19): `PLAN.md` steps 5 and 6 of `077c`, moved to a
  new branch of item 077. The decisions, the serpent path objects (ids 508, 509 in `WilyStage1_v2.tmx`, the
  `serpent_paths` polylines in `RefactorTest077.tmx`) and the art drafts are in `077c`'s `docs/NOTES.md`. The phase-2
  pick is a `TODO` at `ElecDevil.pickMoveAttack`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
