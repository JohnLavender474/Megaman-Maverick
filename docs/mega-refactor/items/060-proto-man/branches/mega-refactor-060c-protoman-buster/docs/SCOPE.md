# Scope: mega-refactor-060c-protoman-buster

- Status: approved (scope change 5; art round 35, code done 2026-11-14)
- Approved (scope change 5): 2026-11-14
- Approved (scope change 4): 2026-11-13
- Approved (scope change 3): 2026-11-13
- Approved (scope change 2): 2026-11-13
- Approved (scope change): 2026-11-12
- Approved: 2026-11-11 (the first scope, below "Scope change")

## Scope change 5 (2026-11-14, owner, at play test 7)

Where this section differs from "Scope change 4", this section wins. Art: round 35 (Fable 5.2 agent).

- **Shield strapped on his back** in every pose that is not a guard pose (stand, run, jump, shoot, sabre): behind
  the torso, hands free. The guard pull-out starts from it.
- **Crouch attacks.** With `DOWN` held on the ground, `B` attacks from the crouch. The attack drops the guard: the
  shield goes on his back, he stays crouched (crouch damage box), and when the attack ends with `DOWN` still held,
  the pull-out runs again.
  - Sabre slot: one low horizontal slash with its own arc, half-charge damage, no combo; `B` after it ends starts
    another.
  - Buster slot: the standing shot (same buster, cooldown and damage), fired at crouch height, in a `crouch_shoot`
    pose.
- `UP`+`B` (buster slot single slash) is unchanged.

Owner decisions (2026-11-14):

- With `DOWN` held on the ground (crouch guard, pull-out or hold), `B` attacks from the crouch. The shield fixture is
  off and the shield is drawn on his back; he keeps the crouch `BODY` and `DAMAGEABLE` boxes for the whole attack.
  When the attack ends with `DOWN` still held, the full pull-out runs again. If `DOWN` is released during the attack,
  he stands when it ends (or at once, if simpler and consistent; Claude's choice in `NOTES.md`).
- Sabre slot: one low slash (`crouch_slash` and its arc), half-charge damage, no combo chain, no movement (as a floor
  slash).
- Buster slot: the standing shot (same `ProtoBuster`, cooldown, damage) from the `crouch_shoot` muzzle point, in the
  `crouch_shoot` pose (hold time as `stand_shoot`). Left and right while crouched only turn him, as in the guard.
- The air guard, X and Megaman are unchanged. The back-shield art needs no code beyond the atlas.
- Test room `shield`: no enemy shoots at him when he spawns, and he can walk to each `SniperJoe`; the ledge block
  matches its painted tiles, with the left `SniperJoe` on top; its shots still pass over the crouch and hit the stand.

## Scope change 4 (2026-11-13, owner, at play test 6 / Diffity round 4)

Where this section differs from "Scope change 3", this section wins. Art: round 34, the final set of rounds 29 to 34
(`temp/mega-refactor-060c-protoman-buster/sabre/v2/guard/round34/`, Fable 5.2 agent); the code sizes follow its
`notes.md` values.

- **Crouch guard.** On the ground, holding `DOWN` makes him crouch and pull out the shield (crouch stance after X6
  Zero). His damage box gets shorter while he crouches, so shots at head height pass over him. The air guard stays a
  jump pose.
- **One shield size.** The front shield is shorter, near the size of the back shield; the back shield is slightly
  bigger on every strip that shows it. The `SHIELD` fixture follows the new front shield (it covers the crouched body;
  in the air it leaves part of the jump body open).
- **Transitions.** Pull-out: 4 frames on the ground, 3 in the air. New put-away frames (front to back), ground and
  air. The shield fixture still drops at once on the `DOWN` release; `B`, `A` and movement cut the put-away animation
  short.
- Unchanged from scope change 3: `DOWN` + `A` is the slide (`060d`), the guard breaks, `B` blocked while guarding.

## Scope change 3 (2026-11-13, owner, at play test 5)

Diffity round 3 closed with 0 threads. Where this section differs from "Scope change 2", this section wins.

- **Held guard on `DOWN`.** The shield is no longer passive.
  - It is up only while the player holds `DOWN`, on the ground and in the air.
  - A guard on the ground plants his feet: left and right only turn him. In the air, he keeps his jump arc.
  - No guard on a ladder: `DOWN` climbs down there (`060d`).
  - The guard still drops on an attack, a hit, a stun, a freeze, a teleport and a room transition. While he guards,
    `B` does not attack.
- **`DOWN` + `A` on the ground** will be the ground slide, built in `060d`. Until then, `A` does nothing while `DOWN`
  is held on the ground, with a `TODO` that names `060d`.
- **Full-body shield.** The shield art and the `SHIELD` fixture cover his whole body height on his facing side.
  - The free arm holds it and is drawn fully: upper arm, forearm and fist.
- **Pull-out frames.** 1 or 2 frames show him bring the shield out before the guard pose, on the ground and in the
  air.
  - The shield fixture turns on when the guard pose starts. The pull-out takes the place of the 0.2 s guard delay.
- **HUD icon** under the health bar, not beside it.
- Unchanged from scope change 2: the deflect spark and its rule, the slots, the buster, the damage.

## Scope change 2 (2026-11-13, owner)

The play test of the sabre art led to a design review. A read-only subagent did a neutral review of the moveset: the
likely complaints of players, the strengths and the alternatives. The owner took 5 points from it: `UP`+`B` shares
`UP` with ladders and is hard to find; there is no shot in the air; the 0.3 s shot pose locks him in place; the
damage is too high; the shield needs feedback. Where this section differs from "Scope change", this section wins.

Owner answers (2026-11-13):

- **Shield:**
  - It is passive, in the free (non-sabre) arm, and blocks projectiles from the front.
  - It turns on when he stands still or jumps without attacking, about 0.2 s after a run, an attack or a hit ends.
  - Separate guard poses, `guard_stand` (2 frames) and `guard_jump`, show it. The plain `stand` and `jump` stay.
  - Running, an attack, a hit or a stun drops it. Contact with an enemy body still hurts.
  - It uses the existing `SHIELD` contact rule: each projectile keeps its own `hitShield` reaction, and lasers stop.
  - A deflect plays a block sound and shows a spark.
  - The work stays in `060c`.
- **Weapon slots replace `UP`+`B`:**
  - Sabre and Proto Buster are his first 2 weapon slots. `B` uses the current slot in every state.
  - `SELECT` switches the slot, as Mega Man switches weapons. The special weapons (`060h`) join the same list.
- **Shoot while moving:**
  - The buster fires on the ground, while he runs and in the air, as Mega Man's does.
  - The 0.3 s pose lock goes.
  - The run-shoot and jump-shoot frames come back from the v2 art removed in step 10, and are checked against the
    current style.
- **Damage:**
  - Combo hits 1 and 2 and the air slash do half-charge damage (3, 10, 15).
  - Combo hit 3 does full-charge damage (5, 15, 30).
  - Shields still do not block the sabre.
  - The Proto Buster stays at half charge.

Owner answers, second round (2026-11-13):

- **The buster slot plays almost like Mega Man's buster:**
  - It fires on the ground, while he runs, in the air and on a ladder, with no pose lock and no charge.
  - Its pellets keep today's half-charge damage.
  - `ProtomanValues.SHOT_POSE_TIME` becomes a short shoot-frame time, as Mega Man's, not a lock.
- **One sabre slash on `UP`+`B` while the buster slot is equipped, as X's.** With the sabre slot, `UP`+`B` is the
  same as `B`.
- **Ladder rule:** on a ladder, `B` is always the buster, for every character.
  - Proto Man has no ladder code yet, so this is a docs rule and a `TODO` for his climb (`060d`).
  - The same rule for X goes to `081g` as a note: a backlog candidate here, and a line in `NOTES.md`, "Overlap with
    other branches".
- **Slot display:** a HUD icon next to the health bar, for Proto Man's slot.
  - New item `153` (on `master`, `e13419951`) makes the icon show for every player, and makes the HUD fade while a
    player is behind it.
  - `060c` adds only Proto Man's 2 icons and the HUD hook that draws them.
- **Switch sound:** `SoundAsset.SELECT_PING_SOUND`.
- **Deflect spark:** the existing `WhiteBurst` decoration, at the contact point. The bounce already plays
  `DINK_SOUND` in `Bullet.bounce`.

Facts from the scan (2026-11-13):

- **Weapon switching** is Mega Man only (`Megaman.setToNextWeapon`, `MegamanWeaponsHandler`). `PlayerSettings`
  `selectButtonAction` is shared by every player. Proto Man switches on `SELECT` when it is `NEXT_WEAPON`, as Mega Man
  does.
- **HUD:** `PlayerStatsHandler` draws only the health bar for a player that is not Mega Man. The pause screen shows Mega
  Man's grid for every player (an existing gap, `TODO` item `024`), and it stays out of scope.
- **The removed shoot frames** `run_shoot`, `run_trans_shoot` and `jump_shoot` are in `ebcffebb6`.
- **`PlayerBuster`** can already shoot while running or in the air. Proto Man's ground-only rule is in his own
  `canShootStanding`.

Damage per move: the class-keyed tables (`docs/class-entries.md`) read a per-move strength from `PlayerSabreSlash`, as
they read `ChargedShot.fullyCharged`. X's moves are all full strength, so X does not change. Item `141` owns a fuller
rule.

## Scope change (2026-11-12, owner)

At the first play test the Proto Strike was too strong (0.5 s charge, 1 hit kills a `PicketJoe`). The owner changed
the item: Proto Man plays like X6 Zero, and `060f` (the sabre) folds into this branch. The item `ITEM.md` has the
decision. The sections below "Scope change" describe the first scope; where they differ, this section wins.

Owner answers (2026-11-12):

- `B` is the sabre. On the ground: a 3-slash combo, as Zero in X4 to X6 (at least one new frame drawn by hand). In each
  other state: 1 slash. The arc is purple.
- `UP`+`B` on the ground is the buster: one fixed shot, no charge. He stands still to shoot: left and right do nothing,
  and a jump cancels the shot. Ground only. Where the shot is not available (air, slide, wall slide), `B` is always
  the sabre, also with `UP` held.
- The shot is weaker than a full charge and stronger than a pellet.

### What goes

- `ProtoStrike` (class, art, `entity-tags.txt` line, its entries in 26 files), the charge for Proto Man (charge
  component, `stopCharging` calls, the charge sound), the gold charge frames, and the `run_shoot`, `run_trans_shoot`
  and `jump_shoot` frames.
- The nullable half time of `playerChargeComponent` (round 1): no caller passes `null` any more.
- `PlayerBuster`, `PlayerCharge`, `playerCanAttack` and the X move to `players/` stay: X and Megaman call them.

### Scan (2026-11-12)

- X's sabre: `MegamanXSabre` (137 lines) holds the slash state (kind, time, frame, the kind change in the middle of a
  slash, `timeIn`) and X's rules (`canStart`: not air dashing, not kick-locked, not wall-grabbing; `kindNow`: wall,
  floor, air). `MegamanXSabreSlash` (199 lines) is the damager entity: arc art from the X atlas, hit boxes per kind and
  frame, placed from the owner's body. Both are typed to `MegamanX`.
- **Classic enemies take no damage from the sabre today.** `StandardDamageNegotiator` (the default table, by size)
  has no slash entry, so only 5 classes with their own entries can be hurt: `D1000`, `D1000Core`, `MetaWheelF`,
  `Junkroid`, `HoverGunner`, plus `SabreBlock`. A new damager class in the tables touches about 26 files (the
  `ProtoStrike` pass).
- Damage today, by target size (large / medium / small): pellet 3 / 5 / 15, half charge 3 / 10 / 15, full charge
  5 / 15 / 30.
- Sheet: the row "FLAME SWORD/TENGU BLADE" has the slash poses with no effect (ground and air); "TENGU BLADE" has thin
  white crescent arcs; "FLAME SWORD" has the same poses with flame arcs. The poses hold a sword hilt that needs an
  edit for a sabre.

### Plan of the change (details in `PLAN.md` after the decisions)

1. Remove what goes (above).
2. The buster: `UP`+`B` on the ground fires one shot through `PlayerBuster`; the shot pose locks left and right; `A`
   ends the pose (B1).
3. Share the sabre: `PlayerSabre` and `PlayerSabreSlash` in `players/`, with per-character data (moves, frame times,
   arc art and size, hit boxes, start rule, move chooser, combo). X passes his 3 kinds (floor, air, wall) and plays
   the same. Proto Man passes 3 floor moves (a combo: a `B` press during a slash chains the next) and 1 air move.
4. Damage: the slash entries (B2, B3).
5. Art: body frames from the sheet with the hilt edited, purple arcs recoloured from the Tengu Blade arcs, the new
   third floor slash by hand; a contact sheet for the owner before the atlas is packed.
6. Test map: a `sabre` room and a changed `buster` room in `RefactorTest060`.
7. Tests, the review before the play test, play test and Diffity.

### New APIs and their first callers

- `PlayerSabre`, `PlayerSabreSlash` and the sabre data type: X and Proto Man.
- The combo rule in `PlayerSabre`: Proto Man (X has 1 floor move, so his combo list has 1 entry).

### Objective check (change)

- `O8-R1` (players share code, thin classes): helps; the sabre is shared, not copied.
- Always-on "keep behavior": X's sabre plays the same, except where B2 gives the slash new targets.

### Brief decisions (owner answers 2026-11-12, all as recommended)

- B1: the shot spawns at once; the shot pose then holds him for the pose time (left and right do nothing); a jump
  ends the pose early.
- B2: one shared sabre: `PlayerSabre` (state; the floor moves are a list, X 1 and Proto Man 3, so a combo is data)
  and one damager class `PlayerSabreSlash` (renamed from `MegamanXSabreSlash`). A state change in the middle of a
  combo ends the combo and turns the move into the air slash, as X's floor slash does today.
  - Changed 2026-11-12 (owner): every state has a move list, not only the floor (a LibGDX `Array<SabreMove>` per
    state), so the combo rule is the same for each state. Today only Proto Man's floor list has more than 1 move. A
    state change in the middle of a combo ends the combo and turns the move into the first move of the new state. X also gets the B3
  entries, so his sabre now hurts classic enemies too.
- B3: a slash on a classic enemy does the half-charge damage, by size: large 3, medium 10, small 15.
  - Changed 2026-11-12 (owner, play test 1: the sabre was too weak and too short): the full-charge damage, large 5,
    medium 15, small 30 (and the full-charge value of each other table). The hit box covers the blade and the whole
    sweep. X's reach (about 51 art px), a visible blade, and uneven frame times (art round 6).
- B4: the buster shot (`ProtoBuster`) does the half-charge damage: large 3, medium 10, small 15.

## Goal

Proto Man shoots: the Proto Buster shot (`ProtoBuster`), the charge (full only), the Proto Strike (`ProtoStrike`, his
full charged shot) and the shoot and charge poses (stand, step, run, jump), in the v2 style. He calls one shared
buster with X; he copies no Megaman or X code.

## Item

`060` (Proto Man), `ITEM.md`:

- Phase `060c`: "Buster: shoot poses, Proto Buster, charge and Proto Strike".
- Decisions (2026-11-10, owner): a thin `Protoman` class that calls Megaman's shared behaviors and components, with his
  own values file, sprites, shield and Proto Strike; MM9/MM10 rules first; `Protoman` extends `MegaPlayer` (`060j`).
- `060b` `docs/NOTES.md` (owner, 2026-11-11): each new Proto Man frame needs the v2 restyle (the `Megaman_v3`
  physique) before it joins `Protoman_v2.txt`.

Research: `temp/mega-refactor-060c-protoman-buster/scan-code.md` (buster, charge, shots, damage keys, test targets),
`scan-sprites.md` (cells, regions, v2 plan; `preview_shoot_draft_x4.png`, `contact_sheet_x4.png`), `web/` (Fandom
wikitext of `Proto_Man` and `Proto_Buster`).

## Current state

Paths under `core/src/main/kotlin/com/megaman/maverick/game/`. Base: `master` `8b8989468`.

- Megaman shoots on the release of `B`, also an uncharged shot, with no cap on the screen (0.1 s cooldown). His shot
  code is in `MegamanWeaponsHandler` (`024` owns it). His charge flash is atlas frames (`_charge_half`,
  `_charge_full`).
- X has his own buster class (`megamanx/MegamanXBuster.kt`): press shoots, hold charges, release fires a half or full
  shot; a cap (4, and 2 full), a wait timer and a pose timer. It spawns the same `Bullet` and `ChargedShot` as
  Megaman. X copies 3 of Megaman's charge functions (`MegamanXController.kt:69-89`). His charge flash is a code tint
  (`MegamanX.kt:383-391`).
- Nothing of the buster or the charge is in `players/`.
- `Protoman` has no buster, no shoot keys, and his atlas `Protoman_v2.txt` has 4 regions (`stand`, `run_trans`, `run`,
  `jump`).
- The damage tables key on the exact shot class (`StandardDamageNegotiator`, 17 files with class keys, 11 `is` lines
  in 7 files). `Bullet` has a look `type`; `ChargedShot` has none.
- MM9/MM10 (Fandom, `Proto_Man` "Mega Man 9"; `Proto_Buster`): Proto Man has 2 shots on the screen (Mega Man 3); he
  charges; his charged shot looks like Mega Man's MM4 charged shot. Inference, not in the source: press shoots, hold
  charges, release fires the charged shot (the MM4 to MM10 rule).
- Sheet: the shoot poses are cells `#20` (stand), `#23`-`#25` (walk w1 w2 w3), `#26` (jump). No step-shoot pose and no
  charge glow. The shots: pellet 8x6, small comet 16x10 (2 frames), Proto Strike 4 frames up to 39x24. In each shoot
  cell the shield is on his back.
- `REFACTOR_TEST_060` has no enemies. `SniperJoe` and `PicketJoe` (`MEDIUM`) take 5, 10 and 15 from the 3 shot levels
  and have no `Megaman::class` code.

## In scope

- `players/PlayerBuster.kt`: X's buster moves here with parameters: the fire rule (X: press; Proto Man: release, D1),
  the shot cap (X: 4 and 2 full; Proto Man: none, D1), the wait times (Proto Man: Megaman's 0.1 s cooldown), the
  pose times, the speeds, the spawn offset function, the charge levels (X: half and full; Proto Man: full only, D2),
  and a function that spawns the shot of each level (X: `Bullet` and `ChargedShot`; Proto Man: `ProtoBuster` and
  `ProtoStrike`, D3). Callers: `MegamanX`, `Protoman`.
- `players/PlayerCharge.kt`: the charge component builder (half and full time), `chargeStatus` (with a `chargeable`
  gate) and `stopCharging()`. Callers: `Megaman`, `MegamanX`, `Protoman`. Megaman keeps his charge input, his weapon
  gate and his shot rule.
- `canAttack` as a `MegaPlayer` rule if the 2 callers (X, Proto) have the same checks; else it stays per class.
- `Protoman`: the buster on `B`: hold charges; release fires a `ProtoBuster` before the full time and a `ProtoStrike`
  after it (D1, D2). Shoot and charge keys. `ProtomanValues` buster numbers: Megaman's speed (12), cooldown (0.1 s),
  pose time (0.3 s) and full time (1.5 s); the charge sound at Megaman's half time (0.75 s). Changed 2026-11-12
  (owner, in the play test): full time 0.5 s; no half time (`playerChargeComponent` takes a nullable half time),
  so the charge sound starts at the full time, when the Strike is ready.
- New classes in `entities/projectiles/` (D3):
  - `ProtoBuster`: the pellet (sheet 8x6). Rules as `Bullet` for a player shot (dies on a block or a hit, bounces off a
    shield once).
  - `ProtoStrike`: the Proto Strike (sheet 4 frames, up to 39x24). It explodes on the first hit, also when the hit
    kills (D3b), with the full `ChargedShotExplosion`. On a shield it explodes too and does no damage.
  - Explicit entries in each table, override map, set and light map that names `Bullet` or `ChargedShot` for a player
    shot: `ProtoBuster` with `Bullet`'s value, `ProtoStrike` with the full `ChargedShot` value (D3a). The `is Bullet`
    and `is ChargedShot` checks get the new class where the check is about a player shot. 68 lines in 35 files name
    the 2 classes; lines that are about enemy bullets stay. Each line is decided in `PLAN.md`.
  - `entity-tags.txt` lines; regions in `Projectiles1` (or a new atlas).
- Art (D4): the v2 shoot frames (`stand_shoot`, `run_trans_shoot`, `run_shoot` x4, `jump_shoot`; 6 unique frames) by
  the `060b` method, and the `_charge_full` frames as recolours of his frames (as Megaman's, 0 new shapes; stand,
  run, jump), in draft rounds for the owner, who picks the colours.
- `RefactorTest060.tmx`: a new room `buster` with `SniperJoe` and `PicketJoe`.
- Tests: the buster through `InputFrame` in `ProtomanTest` (release fires, full charge, cooldown, no cap); `MegamanXTest`
  "the buster" stays green; a damage test that `ProtoBuster` and `ProtoStrike` hurt a `MEDIUM` target as `Bullet` and
  the full `ChargedShot` do; a `ProtoStrike` test that it explodes on a kill.
- Docs: `players/README.md` (the buster and the charge).

Claude's calls:

- Stand shoot from `#20` (1 frame, the idle legs). The arm: the `Megaman_v3` cannon repainted in his red.
- The step shoot uses the v3 `run_trans_shoot` body (the sheet has none).
- No shoot up or down (the sheet has only diagonals; `060g`).
- The shot sounds: Megaman's bullet sound and his charged shot sound (`TODO` if he needs his own).
- The damage values start as Megaman's; `060g` tunes them.

## Out of scope

- Megaman's buster and weapons (`024`). His shot rule and his charge frames do not change.
- The `Met` shield rule (`EntityQueries.shooting` reads Megaman only): backlog candidate.
- Slide, ladder and climb shoot (`060d`), shield and damage reaction (`060e`), the Proto Sabre (`060f`), the rapid and
  aim sets on the sheet and the damage tuning (`060g`), weapons (`060h`).
- The half charge and the small comet (D2).

## Affected areas and risks

- X's buster moves: the X control checks (shoot, charge, cap, sabre on the same button) must play the same.
  `MegamanXTest` "the buster" (9 tests) guards it.
- Megaman's charge functions move: Megaman's charge, its sound and its stop points must play the same.
- New shot classes: a table that misses `ProtoBuster` or `ProtoStrike` gives 0 damage. A test lists each class-keyed
  file and checks that the new class is there where `Bullet` or `ChargedShot` is (for player shots).
- New regions in `Protoman_v2.txt` and `Projectiles1.txt`: a missing region fails the smoke test.
- `ChargedShot` look: X and Megaman keep their look.

## Overlap with other branches

- `mega-refactor-081f-x6-intro-fixes`: changes X files (`MegamanXSprites.kt`, `MegamanXTest.kt`, X's animations) and
  conflicts with `master` already. This branch moves X's buster and charge code. The `081f` diff does not touch
  `MegamanXBuster.kt` or `MegamanXController.kt` (to check again at merge).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` | helps | The buster and the charge move to `players/`; X and Proto Man call them; no copy |
| `O8-R2` | no effect | No character checks added; `Met` rule is a backlog candidate |
| `O8-R3` | waiting | Single player until `060i` |
| `O8-R4` | waiting | Megaman's weapons wait for `024`; Proto Man has the buster only |
| `O1-R2` | no effect | No new layer |
| `O1-R4` | no effect | New classes; no new string keys |
| `O1-R5` | helps | Each new API has a caller: `PlayerBuster` (X, Proto), `PlayerCharge` (3 classes), `ProtoBuster` and `ProtoStrike` (Proto) |
| `O2-R5` | no effect | Slot 0 only |
| `O4-R2` | helps | A test room for the buster |
| `O6-R2` | helps | Named checks and buster tests |
| `O9-R1` | no effect | `B` shoots, as for X |

## Decisions

Owner answers to the brief (2026-11-11), none as recommended:

- D1: every shot fires on the release of `B`, as Megaman in this game; no cap (Megaman's 0.1 s cooldown).
- D2: full charge only: the Proto Strike. No half shot.
- D3: new classes `ProtoBuster` and `ProtoStrike`.
  - D3a: explicit entries in each table, set and light map (no stand-in rule).
  - D3b: the Proto Strike always explodes on the first hit, also when the hit kills.
- D4: the charge shows with recoloured frames, as Megaman's; the owner picks the colours in a draft.
- Owner note: these answers are a first try from the gut; plan to refine or pivot. So the fire rule, the cap and the
  charge levels are `PlayerBuster` values (a pivot is a value change), and the class-table entries (D3a) are a
  separate plan step after the buster works.

## Open questions

See the brief.
