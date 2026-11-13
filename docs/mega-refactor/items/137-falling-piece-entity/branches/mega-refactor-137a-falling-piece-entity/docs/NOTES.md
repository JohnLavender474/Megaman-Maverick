# Notes: mega-refactor-137a-falling-piece-entity

## Decisions

Claude's decisions during the work (2026-11-13):

- `FallingPieceDef` is an enum with 3 values. "None" for the fall clamp, the spin and the blink is `null` in the def.
  As a spawn property, a present key with a `null` value means "none" (`Properties` keeps `null` values, and
  `containsKey` tells "absent" from "none"). Jitter `0` means none, and then the piece draws no random value.
- The sprite size is a small sealed type, `FallingPieceSize.Tiles(size)` or `FallingPieceSize.RegionScale(scale)`,
  because debris takes its size from the region and the other kinds from a fixed tile size.
- New keys `ConstKeys.JITTER` and `ConstKeys.BLINK`: no existing key fits (`OFFSET` is a fixed offset elsewhere,
  `SPAWN_BLINK` is the enemy spawn blink). Reused: `GRAVITY`, `CLAMP`, `FRICTION_X`, `SIZE`, `SPIN`.
- `FallingPiece.spawn` does not copy the position and velocity vectors; `onSpawn` copies their values, as the 3 old
  classes did. So `BreakableBlock` (one shared center) and `WoodCrate` (a pooled impulse vector) behave as before.
  `throwDebris` copies the position for each piece, as `spawnPieces` did.
- `throwDebris` picks the X6 speeds before the spawn, so the 2 table draws come before the 2 jitter draws of
  `onSpawn` (before: jitter first). Same distribution; recorded in `CONTRACT.md` under "Intended changes". The
  temporary suite compared the sets of start velocities (all 64 pairs) and the jitter range over 400 seeds.
- `load` and `throwDebris` keep a Kotlin `Array<String>` for the keys, as `DebrisPiece` had, so that the 5 owner
  declarations and the D-1000 tests stay unchanged (`060c` and `139a` change the same files). A backlog candidate in
  `TODO.md`.
- `FallingPiece.WOOD_CRATE_REGIONS` holds the 10 wood keys; `init()` loads them and the 4 `BlockPieceColor` keys.
  `WoodCrate` picks a key with the same random call as before (`getRandom(0, 9)`).
- The region cache is a companion `ObjectMap` (as before). In tests it outlives a spec, so the tests compare the
  region key (`FallingPiece.key`), not the region object; the smoke test loads the real regions.
- The sprite pre-process sets region, size, center, origin, rotation, alpha (1) and hidden each frame; `onSpawn` sets
  gravity, the Y clamp, X friction, center and velocity. `BlockPiece`'s unused `AudioComponent` is gone.

## Overlap with other branches

Checked 2026-11-13, `master` at `add9d66d2`.

| Branch | State | Overlap with 137a | Risk |
|---|---|---|---|
| `mega-refactor-060c-protoman-buster` | 10 commits, and uncommitted edits | Committed: imports and damage tables in `SabreBlock`, `WoodCrate`, `BigAssMaverickRobot`, `D1000`, `HoverGunner`, `Junkroid`, `MetaWheelF` (7 of the 12 caller files), and 2 lines in `entity-tags.txt` (`players.PlayerSabreSlash`, `projectiles.ProtoBuster`). Uncommitted: damage values in `WoodCrate.kt` and `BigAssMaverickRobot.kt`. | Text conflicts in import blocks only. 137a changes the `DebrisPiece` import and the debris calls; 060c changes the sabre and buster imports and damage tables. No shared logic. Simple to resolve at the second merge. |
| `mega-refactor-138a-wreck-into-mock-sprite` | just started, no commits | Planned: remove `decorations/Wreck` and its `entity-tags.txt` line, in the `decorations.` lines near the 3 piece lines. | Text conflict in `entity-tags.txt` only. |
| `mega-refactor-139a-shared-enemy-shot` | just started, no commits | Planned: change `JunkShot`, which `Junkroid`, `HoverGunner` and `D1000Rig.kt` use. `D1000Rig.kt:160` and `:168` name both `JunkShot` and `DebrisPiece` on the same lines. | Same-line conflict in `D1000Rig.kt`; the right result keeps both renames. Simple. |

No branch changes the 3 piece classes, `BreakableBlock.breakApart`, `CrumblingBlock` or the debris throw.
