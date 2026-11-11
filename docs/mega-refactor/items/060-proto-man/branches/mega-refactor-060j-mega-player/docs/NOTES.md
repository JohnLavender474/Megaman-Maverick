# Notes: mega-refactor-060j-mega-player

## Decisions

- Private helpers that a moved `define...Component()` body calls become `internal` in their file (D3 moves only the
  `define` bodies). Megaman: `setToNextWeapon` and the 4 sprite helpers. X: `stopRunning`, the 2 charge tints and
  the 4 behavior factories in `MegamanXMoves.kt`.
- `MegaPlayer.defineCameraSubjectComponent()`, `definePlayerProgressComponent()` and `defineFreezeComponent()` are
  `internal`, not `private`: the Megaman and X tests build these components on their own.
- Test doubles: `TestMegaPlayer` stubs the abstract members (they throw). `TestBodyPlayer` is the old
  `TestHealthPlayer`, unchanged, for the 2 tests that spawn a player through a real `GameEngine`
  (`KoopaShellKickerTest`, `CartV2DeadPlayerTest`): a `MegaPlayer` double cannot go through `MegaPlayer.init()` and
  `onSpawn()` there. The scan said that the doubles never call `init()`; that was wrong for these 2 tests.

## Fit for future characters

No code for these in `060j` (`O1-R5`). Each item below becomes code when a character with that need arrives.

- Bass, Zero: no charge. The charge stays in each class, so they add none.
- Zero: sabre only. The buster, the weapons, `aButtonTask` and the `SELECT` action stay out of `MegaPlayer`.
- Axl: hover and rolling dodge are behaviors and controller parts (abstract members). A dodge with invincibility frames
  needs a lock reason or an `invinciblePredicates` entry, not a change of `MegaPlayer.invincible`.
- Bass: 7-way rapid aim is a controller part.
- Own values: the damage sound, the damage duration, the start health and the heart-tank bump (`MegamanValues`, the
  D2 `TODO`), `getType() = MEGAMAN`, the death orb ring and the freeze mash buttons. Each becomes an open member, or a
  `PlayerValues`, when a character with another value arrives.
- `eventKeyMask` is fixed. A character that listens to more events needs an open mask.
- The progress component shares one profile for all characters (items `047`, `059`).
- The damage edit uses the Megaman enhancement scalar (item `023`).
- `direction` stays in each class; a shared setter with the camera turn is item `058`.
- An enemy clone (item `121`) that calls the player moves: each move function that takes `player: MegaPlayer` has a
  `TODO` to widen the type.

## Overlap with other branches
