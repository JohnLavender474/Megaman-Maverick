# Scope: mega-refactor-183b-charged-shot

- Status: approved
- Approved: 2026-11-23 (D1 a, D2 b, D3 a)

## Goal

`ChargedShot`, `ChargedShotExplosion` and `ChargedShotResidual` draw only at multiples of 90 degrees. The trajectory of
a charged shot is coerced to a multiple of 45 degrees. A shot on a diagonal draws a new diagonal region, turned by a
multiple of 90 for each of the 4 diagonals.

## Item

`183` (NES-faithful rotation, GitHub issue #4). From `ITEM.md`, "Proposed scope":

| Branch | Content |
|---|---|
| `183b` | `ChargedShot`, `ChargedShotExplosion` and `ChargedShotResidual`: a small first batch |

Decisions in `ITEM.md` (2026-11-23): `183b` is the first branch; each `183` branch squash-merges directly into `master`.

Owner's input for this branch (2026-11-23, verbatim): "I think we should ensure that charged shots' trajectories are
coerced to 45-degree increments, and we have a new animation for the diagonal movement that we can rotate for all 4
diagonal directions. need to account for the charged-shot explosion too"

## Current state

Scan of the worktree at `5188de2c6` (same as `master`).

### `ChargedShot` (`core/.../entities/projectiles/ChargedShot.kt`)

- `trajectory` is set from the spawn prop `TRAJECTORY` (`:110`) and changed only in `bounce()` (`:178-234`).
- Sprite: `sprite.rotation = trajectory.angleDeg()` (`:280`), origin at the center. The art points right (angle 0).
- Regions: `ChargedShot_v2` (full) and `HalfChargedShot` (half) in `PROJECTILES_1`, 2 frames each, 32x32 per frame,
  drawn on 2x2 tiles (`SPRITE_SIZE`, `:71`), so 1 art px = 2 game px.
- `bounce()`: a hit on a side face sets `(-x, abs(x) / 2)` (for `UP` gravity; the other directions are the same turned).
  A hit on a floor or ceiling after a shot down from a jump sets `(abs(y) / 2 * facing, -y)`. Then
  `temp.nor().scl(trajectory.len())` keeps the speed. So a bounce leaves at about 26.6 degrees from the face's axis
  (atan 0.5), or 63.4 degrees after a shot down. Both are not multiples of 45. After the first bounce, the angle is
  never a multiple of 90.
- Bounce limit `BOUNCE_MAX = 2`: the 3rd block or shield hit explodes the shot (`:181`).
- `explodeAndDie()` (`:236`) spawns `ChargedShotExplosion` with `ROTATION = trajectory.angleDeg()`.
- `spawnResidual()` (`:127`) spawns `ChargedShotResidual` with `ROTATION = trajectory.angleDeg()` at spawn time.

Spawners of `ChargedShot` and their trajectories (all axis-aligned today):

| Spawner | Trajectory |
|---|---|
| `MegamanWeaponFire.shootMegaBuster` (`:108-123`) | `BULLET_VEL` on one axis, by direction, facing and shot down |
| `PlayerBuster` via `MEGAMAN_X_BUSTER` (`players/PlayerBuster.kt:136-156`, `megamanx/MegamanXBuster.kt`) | horizontal speed turned by `direction.rotation` |
| `TestChargedShooter` (`entities/special/`, `:74`) | `(velocityX, 0)` |

### `ChargedShotExplosion` (`core/.../entities/explosions/ChargedShotExplosion.kt`)

- `rotation` from spawn prop `ROTATION`, default 0 (`:90`); `sprite.rotation = rotation` (`:142`).
- Art is directional: the arc of the blast is on the right side of the frame (it faces the travel direction).
  Regions `ChargedShotExplosion` and `HalfChargedShotExplosion` in `EXPLOSIONS_1`, 3 frames each, 32x32 per frame.
  Sprite size 1.75 tiles (full) or 1.25 tiles (half).
- Spawners: `ChargedShot.explodeAndDie()` (any angle after a bounce) and `EnemyShot` with
  `Death.CHARGED_SHOT_EXPLOSION` (`EnemyShot.kt:226`, `ROTATION = velocity.angleDeg()`). The only kind with that death
  is `ROLLING_BOT` (`EnemyShot.kt:136`), fired on the x axis (`RollingBot.kt:186`).
- The body and the fixtures are axis-aligned and do not use the rotation.

### `ChargedShotResidual` (`core/.../entities/decorations/ChargedShotResidual.kt`)

- `rotation = spawnProps.get(ROTATION)!!` (`:71`), drawn at `:91`. Spawned only from `ChargedShot.onSpawn`, before any
  bounce, so with today's spawners the angle is always a multiple of 90 (class C in the item `NOTES.md`).
- Regions `FullChargedShotResidual` (2x2 frames of 32 px) and `ChargedShot_Residual_Half` (2 frames) in
  `DECORATIONS_1`.

### Existing patterns

- `HomingMissile.kt:193,253,264`: snaps to 45, draws a `straight` or an `angled` region, and turns the angled region
  by `45 - angle` (a multiple of 90). This branch uses the same pattern.
- `CactusMissile.kt:183` and `WilyCapsuleTentacleScissor.kt:124` also round to 45 inline. No shared helper exists.

### Test maps

`TestChargedShooter` is used only in `RefactorTest079.tmx` (item `079`). No `RefactorTest183.tmx` exists.

## In scope

1. **Coerce the trajectory to 45-degree steps.** At spawn and after each bounce, round the angle of `trajectory` to the
   nearest multiple of 45 and keep the speed. The bounce formulas stay; their result is rounded. So the bounce off a
   side face leaves at 45 degrees (today about 26.6), and the bounce after a shot down leaves at 45 degrees (today about
   63.4).
2. **Diagonal regions for the shot.** New art: a diagonal frame strip for the full shot and for the half shot (2 frames
   each, the shot pointing up-right at 45 degrees). The sprite draws the straight region turned by the angle, or the
   diagonal region turned by `angle - 45`. Both turns are multiples of 90.
3. **The explosion.** It rounds its `ROTATION` to the nearest 45 and draws a straight or a diagonal region in the same
   way (D1 = a). New art: diagonal strips for the full and the half explosion (3 frames each).
4. **The residual.** The same rounding and region choice (D2 = b). New art: diagonal strips for the full residual
   (4 frames, 2x2) and the half residual (2 frames). No spawner reaches the diagonal path today; the owner chose the art
   now (see the objective check, `O1-R5`).
5. **Pack the new frames** into `Projectiles1`, `Explosions1` and `Decorations1` (`Basic` only, `assets.md`), and check
   by script that each old region keeps its name and pixels.
8. **Art** by one Fable art agent in this branch (D3 = a; owner confirmed the Fable use and this exception to "art in
   its own branch"). NES rules of `art.md`; a script checks the palette, the grid and the colour counts.
6. **Test map** `RefactorTest183.tmx` with `REFACTOR_TEST_183`, rooms for the checks below. Built by a subagent with
   the `level-builder` skill.
7. **Credits** in the root `README.md` for the new frames (`art.md`).

## Out of scope

- The other 26 class-B entity sprites of the item (`183c` and later), `MetHelmet` (its 45-degree spin is its own site),
  `BulletResidual`, and the gravity turn (`183a`).
- The body and fixture shapes of the shot and the explosion (axis-aligned, unchanged).
- A shared "snap to 45" helper for the other inline sites: this branch adds one function with its callers here; the
  other sites move to it in their own `183` branches.
- The guard against free-angle rotation (last `183` branch).

## Affected areas and risks

| Area | Files | Risk |
|---|---|---|
| Shot motion | `ChargedShot.kt` | Bounce paths change (intended). Gravity directions `LEFT`, `RIGHT`, `DOWN` take the same rounding. |
| Draw | `ChargedShot.kt`, `ChargedShotExplosion.kt`, `ChargedShotResidual.kt` | Wrong turn sign or region at one diagonal. A check per diagonal. |
| Atlases | `Projectiles1`, `Explosions1`, `Decorations1` (`.png`, `.atlas`) | A repack can move or rename old regions. Script check of old regions. |
| Art | new frame strips | NES rules (2 px grid where drawn 1:1, palette, 3 colors per 8x8 area). Script check. |
| Test map | `RefactorTest183.tmx`, `LevelDefinition.kt` | Load errors; checked by the review before the play test. |

Production files: `ChargedShot.kt`, `ChargedShotExplosion.kt`, `ChargedShotResidual.kt`, `LevelDefinition.kt`, 2
atlases (png + atlas each), new frame files, `README.md`.

## Overlap with other branches

Checked on 2026-11-23: the diffs of `compactor-level`, `fix/auto-perf-freeze`, `fix/projectiles-ignore-death`, `060k`,
`078e`, `078f` and `182a` against `master` touch none of the 3 classes, `EnemyShot`, `MegamanWeaponFire`,
`TestChargedShooter` or the `Projectiles1`, `Explosions1` and `Decorations1` frames. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O9` item goal (NES look, issue #4) | helps | 3 of the 30 free-angle sites go away. |
| `O4-R2` A test room for each mechanic | helps | `RefactorTest183` rooms for the bounce and the explosion. |
| `O1-R2` Components, not new inheritance | no effect | No new types. |
| `O1-R4` Typed state, not new property keys | no effect | Uses the existing `ROTATION` and `TRAJECTORY` keys. |
| `O1-R5` Code only with a production caller | no effect | The rounding function has 3 callers. The residual's diagonal region has no producer today; owner decision D2 (2026-11-23). |
| `O2-R2` Random values only from the seeded source | no effect | No random values. |
| `O2-R7` Gameplay does not depend on rendering | no effect | The trajectory rounding is gameplay; the region choice is draw only. |
| `O5-R1` No per-frame allocation | no effect | Pooled scratch vectors, as today. |
| `O6-R1` CI passes | no effect | |
| `O6-R2` Named checks for kept and changed behavior | helps | A unit test for the rounding and the bounce angles; manual checks. |
| `O8-R2` Capabilities, not character checks | no effect | X's buster and Megaman's buster share the shot. |

## Decisions (2026-11-23)

- D1 (owner accepted the recommendation): the explosion gets diagonal art and draws straight or diagonal.
- D2 (owner): the residual gets diagonal art too, not a fail-fast check.
- D3 (owner accepted the recommendation): the art is made in this branch by one Fable art agent. This confirms the
  Fable use for `183b`.

## Open questions

None.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with a new test of the trajectory rounding: bounce results for each gravity
  direction and push direction are multiples of 45, and the speed stays the same.
- [ ] `./gradlew core:smokeTest` passes (the new regions are found in `init()`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] Art check script: the new frames are on the NES palette and grid, and each old region of the 2 repacked atlases
  keeps its name and pixels.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Room names are final (step 1). Play scripts: `temp/play/play-1.sh` to `play-8.sh` in the worktree.

| # | Level and room | What to check |
|---|---|---|
| 1 | `REFACTOR_TEST_183`, `wall_bounce` | A full and a half shot from `TestChargedShooter` hit a wall and leave at 45 degrees, drawn with the diagonal region, pixels not turned. |
| 2 | `REFACTOR_TEST_183`, `third_bounce` | The 3rd hit explodes the shot; at a diagonal the explosion faces the travel direction, pixels not turned. |
| 3 | `REFACTOR_TEST_183`, `diagonal_hit` | A bounced shot hits an enemy on its diagonal path; the explosion draws as in check 2. |
| 4 | `REFACTOR_TEST_183`, `shoot_down` | Megaman shoots down from a jump onto the floor; the shot leaves at 45 degrees. |
| 5 | `REFACTOR_TEST_183`, `gravity_left` | The same bounce under a turned gravity: 45 degrees, right diagonal region. |
| 6 | `REACTOR_MAN`, `room1` (walk right) | Its shot still ends in the half explosion, facing its travel direction (control). |
| 7 | `INTRO_STAGE`, `room1`; `X6_INTRO`, `room1` (X) | Megaman's and X's charged shots, the residual and the explosion on the axis look as today (control). |

## Behaviors to check

- A shot on the axis looks and moves as today: same region, same turn, same residual.
- Charged shot damage and the bounce limit do not change.

## Intended changes

- The bounce angle of a charged shot changes from about 26.6 or 63.4 degrees to 45 degrees.
- A charged shot, its explosion and its residual on a diagonal draw a drawn diagonal region, not a turned sprite.
