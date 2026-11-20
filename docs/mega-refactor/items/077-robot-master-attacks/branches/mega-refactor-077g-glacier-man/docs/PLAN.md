# Plan: mega-refactor-077g-glacier-man

How to do the work in `SCOPE.md` (approved 2026-11-19). It needs no approval. When the plan changes, write the
reason in the process log.

Rules for every step: `docs/CODE_STYLE.md`; `docs/ways-of-working/gameplay.md` (fast play rounds, no new tests or
Gradle in a round, `docs/TEST-DEBT.md` until the moveset is final); no game launch. Frame data: `077f`
`docs/NOTES.md` "Final frame list". Model code: `DesertMan` and `desertman/DesertManTuning.kt` (`077b`),
`ElecDevil` (`077c`), `OrbAim`.

Subagent groups (one subagent per group; the main session checks each group before the next): A = steps 1;
B = steps 2 to 4; C = steps 5 to 7; D = steps 8 and 9.

## Steps

### 1. Test rooms

- Changes: `RefactorTest077.tmx`: rooms `glacier_man` (player spawn) and `glacier_man_boss` (a copy of the
  `GlacierMan_16x14_v2.tmx` `boss_room`: blocks, painted `tiles`, the `GlacierMan` spawn with `wall_1`/`wall_2`, the
  boss gate as in the `elec_devil` rooms). `level-builder` skill, subagent. The `elec_devil` rooms stay as they are.
- Check: `level-analyzer` lists both rooms; the spawn is inside `glacier_man`; object references resolve; free
  object ids.
- Done: yes (2026-11-19)

### 2. Phases, tuning rows, damage tables

- Changes: package `bosses/glacierman/` with `GlacierManTuning` (rows normal, normal phase 2, hard, hard phase 2);
  `GlacierMan.healthPhases = 2` and a second bar region; boss damage 2 × `master` (D4) for `BOSS_DMG_NEG` and the 4
  overrides; `PlayerDamageNegotiator` entries for `GlacierMan`, `Snowhead` (owner Glacier Man), `SmallIceCube` (owner
  Glacier Man) and `FallingIcicle` (owner Glacier Man) at 3 (D2), the `else` keeping other owners.
- Check: compiles in the next round; damage values listed in `TEST-DEBT.md`.
- Done: yes (2026-11-19, not compiled yet)

### 3. Move choice, chains, tells, timing

- Changes: a weighted pick from `STAND` (weights from the tuning row, distance, height, last move; no move 3 times in
  a row; `services.random`); chains sled → jump over → shoot back and duck → snowball → sled; a tell pose before each
  attack (`brake_flash`, `fall_flash` for fast moves); random stand and shot times from the row. Remove the per-call
  `gdxArrayOf` and `Vector2()` allocations in the touched code.
- Check: owner play round.
- Done: yes (2026-11-19, code; play round open)

### 4. Throws and snowball aim

- Changes: `chunk`, `aim`, `aim_up` animations; the cube spawns at the release frame (frame 2) at the strip's spawn
  point (mirrored for right facing); a volley shuffles `OrbAim` `LEAD`, `CURRENT`, `NEAR`, `WIDE`; some snowballs aim
  at the player.
- Check: owner play round.
- Done: yes (2026-11-19, code; play round open)

### 5. `FallingIcicle` form and timed drop

- Changes: a `FORM` state (`form` strip, 4 × 0.15 s, then `still`) and a timed drop (form, still, shake, fall on a
  delay) when the spawn props ask for it. With no new props, the `STILL` → player-under → `SHAKE` path is unchanged.
- Check: unit test that the default path (`GapingFish`, `IcicleTelly`) is unchanged (written in the final pass;
  `TEST-DEBT.md` line now).
- Done: yes (2026-11-19, code; play round open)

### 6. Icicle Rain and the double rain

- Changes: phase-2 move: `pound` (impact frame 2, `SHAKE_CAM`); a wave of icicles along the ceiling line of the room
  (map property or a block scan, chosen here and written in `NOTES.md`); one icicle over the player's lead position;
  one safe gap per wave; no column skip for enemies (D1). Under 25 % of the phase-2 health: 2 waves (D3).
- Check: owner play round; safe-gap rule in `TEST-DEBT.md`.
- Done: yes (2026-11-19, code; play round open)

### 7. Sled Ram

- Changes: phase-2 move: a fast sled across the arena, a bounce at the wall, snowballs both ways.
- Check: owner play round.
- Done: yes (2026-11-19, code; play round open)

### 8. Phase-2 start

- Changes: `onHealthPhaseStart(2)`: `ice_blast_attack` pose with a flash, a sound, `SHAKE_CAM`; the tuning row
  switches.
- Check: owner play round.
- Done: yes (2026-11-19, code; play round open)

### 9. Freeze-and-shatter death

- Changes: `freeze` (5 × 0.15 s) → `frozen_crack` (0.25, 0.25, 0.1 s) → `frozen_shatter` (3 × 0.1 s) with
  `IceShard` pieces at frame 0 from (32, 22); then the `AbstractBoss` explosions and defeat flow. The defeat time
  covers the sequence.
- Check: owner play round, both real levels.
- Done: yes (2026-11-19, code; play round open)

### 10. Play rounds

- Changes: owner play rounds on the contract checks; fixes made directly (`gameplay.md`).
- Check: the owner says the moveset is final.
- Done: yes (2026-11-20, owner: "new art and behaviors are ready to merge")

### 11. Final pass

- Changes: fight tests on a `GlacierManRig` (the `077b` rig pattern) from each `TEST-DEBT.md` line; the
  `FallingIcicle` default-path test; batched mutation checks (`git.md`); `entity-tags.txt` if a tag changes.
- Check: all automated checks of the contract.
- Done: yes (2026-11-20)

### 12. Review before the play test

- Changes: one Sonnet review agent (diff, contract, no launch); fixes; process log; Diffity.
- Check: the rules in `docs/mega-refactor/CLAUDE.md` "Review before the play test".
- Done: yes (2026-11-20)
