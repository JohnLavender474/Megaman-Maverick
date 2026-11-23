# Process log: mega-refactor-078g-timber-walker

## Status

- State: active
- Goal: Timber Walker mini boss, first pass: the fight from the round-5 mockup (with the owner's R1 to R3 answers) in a test room of `RefactorTest078`
- Done: branch start; first pass built (round-6 art, test map `RefactorTest078`); play round 1; round-2 code; art round 7 swapped in
- Next: the owner's play round 2
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-22

- Branch start. Owner: "Go ahead and implement a first-pass of the boss fight, and put it into a test room in a test
  level". Owner answers on the walker mockup page (https://claude.ai/artifact/37YRaAp6NdsPLCKaJeFYY8), round 5:
  R1 B (saw reach 8 tiles, a third section), R2 C (both thrust heights, 2 aim poses), R3 B (the claws also grab and
  throw Megaman).
- The walker is stage content of `078` (Timber Woman redesign, part 1), but not in the art-only scope of `078f`, so
  it gets its own branch. Merge target `master`, as the other `078` branches.
- Planning: no brief. The owner asked for a first pass to play, so the branch follows
  `docs/ways-of-working/gameplay.md` (play rounds first, tests and the review when the design is final).

## 2026-11-22 (first pass, built)

Built: `TimberWalker` (`entities/bosses/`, with `bosses/timberwalker/TimberWalkerAnchors.kt` and the generated
`TimberWalkerFrames.kt`), `TimberWalkerLog` and `TimberWalkerChipWave` (`entities/projectiles/`), the atlas
`TimberWalker` (`assets/sprites/frames/TimberWalker/`, `texture_packer/TimberWalker.tps`, `TextureAsset.TIMBER_WALKER`),
`MegaLockReason.BOSS_GRAB`, the `FightPlans.find` entry (no music, the map gives `mini=true`), 3 entries in
`PlayerDamageNegotiator`, `RefactorTest078.tmx` and `LevelDefinition.REFACTOR_TEST_078`.

Art: round 6 of the art agent (`temp/078-timber-walker-miniboss/round6/` in the main checkout). The assets come from
`temp/078g-timber-walker/build_walker_assets.py` (worktree) and the game spec `temp/078g-timber-walker/animations_game.tsv`.
Rebuild (from the worktree `temp/078g-timber-walker/`):
`python3 build_walker_assets.py --frames /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round6/frames --anchors /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round6/anchors.tsv`.
It writes the frames, `TimberWalkerFrames.kt` and packs the atlas with the `TexturePacker` CLI.

Decisions made without the owner:

- One region per unique frame (not strips): the engine `Animation` takes a frame list, so an animation is a list of
  frame names with durations. A `~` step (the turn) flips the sprite in code; no mirrored region.
- The `.tps` is a copy of `Bosses4.tps` (Basic) with `autodetectAnimations` off, so that `idle_1` stays a region name
  and does not become `idle` with an index.
- The game animation spec is its own file (`animations_game.tsv`); it takes the round-6 timings, with these changes:
  `stuck_open` held 1.5 s (round 6: 0.9 s), so that the weak point can take more than 1 hit per chop; the `turn` at
  0.62 s (round 6: 1.07 s); the hop split into crouch, air (held until the landing) and land; the log throw ends
  without the idle tail; a `saw_recover` (stuck_2, recover, idle_1) after `HURT`; a `grab_miss` (the reach in
  reverse); the defeat wreck held 2.1 s so that the animation fills the 3 s defeat timer.
- The walker moves on its own: an `ABSTRACT` body at a bottom-center point, walk and hop in code, x clamped to the
  arena; the floor is the spawn bounds bottom (the landing point).
- Fixtures from the anchors: the hull box from the cab bottom down (round 6's hull box also covers the cab), 2 leg
  boxes of 20 px at the hull ends from the hull bottom to the floor, all `SHIELD` and `DAMAGER`; the cab `SHIELD` or
  `DAMAGEABLE`; the saw and claw `DAMAGER`. No `BODY` fixture, so that shots do not stop on the empty space under the
  hull.
- The grab uses the `SpiderWeb` pattern (`MOVEMENT` and `BEHAVIORS` locks, gravity off), with a new lock reason
  `BOSS_GRAB`. The claw catches on the `grab_3` and `grab_hold` frames. The throw calls `takeDamageFrom` directly
  (as `CameraViewEdges` does), then sets the throw velocity.
- Damage: all walker-body damagers (contact, saw, claw, throw) share the `TimberWalker` tag, so all do 4; log 3;
  chip wave 2.
- The log break is one harmless frame; no separate log pieces (`TODO` in `TimberWalkerLog`).
- Defeat: the `defeat` animation, explosions for the first 1 s, no fade; the Met run-away is not built (`TODO` in
  `TimberWalker`); no explosion orbs (the mini boss default).
- Picks: a player behind turns the walker; 2 attacks max between chops; high player (2.5 tiles) gets `CLAW_HIGH` or
  `LOG_THROW`; a low player 4.5 to 7.5 tiles away gets `CLAW_GRAB` at 40 %; 7 tiles or more gets one of the 2 thrusts
  or the log (not the last attack again); nearer gets `SAW_CHOP` or the hop at 50 %. `SAW_CHOP` and `CLAW_HIGH` walk
  first when the player is more than 1 tile past their reach.
- Sounds: `QUAKE_SOUND` at both landings, `MM2_MECHA_DRAGON_SOUND` roar, `MM3_ELECTRIC_SAW_SOUND` thrust,
  `POUND_SOUND` chop, `WHOOSH_SOUND` log, `THUMP_SOUND` log break, `CHOMP_SOUND` grab, `WHIP_SOUND` throw.
- Test map: 2 rooms of 16 x 14 on `Tileset1.tsx` tile 175; the gate wall block is the walker's `left_wall`, as the
  gate pattern of `ReactorMan_v3.tmx` and `RefactorTest077.tmx` (the wall block runs to the floor over the gate).
  The high platform is 5 tiles up at the right wall (reach it with a wall jump). `far_start` is in the layer
  `walker_points` (no builder), 6 tiles up at the right side, because the platform holds the lower spot.
- Round-5 stand-ins were built first (`temp/078g-timber-walker/prepare_r5.py`), then replaced by round 6 when it
  was done; nothing of round 5 is in the branch.

Checks: `./gradlew core:compileKotlin core:smokeTest` passed; `./gradlew core:test --tests 'com.megaman.maverick.game.levels.*'`
passed. No game launch. No unit tests (play rounds first); the rules are in `docs/TEST-DEBT.md`.
- Play round 1, owner: "the gate's not opening". Cause: the entry gate of `walker_gate` had `boss_key=1` (and `mini_boss`), so it stayed locked until the walker was defeated. Fix: the two properties removed from the entry gate.
- Owner: the gate `room` property must name the room the gate leads to (`timber_walker`), not the room it is in. The first fix (boss key) was not the cause of the closed gate. Fixed.
- Real cause of the closed gate: `Gate` puts its `GATE` fixture on the `direction` side of its body (`DirectionPositionMapper`), so `direction=right` put it inside the wall block, out of reach from the left. The entry gates of `ReactorMan_v3.tmx` name the side the player comes from (gate 194: `right`, player from `room6`; gate 198: default `left`). Fix: `direction` removed (default `left`).

## 2026-11-22 (level-builder gates)

- Tooling change on the owner's request (out of the walker scope): `level-builder` gets `add-gate` and `add-boss-room`, with the 3 gate rules of play round 1 as checks; `utils/level_builder/run_tests.sh` passes. A rebuild of the 2 `RefactorTest078` rooms with the new commands gives the same gate, room and boss-wiring properties as the fixed map.

## 2026-11-22 (play round 2: code, map, stand-in atlas)

Owner notes of play round 1 and the decisions: `docs/SCOPE.md`, "Owner notes, play round 1". Art round 7 (the larger
walker and the new frames) is drawn in parallel by another agent; this round uses round-6 frames as stand-ins.

Decisions made without the owner:

- The walker faces left for the whole fight; the fight `TURN` is gone (with the back at the right wall, a turn
  would point the arms into the wall). The intro keeps the turn: `far_start` moved to the left side (x 672), so the
  walker walks right in the background, lands facing right and turns before the roar (review finding 5).
- Home = `right_wall` left edge minus the rear extent, read from the frames: the largest opaque right edge of the
  `idle` and `walk` frames (`rightX` in `TimberWalkerFrames`). `minX` uses the front hull edge of the same frames.
- Guard count: a `ChargedShot` counts 2 for a half and a full charge. A shot counts when its owner chain reaches a
  player (`rootPlayer`). The hit sound is `ENEMY_DAMAGE_SOUND`; open and close play `BOSS_DOOR_SOUND`.
- 4 hits during an attack: the guard opens when the attack ends, not at once (an interrupt would snap the frames);
  at once in `WALK`; at the end of a `TELL` the attack is dropped.
- Tell sounds: `REV_SOUND` for the levers, `BUTTON_SOUND` for the claw button.
- `met_hit` (frame `hit_flash`, 0.1 s) is an extra row for the hit in the stun; the stun timer runs on.
- Hop: the target is the player's x at the crouch start (the shadow shows it from then), clamped before lift-off
  (review finding 8). Apex 4 tiles of foot clearance, air time 1.6 s (vy0 10 tiles/s, gravity 12.5 tiles/s2). Feet
  above 1.75 tiles for 1.2 s; a run at 6 tiles/s plus the walker's own x speed covers more than the walker width.
  Only the hop toward the player makes chip waves; the hop home lands with a shake only.
- Shadow: a stand-in region `hop_shadow` (72 x 8 px, black at 47 %) drawn at the hop target on the floor. It is not
  in the art contract; keep the stand-in or ask the art agent for one.
- Picks: a player under the hull gets the hop or the log; the high platform moved to the left side
  (x 576..672, 5 tiles up), because the walker now holds the right side.
- Arena: the `timber_walker` interior is 12 tiles (y 32..416). The ceiling block over the arena is now y 0..32 (new
  block 13), the walls 6 and 7 grow 2 tiles up to meet it, the gate-room ceiling (block 4) ends at x 512. The room
  top is the ceiling bottom, so no ceiling tile is in view in the arena. Edits made with `level-builder`
  (`translate-width`, `add-block`, `translate-height`, `translate-x`); the tile layer was changed by a small script
  (the CLI cannot paint).
- Smoothness review (`temp/078g-timber-walker/review-smoothness.md`): finding 1 `setIndex(0)` in `setAnimation`
  (engine `reset()` left as a bug candidate in `docs/TODO.md`); 2 a per-frame foot line `footY` (the anchors column
  `foot_y` when the TSV has one, else the lowest opaque row, at most `floor_y`; only `jump` differs today: 104), used
  for the sprite and the fixtures; 3 the hold point is the grip of the current frame, eased at 25 tiles/s, and the
  throw puts the player at the `grab_throw` grip first; 4 a `damaged_` copy of each row with `idle_1`, written by the
  generator and picked in code below half health; 5 see the intro turn above; 8 the hop target clamp; 6 and 7 are
  art (round 7); 9 is a bug candidate in `docs/TODO.md`.
- Tells follow the first aim pose of each attack as stand-ins, so the attack rows no longer start with `idle_1` and
  the aim pose (no flash back to idle between the tell and the attack).
- `WALK` back home plays the forward walk while moving backward (`docs/TODO.md`).
- Body: its size follows the hull of the current frame (it has no fixture of its own).

Assets: `build_walker_assets.py` got `--extra-frames` (stand-ins for frames that the frame folder lacks),
`--canvas-frame` (the canvas size comes from that frame, default `idle_1`), `--body-x` (default 220), `footY` and
`rightX` per frame, and the `damaged_` rows. The stand-in overlays are in `temp/078g-timber-walker/standins_round2/`.
Rebuild:
`python3 build_walker_assets.py --frames /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round6/frames --anchors /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round6/anchors.tsv --extra-frames standins_round2`.
A script check after the repack: every region of the atlas equals its frame PNG, with offset 0; the 66 old frames
keep their names and pixels; 5 regions are new.

Swap to art round 7:

1. Copy the round-7 rows of `round7/animations.tsv` into `temp/078g-timber-walker/animations_game.tsv` (same
   columns), in place of the stand-in rows: `tell_*`, `guard_open`, `met_stunned`, `met_recover`, `guard_close`,
   `hop_air` (frame `hop_tuck`), `guard_crack_1..3`, `guard_hit`, and `met_hit` if round 7 draws its own hit frame.
   If round 7 redraws the attacks so that they start from their aim pose, put that pose back at the start of
   `saw_low`, `saw_level`, `attack_saw` (`attack_windup`), `claw_extend` (`claw_ext_1`) and `grab_reach`.
2. Run the build with the round-7 frames and anchors: `--frames <round7>/frames --anchors <round7>/anchors.tsv
   --extra-frames standins_round2` (the last option only while a frame, for example `hop_shadow`, is still a
   stand-in). If the canvas or the pivot changes, pass `--canvas-frame` and `--body-x`. If the anchors TSV has a
   `foot_y` column, it is used.
3. No code change is needed for the size: home, `minX`, the body, the fixtures, the sprite placement and the hop
   clearance read the generated frame data. Check `LEG_WIDTH_PX` (20 art px) against the new legs, and the reach
   constants (`CHOP_REACH`, `CLAW_HIGH_REACH`, `GRAB_*`) against the new arm lengths.
4. The overlays draw at the cab box of the current frame, scaled to the box; any size works.

Not done: no Gradle run, no game launch, no tests (play round).

## 2026-11-22 (play round 2: art round 7 swapped in)

Art round 7 of the art agent (`temp/078-timber-walker-miniboss/round7/` in the main checkout; notes in the "Round 7"
section of its `NOTES.md`). `animations_game.tsv` is now the round-7 `animations.tsv` (the art rows win, also the 17
`damaged_` rows) plus 5 rows that only export the overlay and shadow regions. The generator adds `damaged_` copies
only for `roar_start`, `roar_end` and `turn` (never played: low health is a fight state). The stand-ins and
`--extra-frames` are no longer used. Rebuild (from the worktree `temp/078g-timber-walker/`):
`python3 build_walker_assets.py --frames /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round7/frames --anchors /home/johnlavender/IdeaProjects/Megaman-Maverick/temp/078-timber-walker-miniboss/round7/anchors.tsv`.
Script checks: all 109 atlas regions equal their frame PNG with offset 0; every frame of every animation is a
region; every region and animation name in the walker code exists.

Decisions made without the owner (code changes beyond the data swap):

- Placement: the sprite, the hit boxes, the grip and the overlays use the floor line `FLOOR_Y` again, not the foot
  line. With round 7 the hull of `hop_tuck` keeps the place of `jump` and only the feet rise, so a foot-line
  placement would drop the hull 1.75 tiles at the tuck. `footY` is used for the leg boxes (they end at it) and for
  the tuck gap.
- Hop: `hop_air` is `jump` 90 ms, then `hop_tuck`. While descending, the frame changes to `jump` when the floor line
  is less than the tuck gap (1.75 tiles, read from the frames) above the floor. The floor line rises 3 tiles
  (`HOP_RISE`, was a 4-tile apex clearance); air time 1.6 s (start speed 7.5 tiles/s up, gravity 9.4 tiles/s2).
  Gap under the feet at the apex: 4.75 tiles. Feet above 1.75 tiles from 0.09 s to 1.32 s (1.23 s); the walker top
  at the apex is 10.6 tiles up (arena 12).
- Intro roar: `roar_start`, then `roar` for 1.2 s, then `roar_end`; the fight starts after `roar_end`.
- Reaches are now from the pivot, measured on round 7 (the hull front is 2.4 tiles in front of the pivot):
  `CHOP_REACH` 7.5, `CLAW_HIGH_REACH` 7.9, grab 6.5 to 9 tiles. Pick zones for a low player: under 6 tiles the
  thrusts or the hop (the chop does not reach that close), 6 to 8.5 the chop or the hop, 8.5 or more the thrusts
  or the log.
- Legs: `LEG_WIDTH_PX` 30 (round 7 legs are 30 to 32 px wide); the rear leg box ends at the frame's `rightX` (the
  rear legs stand 11 px past the hull end).
- Hull: a second hull shield and damager behind the cab (x from the cab end to the hull end, y from the hull top to
  the cab bottom). The round-7 hull box holds the cab, and this area had no box, so shots passed through it.
- Home: the rear extent uses the `idle` frames only (55 px). With the `walk` frames (64 px, the walk_1 rear foot),
  home stood 0.6 tiles off the wall. The walk legs can then overlap the wall tile by up to 9 px while walking home.
- Tells end on `idle_1` and the attack rows start on `idle_1` (art rows), so no aim pose was put back.
- `met_recover` is 0.89 s in the art row (asked: 0.6 s); kept as drawn.

## 2026-11-23 — merge as a first pass (owner)

- Owner: merge the branch now; the mini boss will be refined and expanded with an artist, and its integration into
  the game is a TODO (backlog candidate in `docs/TODO.md`, a new item on `master`).
- Not done before the merge, by this owner decision: the tests from `docs/TEST-DEBT.md`, mutation checks, the review
  before the play test, and a passed owner play test of round 2. The walker spawns only in `RefactorTest078`.
- Checks after the `master` merge into the branch: `engine:test core:test core:smokeTest lwjgl3:jar detektMain
  core:verifyRandomRule` pass. Diffity: opened, 0 threads.
