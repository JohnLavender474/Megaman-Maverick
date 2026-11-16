# Process log: mega-refactor-060l-protoman-one-input

## Status

- State: merged
- Brief: https://claude.ai/artifact/H5H6rjZEUCSDq8xxHnfVet
- Goal: One input in place of Proto Man's 2 weapon slots: `B` is the buster, `UP`+`B` is the sabre, as X
- Done: planning (scan, `SCOPE.md` and `CONTRACT.md` approved 2026-11-15, `PLAN.md`); `PLAN.md` steps 1-5; step 6 (review)
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-15

- Branch start. Merge target: `master` (item decision). Owner decision of 2026-11-15 in the item `ITEM.md`: X's input,
  `B` the buster, `UP`+`B` the sabre; `ProtomanWeapon`, the 2 HUD slot icons and the slot branches go. 2 open
  questions in `ITEM.md` (`DOWN`+`B`; the combo on `UP`+`B`).

## 2026-11-15 (planning)

- Scan: `ProtomanWeapon`, the `B` actuator, `crouchAttack`, `slideAttack`, `SELECT`, `PlayerStatsHandler`,
  `HudIconsSmokeTest`, about 30 slot switches in `ProtomanTest`. Overlap: `024a` changes `PlayerStatsHandler.kt` in
  other hunks.
- Brief published: https://claude.ai/artifact/H5H6rjZEUCSDq8xxHnfVet. Owner answers: D1 the crouch shot; D2 the
  3-slash combo on `UP`+`B`; D3 delete the icons. Follow-up: the `Hud` atlas holds only the 2 icons, so the whole
  atlas goes (owner). `SCOPE.md` and `CONTRACT.md` approved 2026-11-15; `PLAN.md` written (6 steps).

## 2026-11-15 (steps 1-5)

- Steps 1-2 (`29e04fe21`): the `B` actuator of `SCOPE.md` item 1 in `Protoman.kt`: `sabre.active` chains,
  `climbing` the ladder shot, `sliding` `slideAttack()` (`B` the slide shot, `UP`+`B` the floor combo, nothing
  under a low ceiling), `crouchAttackWanted()` the crouch shot (D1), `guardWanted()` nothing, `UP` the sabre (floor
  combo or the one air slash), else the standing or jump shot. `ProtomanWeapon.kt`, `weapon`, `selectNextWeapon()`
  and the `SELECT` actuator go; one `TODO` for `060h` at the controller component. Plan change: the HUD icon code
  in `PlayerStatsHandler.kt` and `HudIconsSmokeTest.kt` went in this commit, not in step 4, because both read
  `ProtomanWeapon`. `ProtomanTest` rewritten; the case map is in `docs/NOTES.md`.
- Step 3 (`e38070ba9`): `SabreState.CROUCH`, `ProtomanSabreMoves.CROUCH` and its `ALL` entry, the move list and the
  state rule branch go; `PlayerSabre.start` loses `state` and `chains` (no caller left). The `crouch_slash` frames
  stay in `Protoman_v2`; `ProtomanGuardStripsSmokeTest` still checks them.
- Step 4 (`4ed5ac3bb`): `TextureAsset.HUD`, `Hud.txt`, `Hud.png` and `assets/sprites/frames/Hud/` go.
  `assets/assets.txt` is git-ignored; the build wrote it with no `Hud` line.
- Step 5: `players/README.md` (the sabre states, `start`, the slot and HUD sentences), `docs/NOTES.md` (decisions,
  test case map), this entry, `PLAN.md`. Grep for `ProtomanWeapon`, `weapon slot`, `SabreState.CROUCH` and
  `TextureAsset.HUD` in code and READMEs: no hit.
- `UP`+`B` in front of a ladder: the slash wins (`ControllerSystem` runs before `BehaviorsSystem`,
  `MegamanMaverickGame.kt:988-989`; the climb is `blocked = { sabre.active }`). Pinned by a test.
- Checks: `engine:test core:test core:smokeTest lwjgl3:jar --rerun` green after step 4: engine 689, core 2102
  (`ProtomanTest` 167), smoke 467, 0 failures; the jar builds. Statically `ProtomanTest` went from 162 to 150 `it`
  blocks (the loops expand at run time).
- Mutation checks (2 runs of `core:test`, each mutation set chosen so that no two touch the same case; code restored
  with `git checkout` from the step 1-2 commit after each run, `git diff` empty):

| Mutation | New case | Failed |
|---|---|---|
| `SELECT` release plays the ping | does nothing on SELECT ... | yes (`pings()`) |
| `B` alone starts the sabre (`playerCanAttack` branch) | fires one pellet on B on the ground, with no slash | yes |
| same | fires the jump shot on B in the air, with no slash | yes |
| `UP`+`B` starts with `chains = false` | chains the floor slashes also on UP and B | yes |
| `UP` branch only on the ground | starts the one air slash on UP and B in the air ... | yes |
| `crouchAttack()` starts the sabre | fires the crouch shot, never a slash, ... | yes |
| same | ends the crouch shot pose on a jump after DOWN is released | yes |
| slide `UP`+`B` starts with `chains = false` | ends the slide and starts the floor combo on UP and B, and B chains it | yes |
| climb `blocked = { false }` | slashes, and does not grab, on UP and B ... in front of a ladder | yes (`climbing()`) |

## 2026-11-16 (play test, scope change 1)

- Owner, at `REFACTOR_TEST_060` `sabre`: the Robbit takes too many sabre slashes; raise the sabre damage on it and on
  all similar enemies. Measured: 7 slashes with the floor combo (4, 4, 5), 8 air slashes, 6 for X.
- Change: `StandardDamageNegotiator` `LARGE` row, `PlayerSabreSlash` 4/5 to 10/12 (normal/full), so one floor combo
  kills a 30-health large enemy. Affects `Robbit`, `BigJumpingJoe`, `PreciousTron`, and X's sabre. Not changed:
  `MetaWheelF` (own entry 6/9), `MetaDridler` (no damage), bosses, `MEDIUM`, `SMALL`, buster. `SCOPE.md` scope change
  1, `CONTRACT.md`, `NOTES.md` table.
- Tests: `ProtomanTest` (combo damage values, the air slash case name, new Robbit case) and `ProtoShotDamageTest`
  (`LARGE` 12). No Gradle run (play test in progress); the parent session runs them.

## 2026-11-15 (step 6)

- Review before the play test, 2 subagents (branch diff; the 7 manual checks), no game launch. No bug found. Every
  manual check is valid, its room loads and holds the setup; `REFACTOR_TEST_029` `enemy_aim` gives Megaman weapons
  through its `levelOverride`, so no `--weapons`.
- Fixed:
  - `ProtomanTest`: new case "grabs on UP held a frame before B in front of a ladder, then fires the ladder shot on
    B" (the second half of the ladder-foot rule had no case). Mutation (the `UP` branch before `climbing` in the `B`
    order): it fails, with 4 other cases. An unused import (`BehaviorsComponent`, older than the branch) removed.
  - `CONTRACT.md`: the ladder check and the ladder case name the ladder-foot rule.
- Checks with `--rerun`: engine 689, core 2103, smoke 467 tests, 0 failed; the jar builds.

- Checks (owner allowed unit tests during the play test): `core:test` 2104 tests, 0 failed. Mutation: the `LARGE`
  sabre row back to 4/5 fails the new `Robbit` case and the combo damage case.
- Owner: the multi-hit of one slash on one enemy (0.25 s hit box, 0.1 s damage time) stays as it is.
- Owner: ready to merge (the play test passes). Diffity round 1: 0 threads; the review is closed and scope change 1
  is committed.

## 2026-11-16 (merge)

- `master` merged into the branch (`0fed20d7f`; `024a`, `011a`). One simple conflict, the imports of
  `PlayerStatsHandler.kt`: `master`'s `WeaponDef` imports kept, the Proto Man imports dropped. Checks with `--rerun`:
  engine 698, core 2120, smoke 467 tests, 0 failed; the jar builds.
- Overlap: `024b` (active) adds `character` and `shooting` to `Protoman.kt` and rewrites the weapon bars in
  `PlayerStatsHandler.kt`; its approved `SCOPE.md` says the branch that merges second resolves it. `060l` merges first.
