# Contract: mega-refactor-060l-protoman-one-input

- Status: approved
- Approved: 2026-11-15

## Intended changes

- Proto Man has no weapon slots. On the ground and in the air, `B` is the buster (the standing shot, the jump shot)
  and `UP`+`B` is the sabre (ground: the 3-slash floor combo, D2; air: one air slash). Later `B` presses during a slash chain it, as
  today.
- `DOWN`+`B` on the ground is the crouch shot (D1). The crouch slash is gone.
- In a slide: `B` is the slide shot; `UP`+`B` ends the slide with the floor combo (D2) (nothing under a low ceiling).
- On a ladder: `B` and `UP`+`B` are the ladder shot (the `060c` rule, unchanged).
- `SELECT` does nothing for Proto Man with `selectButtonAction` `NEXT_WEAPON` (no ping). With `START`, it pauses, as
  today.
- The HUD shows no slot icon for Proto Man. The `Hud` atlas is gone (D3).
- Scope change 1 (2026-11-16): the sabre does 10 (normal) and 12 (full) to a `Size.LARGE` enemy on the standard
  damage table (was 4 and 5): `Robbit`, `BigJumpingJoe`, `PreciousTron`. One floor combo kills such an enemy. X's
  sabre gets the same values (it uses the same `PlayerSabreSlash`).

## Must not change

- The sabre and buster rules: damage (except the sabre on large enemies, scope change 1), shot speed and offsets, combo timing, poses, the guard and its breaks, the
  slide and ladder rules of `060d`, the slide-to-crouch delay.
- Megaman's and X's input and HUD.

## Automated checks

- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`.
- `ProtomanTest`: each case that used a slot is kept with the new input (`B` for the buster, `UP`+`B` for the
  sabre), or replaced by a named case; the slot cases go; new cases: `B` on the ground and in the air is the buster;
  `UP`+`B` on the ground and in the air is the sabre; `SELECT` changes nothing; `DOWN`+`B` is the crouch shot (D1); `UP`+`B` in a slide
  starts the combo (D2); `UP`+`B` at a ladder foot (same frame: the combo; `UP` held first: the grab, then the ladder shot).
- Scope change 1: `ProtomanTest` and `ProtoShotDamageTest` pin the `LARGE` sabre values 10 and 12; the new case
  "Robbit, a large standard enemy, takes 10 from a half hit and 12 from a full hit: one floor combo kills it".
- `HudIconsSmokeTest` is deleted with the `Hud` atlas.
- Each new case is seen to fail on a mutation of its rule.

## Manual checks

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-060l-protoman-one-input`.

Experimental:

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room sabre`: `UP`+`B` slashes (the 3-slash combo on the ground, one air slash in
  a jump), `B` during a slash chains; `B` alone shoots, also in a jump; no HUD slot icon; `SELECT` does nothing.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room shield`: `DOWN`+`B` is the crouch shot, never a slash; the guard as
  before.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room slide`: `B` in a slide is the slide shot; `UP`+`B` ends the
  slide with the floor combo; under the low ceiling `UP`+`B` does nothing.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room ladder`: while climbing, `B` and `UP`+`B` shoot; at the
  ladder foot, `UP` held first grabs the ladder and then `B` shoots; `UP`+`B` in the same frame starts the floor combo.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room sabre` (scope change 1): the Robbit dies in at most 3 slashes
  (one floor combo with `UP`+`B`, `B`, `B`), and in at most 3 air slashes (a slash that stays on it can hit twice).

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room buster`: the shot, its pose and its damage as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat`: X's buster, sabre and HUD as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room sabre` (scope change 1): the Metal Wheel F (`MetaWheelF`,
  own sabre entry) takes 9 per X slash hit, as before (at most 4 slashes).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: Megaman shoots and switches weapons with
  `SELECT`, HUD as before.
