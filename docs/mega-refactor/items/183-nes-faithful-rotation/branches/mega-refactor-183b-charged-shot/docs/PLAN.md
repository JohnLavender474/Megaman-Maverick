# Plan: mega-refactor-183b-charged-shot

How to do the work in `SCOPE.md` (approved 2026-11-23). When the plan changes, write the reason in the process log.

New regions (names are proposals; step 2 fixes them). Each diagonal strip has the same frame size, frame count and
layout as its straight strip, and the object points up-right (45 degrees, LibGDX angle):

| Straight region | Diagonal region | Atlas | Frames |
|---|---|---|---|
| `ChargedShot_v2` | `ChargedShot_v2_diagonal` | `PROJECTILES_1` | 2 (1x2) |
| `HalfChargedShot` | `HalfChargedShot_diagonal` | `PROJECTILES_1` | 2 (1x2) |
| `ChargedShotExplosion` | `ChargedShotExplosion_diagonal` | `EXPLOSIONS_1` | 3 (1x3) |
| `HalfChargedShotExplosion` | `HalfChargedShotExplosion_diagonal` | `EXPLOSIONS_1` | 3 (1x3) |
| `FullChargedShotResidual` | `FullChargedShotResidual_diagonal` | `DECORATIONS_1` | 4 (2x2) |
| `ChargedShot_Residual_Half` | `ChargedShot_Residual_Half_diagonal` | `DECORATIONS_1` | 2 (1x2) |

New API and its callers: one engine extension, `Float.roundAngleToStep(step: Float)` in
`engine/.../common/extensions/FloatExtensions.kt`. It rounds an angle in degrees to the nearest multiple of `step` and
wraps the result into `[0, 360)` (so 350 gives 0, not 360). It first wraps the input into `[0, 360)`, so a
negative angle works. It fails fast unless `step > 0` and `step` divides 360 (for example 22.5, 30, 45, 90); with a
step such as 50, the angles near 360 would round to the wrong neighbour. Pure math with no state, so a top-level function is safe;
it is generic, so it goes in `engine`. Callers in this branch: `ChargedShot` (spawn, bounce, draw),
`ChargedShotExplosion` and `ChargedShotResidual`. Tests in `FloatExtensionsTest`. The 3 inline copies
(`HomingMissile.kt:193`, `CactusMissile.kt:183`, `WilyCapsuleTentacleScissor.kt:124`) stay as they are and get a
`TODO` that points to their `183` branch, which moves them to the function.

## Steps

### 1. Test map `RefactorTest183.tmx`

- Changes: subagent with the `level-builder` skill. New `assets/tiled_maps/tmx/RefactorTest183.tmx` and the
  `LevelDefinition` entry `REFACTOR_TEST_183` (type `TEST_LEVEL`). Rooms, each with a player spawn and painted
  `tiles`: `wall_bounce` (a full and a half `TestChargedShooter` fire at a wall), `third_bounce` (a closed pocket where a
  shot hits 3 times), `diagonal_hit` (an enemy on the 45-degree path after a wall bounce), `shoot_down` (a ledge to
  jump from and shoot down at the floor), `gravity_left` (a turned gravity, with a wall and a shooter or room for
  Megaman's own shot). Find a real level room with `RollingBot` for control check 6, and a real stage room for check 7.
- Check: a render of tiles and blocks per room; the review before the play test checks the load.
- Done: yes

### 2. Diagonal art (Fable art agent)

- Changes: one Fable art agent (Claude Fable 5.2) draws the 6 diagonal strips in `temp/mega-refactor-183b-charged-shot/art/`,
  from the straight strips as the source. Rules from `art.md`: NES palette, single art px (these frames draw at 2 game
  px per art px or more), at most 3 colours plus transparent per 8x8 NES-px area, stepped edges, no turned pixels,
  same frame centre as the straight strip. Contact sheet with the straight and the diagonal strips at the 4 turns.
- Check: palette and colour-count script; the owner looks at the contact sheet (one round per fresh agent).
- Done: yes

### 3. Pack the atlases

- Changes: copy the approved strips to `assets/sprites/frames/Projectiles1/`, `Explosions1/`, `Decorations1/`. Repack
  the 3 `.tps` projects with `Basic`.
- Check: a script compares each old region (name and pixels) before and after; `core:smokeTest`.
- Done: yes

### 4. Trajectory rounding in `ChargedShot`

- Changes: `Float.roundAngleToStep` with tests, and a `TODO` at the 3 inline copies; `ChargedShot.onSpawn` rounds the spawn trajectory; `bounce()` rounds its result and
  keeps the speed. Unit test in `core/src/test/`: each gravity direction x push direction x shot-down case gives an angle
  that is a multiple of 45, with the same length; axis spawn trajectories stay unchanged.
- Check: `./gradlew engine:test core:test`.
- Done: yes

### 5. Region choice in the 3 classes

- Changes: `ChargedShot`, `ChargedShotExplosion`, `ChargedShotResidual` choose the straight or the diagonal animation
  from the rounded angle, and turn by `angle` or `angle - 45`. The explosion and the residual round their `ROTATION`
  prop to 45.
- Check: `./gradlew core:test core:smokeTest lwjgl3:jar`.
- Done: yes

### 6. Credits

- Changes: root `README.md` `CREDITS`: the diagonal charged shot, explosion and residual frames, made by Claude Fable
  5.2 directed by the developer, based on the existing charged shot art (source of the straight art to be read from
  the README or asked).
- Check: read the section.
- Done: yes

### 7. Checks and handover

- Changes: all automated checks of the contract; the review before the play test (one Sonnet agent); play scripts in
  `temp/play/`; Diffity review.
- Check: the contract.
- Done: yes (handover; play test open)
