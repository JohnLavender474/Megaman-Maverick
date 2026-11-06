# Scope: mega-refactor-081b-x-run-jump-shoot

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D3)

## Goal

Phase 1 of X: a new player class `MegamanX` plays in slot 0 on `X6_INTRO`, in single player, with stand, run, jump,
fall and the buster. Megaman is unregistered and dormant on that level. X is dormant on every other level.

## Item

`081`. From `ITEM.md`, "Branch split", step 1: "Phase 1: the `MegamanX` class in slot 0, the `characters` field on
`LevelDefinition` and the slot-0 swap at level start, the focus set at each level start, the `TextureAsset` entry,
body, sprites, stand, walk, jump, fall and buster. The workarounds for the weapon reads in the screens.
`jumpBehavior()` gets parameters."

Decisions that apply (`ITEM.md`, "Decisions", 2026-11-05): `characters` with default `setOf(MEGAMAN)`; slot 0 swap;
more than 1 player on an X level may throw; focus set at level start; workarounds with `TODO`, documented in `024` and
`029`; X shares Megaman's progress, with a `TODO`; merge directly into `master`.

## Current state

The full scan, with `file:line` evidence, is in `temp/081b/scan.md` (git-ignored; written 2026-11-06 at `1653c3d45`,
then the branch took in `038a` at `c9d99c6bc`). Main facts on `master` at `c9d99c6bc`:

- `postCreate` makes and registers 1 `Megaman` for each slot (`MegamanMaverickGame.kt:446-457`). `register` and
  `unregister` exist (`engine/.../players/Players.kt:13-26`). Every `startLevel` caller runs after `engine.reset()`,
  so no player is in the engine at a level start.
- The camera focus is set once, in `MegaLevelScreen.init()` (`:326`). Every other read of the player goes through
  a getter on slot 0, so it follows a swap.
- 10 `as Megaman` lines outside `entities/megaman/`. 4 of them crash on `X6_INTRO`: `PlayerStatsHandler.kt:106,115`
  (the HUD, first frame), `MegaLevelScreen.kt:564` (a `WeaponEnergyBulb` drop), `EndLevelEventHandler.kt:173`
  (the beam-out after `D1000`), `MegamanMaverickGame.kt:822` (`resetPlayerProgress`, which runs before the next
  `startLevel`). `LevelPauseScreen.kt:286,569` crash only when the game state owns weapons.
- All 8 behaviors and all 12 files in `entities/megaman/components/` take the `Megaman` type. `jumpBehavior()` reads
  `aButtonTask`, the weapon, `jumpVel`, `wallJumpVel`, `JUMP_BOOST` and the Mario sound from `Megaman`.
- The atlas `MegamanX.png` and `MegamanX.txt` exist (36 strips of 48 x 48 cells, names with no prefix). There is no
  `TextureAsset` entry, no `stand_shoot` strip, no `fall` strip and no charge art.
- `D1000` takes damage only from a fully charged `ChargedShot` (`D1000.kt:131-136`). The X6 enemies' damage tables
  key on `Bullet` and `ChargedShot`.
- `room1` of `X6_INTRO` goes down a ladder at tile x 62. `LadderTop` is `COLLIDE_DOWN_ONLY` (`LadderTop.kt:22`), and
  only a climbing body passes it. A phase-1 X cannot pass it.

## In scope

- A character enum (`MEGAMAN`, `X`) and `LevelDefinition.characters` (default `setOf(MEGAMAN)`); `X6_INTRO` sets X.
- In the game: a private roster of the player entities, all made in `postCreate`. At `startLevel`, before
  `levelScreen.start()`, slot 0 gets the entity of the level's character (`unregister` and `register`). It throws if
  the outgoing player is live, or if there is more than 1 player on a level with no `MEGAMAN`. `TODO` for co-op
  (`047`).
- `cameraFollow.setFocus` moves from `MegaLevelScreen.init()` to the top of `start()`.
- `MegamanX`: its own body, sprites, animations, controller, run and buster. It calls the `081a` functions in
  `players/` and uses `PlayerDamageNegotiator(this)`. X6 values in `MegamanXValues`, in Maverick units (seconds and
  tiles x `PPM`).
- `TextureAsset.MEGAMAN_X`.
- `jumpBehavior()` gets a generic receiver and parameters, so that Megaman and X both call it. Megaman jumps the same.
- The 4 crash fixes above, and `LevelPauseScreen.kt:286,569`, as `is Megaman` workarounds with `TODO`s for `024`.
  `024` and `029` get the workaround in their docs at merge.
- `resetPlayerProgress` resets Megaman's weapon through the roster, not through slot 0.
- X gets its max health from the shared game state, with a `TODO` (`059`, `047`).
- The ladder at tile 62 (D1): a temporary rule with a `TODO`: X passes a one-way top (`COLLIDE_DOWN_ONLY`) while `DOWN` is held. Climb comes in a later phase.
- Charge feedback (D2): a tint flash on X's sprite at half and full charge, with a `TODO` for charge art.
- A test map `REFACTOR_TEST_081`, with rooms for a flat run and jump, a slope and buster targets, because `--room`
  cannot start at the spawns `1` and `2` of `X6_INTRO`.

## Out of scope

- Wall slide, air dash, ground dash, dash wall kick, climb, crouch, and the sabre (phases 2 to 4, open question 10).
- `CharacterDef` (open question 7), and the overrides parameter on `PlayerDamageNegotiator` (X uses the same table).
- X's own beam-in and beam-out art. X keeps Megaman's beam, with a `TODO`.
- `GetWeaponScreen` (an extra level gives no reward, so X does not reach it) and the `shooting` read in
  `EntityQueries.kt:43` (no `Met` on the map).
- Co-op as X.
- Split of the `damaged` burst frames (open question 5).

## Affected areas and risks

- `MegamanMaverickGame.kt` (`postCreate`, `startLevel`, `resetPlayerProgress`), `LevelDefinition.kt`,
  `MegaLevelScreen.kt`, `PlayerStatsHandler.kt`, `EndLevelEventHandler.kt`, `LevelPauseScreen.kt`,
  `TextureAsset.kt`, `JumpBehavior.kt` and Megaman's behaviors component, and the new `entities/megamanx/` package.
- Risk: the `jumpBehavior` parameters change Megaman's jump code. `MegamanBehaviorsTest` and a control play check.
- Risk: a swap while a player is in the engine. Guarded by a fail-fast check.
- Risk: the X6 values are small next to Megaman's (jump about 2.05 tiles high, walk 3.75 tiles/s). D3: use them as they are, in `MegamanXValues`, and tune after the play test.

## Overlap with other branches

- `038a` merged before this branch did code work; the branch took it in. The respawn steps read slot 0 through
  getters, so the swap before `start()` covers them.
- `038b` (planned) changes `playerSpawnEnd`. X calls it, so `038b` changes the function, not X's call.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | no effect | The roster is a field of the game, not a static. |
| `O1-R2` | no effect | `MegamanX` extends `AbstractHealthEntity`, as `Megaman` does (`TODO` `048`). No new base class. |
| `O1-R3` | no effect | No new event. |
| `O1-R4` | helps | `characters` is a typed field. |
| `O1-R5` | no effect | Each new API has a production caller (see the table below). |
| `O8-R1` | helps | X uses the `players/` functions and the `entities/state/` components, not a copy of `Megaman.kt`. |
| `O8-R2` | waiting | The workarounds add `is Megaman` lines (0 today). Allowed by the owner (2026-11-05); `024` removes them. |
| `O8-R3` | no effect | |
| `O8-R4` | waiting | X has only the buster. |

### New APIs and their first callers

| API | Production caller |
|---|---|
| character enum | the `LevelDefinition` entries, the swap |
| `LevelDefinition.characters` | `X6_INTRO`, `startLevel` |
| `MegamanX` | `postCreate`, the swap |
| `MegamanXValues` | `MegamanX` body, controller and buster |
| `TextureAsset.MEGAMAN_X` | `MegamanX` animations |
| `jumpBehavior(params)` | Megaman's and X's behaviors |
| the roster and the swap | `startLevel`, `resetPlayerProgress` |

## Open questions

None. D1 to D3 answered 2026-11-06 (see `NOTES.md`, "Decisions").
