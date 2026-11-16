# Notes: mega-refactor-060l-protoman-one-input

## Decisions

Made during steps 1-5 (2026-11-15), in the frame of `SCOPE.md`:

- The HUD icon code in `PlayerStatsHandler.kt` and `HudIconsSmokeTest.kt` go in the step 1-2 commit, not in step 4:
  both read `ProtomanWeapon`, so the step 2 code does not compile without the change. Step 4 removes the `Hud` atlas
  (`TextureAsset.HUD`, `Hud.txt`, `Hud.png`, `assets/sprites/frames/Hud/`).
- `crouchAttack()` keeps its name and is the crouch shot only. The private `crouchAttacking` goes; its readers read
  `crouchShooting`, which was the only part left.
- The `state` parameter of `PlayerSabre.start` has no caller after D1, so it goes; `start` always takes
  `params.chooseState()`. The `chains` parameter also has no caller left (the `060c` single slash with
  `chains = false` is gone with the buster slot; D2 makes every start a combo start), so it goes too. A list of 1
  move (the air slash) still has no chain, and a landing move still locks the chain.
- `assets/assets.txt` is git-ignored (`.gitignore:181`), so the build writes it with no `Hud` entry and there is no
  change to commit.
- The docs outside the branch folder that name the slots (`ITEMS.md`, `O8-characters-and-modes.md`, the root
  `process-log.md`, `153` `ITEM.md`, `060` `ITEM.md`) are `master` files; the merge updates them.
- `ProtomanGuardStripsSmokeTest` still checks the `crouch_slash` and `crouch_slash_arc` strips in `Protoman_v2`. The
  art stays in the atlas (D1), so the check stays; no code reads the regions now.
- `UP`+`B` in front of a ladder: the slash wins. The engine runs `ControllerSystem` before `BehaviorsSystem`
  (`MegamanMaverickGame.kt:988-989`), so the `B` actuator starts the floor combo in the frame of the press. The climb
  behavior then evaluates with `blocked = { sabre.active }` (`Protoman.kt`, `defineBehaviorsComponent`, read in
  `PlayerClimb.kt:107`), so he does not grab until the slash ends. When `UP` was held a frame before `B`, he grabs
  first, and `B` is the ladder shot. The case "slashes, and does not grab, on UP and B pressed in the same frame in
  front of a ladder" pins it.

## Test case map

Each `ProtomanTest` case that `060l` removes or rewrites, and the case that covers its rule now.

### Removed

| Old case | Rule now | Covered by |
|---|---|---|
| the weapon slots: starts with the sabre | no slot | gone; "does nothing on SELECT ..." |
| the weapon slots: switches sabre, buster, sabre on SELECT, with a ping each time | `SELECT` does nothing | "does nothing on SELECT: no ping, the slash goes on, and B stays the buster" |
| the weapon slots: does not switch on the SELECT press | same | same |
| the weapon slots: does not switch when SELECT has another action | same | same |
| the weapon slots: does not switch during a slash | same | same (`SELECT` in a slash) |
| the weapon slots: does not switch while damaged | no slot | gone |
| the weapon slots: goes back to the sabre at a spawn | no slot | gone |
| the sabre slot: starts the sabre on UP and B on the ground, with no shot | `UP`+`B` is the combo | the sabre: "starts the first floor slash on UP and B on the ground, with no shot" |
| crouch attacks: starts one crouch slash on B with DOWN held ... at half strength | D1: crouch shot | "fires the crouch shot, never a slash, on B with DOWN held, in the pull-out and in the guard" |
| crouch attacks: does not chain the crouch slash, and B after it ends starts another | D1 | "keeps the buster cooldown for the crouch shot" |
| crouch attacks: neither runs nor turns during the crouch slash | D1 (the shot lets him turn) | "lets left and right only turn him during the crouch shot" |
| crouch attacks: shows the crouch slash body frame | D1 | "fires the crouch shot, never a slash ..." (`crouch_shoot` key) |
| crouch attacks: ends the crouch slash on a jump after DOWN is released | D1 | "ends the crouch shot pose on a jump after DOWN is released" (new) |
| crouch attacks: does a crouch slash, not the UP slash, on UP, DOWN and B with the sabre slot | D1 | "fires a crouch shot, not the UP slash, on UP, DOWN and B on the ground" |
| crouch attacks: the `SABRE` half of the 3 guard loops and the 2 break loops (5 cases) | D1 | the `BUSTER` half, kept as single crouch shot cases |
| slide: ends the slide and starts the floor slash on B with the sabre slot and `null` held | `B` in a slide is the shot | "fires one shot from the slide offset facing RIGHT/LEFT on B, and the slide goes on" |
| slide: ends the slide and starts the floor slash on B with the sabre slot and `UP` held | D2 | "ends the slide and starts the floor combo on UP and B, and B chains it" |
| slide: does nothing on B with the sabre slot under a low ceiling | `B` is the shot | "fires the slide shot, not a crouch shot, on DOWN and B under a low ceiling" |
| slide: does no crouch attack on DOWN and B in a slide with the sabre slot under a low ceiling | same | same (holds `DOWN`, checks `groundslide_shoot`) |
| ladder: the `SABRE` half of "fires one shot from the climb offset ..." (3 cases) | `B` on a ladder is the shot | "fires one shot from the climb offset on B with null/UP/DOWN held, and holds still" |
| ladder: the `SABRE` half of "does no crouch attack on DOWN and B at the ladder foot" | same | "does no crouch attack on DOWN and B at the ladder foot" |
| ladder: changes the slot on SELECT, and B stays the buster | `SELECT` does nothing; `B` is the shot | "does nothing on SELECT ..."; the ladder shot cases |

### Rewritten

| Old case | Change |
|---|---|
| the buster slot (group) | Now "the buster"; no `SELECT` step before each case. |
| fires one pellet on B on the ground | Name adds "with no slash" (new case: `B` on the ground is the buster). |
| fires in the air | Now "fires the jump shot on B in the air, with no slash"; checks no slash and `jump_shoot` (new case). |
| starts one sabre slash on UP and B, with no shot and no chain | Now "starts the floor combo on UP and B on the ground, with no shot, and B chains it" (D2). |
| the sabre: every case that started a slash with `B` | Starts with `UP`+`B`; chains with `B`. |
| starts the air slash on B in the air, also with UP held | Now "starts the one air slash on UP and B in the air, with no shot and no chain" (new case). |
| the X6 sabre targets: `halfSlash`, `fullSlash` | Start with `UP`+`B`. |
| does not attack on B while DOWN is held in the air, with either slot | Now "... on B, or on UP and B, ..."; no `SELECT`. |
| crouch attacks: the crouch shot cases | No `SELECT` step. |
| the shield: is off during a slash / a shot | `UP`+`B` for the slash; no `SELECT` for the shot. |
| slide: does not start while a slash is active | `UP`+`B`. |
| slide: ends the slide and does the single slash on UP and B with the buster slot | Now the floor combo, and `B` chains it (D2, new case). |
| slide: does nothing on UP and B with the buster slot under a low ceiling | No slot in the name. |
| ladder: does not grab during a slash | `UP`+`B` for the air slash. |
| animation key: shows the shoot key of each pose | No `SELECT` step. |

### New

| Case | Rule |
|---|---|
| does nothing on SELECT: no ping, the slash goes on, and B stays the buster | `SELECT` does nothing |
| chains the floor slashes also on UP and B | D2: chain with or without `UP` |
| fires the crouch shot, never a slash, on B with DOWN held, in the pull-out and in the guard | D1 |
| ends the crouch shot pose on a jump after DOWN is released | D1 (replaces the crouch slash jump case) |
| slashes, and does not grab, on UP and B pressed in the same frame in front of a ladder | `UP`+`B` at a ladder: the slash wins |

## Scope change 1: sabre hits to kill a large enemy

All have 30 health. Proto Man's floor combo is normal, normal, full; his air slash is normal. Every X move is full.

| Enemy | Sabre entry | Proto floor combo before | Proto air before | X before | Proto floor combo after | Proto air after | X after |
|---|---|---|---|---|---|---|---|
| `Robbit` | `LARGE` row | 7 (4, 4, 5) | 8 (4) | 6 (5) | 3 (10, 10, 12) | 3 (10) | 3 (12) |
| `BigJumpingJoe` | `LARGE` row | 7 | 8 | 6 | 3 | 3 | 3 |
| `PreciousTron` | `LARGE` row | 7 | 8 | 6 | 3 | 3 | 3 |
| `MetaWheelF` | own (6, 9) | 5 | 5 | 4 | no change | no change | no change |
| `MetaDridler` | no damage | - | - | - | no change | no change | no change |

The counts assume one hit per slash. A slash keeps no record of the targets it hit, and its hit box is on for about
0.25 s, while a standard enemy is invincible for only 0.1 s after a hit (`AbstractHealthEntity.DEFAULT_DMG_DURATION`).
So a slash that stays on an enemy can hit it 2 or 3 times. This explains the owner's "like 4" slashes before (about 2
hits per slash), and after the change 2 slashes can be enough. Not changed here (item `141`).

For reference, unchanged: a `MEDIUM` enemy (12, 15) dies to one floor combo; a `SMALL` enemy (20, 30) to 1 or 2
slashes. Live rooms with these enemies and a sabre user: only `REFACTOR_TEST_060` `sabre` (`Robbit`);
`BigJumpingJoe` (`RodentMan`, `Gauntlet1_v2`) and `PreciousTron` (`PreciousWoman_v2`) are in Megaman levels today.

- 2026-11-16 (owner): one sabre slash can hit the same enemy 2 or 3 times (hit box about 0.25 s, enemy damage time
  0.1 s). Kept as it is; no issue filed.

## Overlap with other branches
