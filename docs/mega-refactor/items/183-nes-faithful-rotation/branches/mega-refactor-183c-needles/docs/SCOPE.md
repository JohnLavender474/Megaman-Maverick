# Scope: mega-refactor-183c-needles

- Status: approved
- Approved: 2026-11-24 (D1 a, D2 a, D3 a, D4 a; owner accepted the recommendations with `/go-with-recs`)

Small branch (at most about 5 production files, 1 owner decision beyond the recommendations): no HTML brief and
no `PLAN.md`; the steps are in "Steps" below.

## Goal

`Needle` draws only at turns that are multiples of 90 degrees (or a mirror). The needle keeps its motion: it still
points along its velocity, but the drawn angle snaps to 32 directions (11.25-degree steps), each from a drawn
frame. So the arc of a needle under gravity still looks smooth, with no rotated pixels.

## Item

`183` (NES-faithful rotation, GitHub issue #4). From `ITEM.md`, "Proposed scope":

| Branch | Content |
|---|---|
| `183d` and later | Spinning and aimed sites, a few per branch: snap each one to 8 or 16 directions with drawn regions; the count is set per entity |

This branch takes the next free letter, `c`. The owner asked for more directions than 16 for the needle
(2026-11-24, verbatim): "let's make the needles more faithful. Right now, the needles are shot out of Desert Man's
attacks and Mega Man's needle-spin attack. We need a lot of frames so that the current needles' movement continues
to look natural."

## Current state

Scan of `master` at `a5e61cdb0`.

- `Needle` (`core/.../entities/projectiles/Needle.kt`): `sprite.rotation = body.physics.velocity.angleDeg() + 270f`
  (`:223`), origin at the center. The art points up (angle 0 = up). With gravity, the angle changes every frame
  (class B, "follows velocity", in the item `NOTES.md`).
- Regions `Needle/default` and `Needle/ice` in `PROJECTILES_2` (`Projectiles2.tps`), 16x16 px each, drawn on 1 tile
  (32 game px), so 1 art px = 2 game px (one NES pixel). Loaded at `:85`.
- The art is mirror-symmetric about its long (vertical) axis: 7 columns wide, 8 rows tall, colors `000000` outline,
  `FCFCFC` core, and `FCA044` (default) or `C0F5FB` (ice) edge. `C0F5FB` is not on the NES palette.
- Spawners (all keep their code; the change is in `Needle` only):

  | Spawner | Angles at spawn | Gravity | Type |
  |---|---|---|---|
  | `DesertMan.spawnNeedles` (`:1100`) | 9 fixed angles, 0 to 90 to 270 (`NEEDLE_ANGLES`) | yes | default |
  | `DesertMan.spawnThrowNeedles` (`:1144`) | aimed at the player, spread | no | default |
  | `DesertMan.fireNeedleSpinBurst` (`:1226`) | `NEEDLE_SPIN_ANGLES`, 15-degree grid | yes | default |
  | `MegamanWeaponFire.fireNeedleSpin` (`:440`) | fixed angles turned by gravity direction | yes | default |
  | `Cactus.spawnNeedles` (`Cactus.kt:372`) | fixed angles | yes | default |
  | `SpikeBot` (`SpikeBot.kt:378`) | fixed angles | yes | default or ice |

## In scope

1. **Draw rule.** Each frame, `Needle` rounds the sprite angle (velocity angle + 270) to a multiple of 11.25
   (`Float.roundAngleToStep`, from `183b`). From the step index `k` (0 to 31): quadrant `q = k / 8`, `j = k % 8`.
   - `j` 0 to 4: region `j`, rotation `q * 90`, no flip.
   - `j` 5 to 7: region `8 - j`, flip X, rotation `q * 90 + 90`. The mirror of the frame at `t` is the frame at `-t`,
     and the 90-degree turn makes it `90 - t`. This works because the art is symmetric about its long axis.
2. **Art.** 4 new frames per type, the needle turned 11.25, 22.5, 33.75 and 45 degrees counterclockwise from up,
   drawn as pixel art (D1 = a). Region names `Needle/default_step1` to `default_step4` and `Needle/ice_step1` to `ice_step4` (a name that ends in `_<n>` is an animation index for TexturePacker); the
   existing `default` and `ice` are step 0. The ice frames are a color map of the default frames.
3. **Ice palette** (D2 = a). The ice edge color `C0F5FB` becomes `A4E4FC` (NES) in the new frames and in the existing
   `ice` frame, so a needle does not change shade when it turns.
4. **Pack** the new frames into `Projectiles2` (`Basic` only, `assets.md`). A script checks that each other region
   keeps its name and pixels.
5. **Test.** The draw rule is the `internal` class `NeedleDrawDirection` in `Needle.kt` (caller: the `Needle` sprite pre-process), so that a test can read the frame index: the needle regions are static and another spec (`DesertManNeedleSpinTest`) can load them first. A unit test of the draw rule: for each of the 32 steps, the region index, the flip and the rotation; and
   that the motion (velocity) is not changed.
6. **Credits** in the root `README.md` for the new frames (`art.md`).

## Out of scope

- The motion of the needle, its body, its damage and its spawners: no change.
- The other class-B sites of the item, and the gravity turn (`183a`).
- A shared draw helper for symmetric directional frames: one caller here. If a later `183` branch needs the same
  rule, it moves the rule to a shared home then.
- A new test map room (D4 = a): every spawner is in a live level (see the contract).

## Affected areas and risks

| Area | Files | Risk |
|---|---|---|
| Draw | `Needle.kt` | A wrong sign or mirror at one octant. The unit test covers all 32 steps. |
| Atlas | `Projectiles2.png`, `Projectiles2.txt` | A repack can move or rename old regions. Script check. |
| Art | 8 new frames, 1 changed frame | NES rules: palette, single art px, 3 colors per 8x8 area. Script check. The 16x16 frame turns about its center (8, 8), so each new frame keeps the needle's center at the same place as the straight frame. |

Production files: `Needle.kt`, `Projectiles2.png`, `Projectiles2.txt`, the frame files, `README.md`.

## Overlap with other branches

Checked on 2026-11-24: the diffs of `compactor-level`, `fix/auto-perf-freeze`, `fix/projectiles-ignore-death`,
`060k` and `078f` against `master` touch none of `Needle.kt`, its spawners or the `Projectiles2` frames.
`compactor-level` touches only `docs/issues/README.md` of these paths. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O9` item goal (NES look, issue #4) | helps | 1 of the 30 free-angle sites goes away. |
| `O4-R2` A test room for each mechanic | no effect | No new mechanic; the draw shows in live levels (D4). |
| `O1-R5` Code only with a production caller | no effect | The draw rule has 1 caller (`Needle`). |
| `O2-R7` Gameplay does not depend on rendering | no effect | Draw only. |
| `O5-R1` No per-frame allocation | no effect | Owned fields, no new objects per frame. |
| `O6-R1` CI passes | no effect | |
| `O6-R2` Named checks for kept and changed behavior | helps | Unit test of the 32 steps. |

## Decisions (2026-11-24, owner accepted the recommendations with `/go-with-recs`)

- D1 a: 32 directions (11.25 degrees), not 16 (22.5) or 8 (45). The owner asked for "a lot of frames"; with the
  mirror, 32 directions need only 4 new frames per type.
- D2 a: the ice edge color goes to the NES palette (`A4E4FC`) in all ice frames.
- D3 a: art in this branch by one Fable art agent (as `183b`). The Fable use needs the owner's confirmation
  (`art.md`, temporary rule).
- D4 a: no new test room; play checks in live levels.

- 2026-11-24 (owner): the Fable use is confirmed; the straight needle frames are by the authors of the Megaman spritesheet (SamuelX4 and oldlavygenes), for the credit.
- 2026-11-24 (decision without the owner): the frames are packed before the owner's art review, so that the review happens with the play test.

## Open questions

None.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new test of the draw rule (32 steps).
- [ ] `./gradlew core:smokeTest` passes (the new regions are found in `init()`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] Art check script: the new frames are on the NES palette, with at most 3 colors per 8x8 area; each other region
  of `Projectiles2` keeps its name and pixels.
- [ ] The review before the play test finds that every manual check runs as written. No game launch.

## Manual playthrough

Play scripts: `temp/play/play-1.sh` to `play-5.sh` in the worktree.

| # | Level and room | What to check |
|---|---|---|
| 1 | `DESERT_MAN`, `room1` (walk right) | `Cactus` needles point along their arc and turn in small steps; no rotated pixels. |
| 2 | `DESERT_MAN`, `room1`, `--weapons needle_spin` | Mega Man's needle spin: the same as check 1. |
| 3 | `GLACIER_MAN`, `room1` (walk right) | Snow `SpikeBot` ice needles: the same, and the ice color reads as before. |
| 4 | `REFACTOR_TEST_183`, `gravity_left`, `--weapons needle_spin` | The needle spin under left gravity: the needles arc to the left and still point along their path. |
| 5 | `DESERT_MAN`, `room16` (walk right into the boss), `--heartTanks all --healthTanks all` | Desert Man's needle burst, thrown needles and needle-spin needles: the same as check 1. |

## Behaviors to check

- Needle paths, speed, damage and hits do not change.

## Intended changes

- A needle draws a drawn frame for each of 32 directions, not a sprite turned to any angle.
- The ice needle edge color changes from `C0F5FB` to `A4E4FC`.

## Steps

1. Branch start on `master`, worktree.
2. Art: one Fable agent draws the 4 default frames; the ice frames by color map; check script.
3. Owner review of the art.
4. Copy the frames to `assets/sprites/frames/Projectiles2/Needle/`, repack `Projectiles2`, compare script.
5. `Needle` draw rule and its unit test; mutation check.
6. README credit.
7. Automated checks, then the review before the play test (1 Sonnet agent).
