# Scope: mega-refactor-077b-desert-man

- Status: approved
- Approved: 2026-11-15 (owner, answers to the review; the brief has no open decision that blocks the work)

## Goal

A richer Desert Man fight on the shared boss parts of `077a`: 2 health phases, clear tells before every attack, a
shorter fight, a phase-2 burrow attack, and a fix for the gap in the long-punch arm. The same class is the Desert Man
stage boss and the `WILY_STAGE_1` mid-boss rematch; both get the new fight.

## Item

`077` (one branch per boss; a phased boss gets its harder attacks in its last phase in place of a panic attack). The
review of the current fight is in `temp/077b-desert-man/boss-review.md` (not committed; its findings are below).

## Owner answers (2026-11-15)

- Scope: ideas 1, 2, 3 and 5 of the review, plus the long-punch arm gap.
- The `WILY_STAGE_1` rematch gets the same fight.
- Damage to the player stays 3 from every damager, moved into `PlayerDamageNegotiator`, with a `TODO` to consider more
  varied damage per damager (item `077`).

## Current state (from the review and the scan)

- `entities/bosses/DesertMan.kt` (843 lines). States `INIT`, `STAND` (1 s), `PUNCH`, `TORNADO`, `DANCE`, `JUMP`,
  `WALL_SLIDE`; the engine guard `StateMachine` picks the first true guard (punch 75 % when near; tornado with no
  roll; dance 50 %; jump 81 %).
- Tells: punch none (the arm damager is on in frame 1); jump none; tornado 0.2 s, then it homes on the player.
- Bug: `timers["tornado_punch"]` is never updated (`DesertMan.kt:539`, `:805`), so the short-punch damager and the
  `tornado_punch` animation stay on for the whole tornado.
- Damage to the boss: `BOSS_DMG_NEG` (pellet 1, full charge 2) plus Moon Scythe 4 and Reactor Shot 3. 30 pellets.
- Damage to the player: 3 from the size rule; no `PlayerDamageNegotiator` entry.
- Arena: `DesertMan_16x14_v4.tmx` `boss_room` 660, boss object 665 (`left_wall` 662, `right_wall` 661, read with `!!` into
  fields that nothing reads, `:202-208`); the floor is `QuickSand` 656. The boss leaves `INIT` only on sand (`:249`).
- Rematch: `WilyStage1_v2.tmx` has one `DesertMan` object (`room9` before it).
- Long-punch arm gap: `updateArmExtensions` (`:832-842`) places extension `i` at `i x SPRITE_SIZE` from the body edge
  (body 1.5 tiles), but the main sprite (3.5 tiles) draws its arm to 1.75 tiles from the center: a 22 px gap between
  the main sprite and extension 1 on each side. `buildArmExtensions` sizes the rectangles 3.5 px (not x PPM).

## In scope

1. Health phases: `healthPhases = 2`, `onHealthPhaseStart(2)` (shake, `QUAKE_SOUND`, the `dance_flash` frame), the
   phase-2 bar region. Damage table at 2 × the `master` values (owner, 2026-11-15: the
   same hits to kill as `master` over the 2 phases; the first `077a`-scale table was replaced). Tuning rows per difficulty and phase (`DesertManTuning`, the `077a` pattern).
2. Punch wind-up: the unused white-fist `punch_short` frame held (0.5 s normal phase 1, shorter in phase 2 and hard),
   with a sound; the arm damager is off until the hold ends.
3. Tornado: the timer bug fixed; a 0.5 s spin-up tell; the direction locks at the start; in phase 2 one turn back; a
   random roll on its guard.
4. Burrow and erupt (phase 2): he sinks into the quicksand, a `SandSplash` trail runs under the floor toward the
   target player, stops, splashes on one spot for 0.6 s (the tell) with `DIG_SOUND`, then he bursts up with a tornado
   uppercut. Existing art.
5. Long-punch arm: the extensions are placed from the body center, so extension 1 starts where the main sprite's arm
   ends; the rectangles get their real size. The long-punch hitbox is checked against the drawn arm.
6. Damage to the player: 3 for every damager of this fight, in `PlayerDamageNegotiator`, with a `TODO` (item `077`).
7. The dead `left_wall` / `right_wall` fields are used (the arena for the burrow trail) or removed.

## Out of scope

- (Changed in the play-test rounds: new art was added for the burrow, erupt, long punch, throw, needle spin and intro, and the limb outline.) The jump tell (no idea in the chosen set gives it one; a backlog candidate).
- Ideas 4, 6 to 12 of the review (backlog candidates).
- The quicksand and stage layout (item `075`).

## Affected areas and risks

- `DesertMan.kt`, a new `DesertManTuning.kt`, `PlayerDamageNegotiator.kt`, maybe the boss object properties in the 2
  TMX files (a `ground` reference for the burrow).
- The `WILY_STAGE_1` rematch room is dark; the burrow trail must read there.
- Item `075` (another agent) edits the Desert Man stage: it must keep `boss_room` 660, objects 661, 662, 665 and the
  shared ceiling 654, floor 658 and `QuickSand` 656 (the sand must reach x 688, or the fight never starts).

## Overlap with other branches

`075` (not started yet): the Desert Man TMX, see above. `060d`, `fix/small-bugs-3`: no shared files.

## Objective check

| Rule | Check |
|---|---|
| Always-on rules | no effect |
| `O4` | helps (richer fight) |
