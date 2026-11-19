# Scope: mega-refactor-077c-elec-devil

- Status: approved
- Approved: 2026-11-18 (owner, answers to the idea round and the 4 planning questions; no brief, because every
  decision is answered, as in `077b`)

## Goal

A richer `ElecDevil` fight (the `WILY_STAGE_1` boss): 2 health phases, new attacks with the Spark Serpent as the
signature, and art polish of the same design.

## Item

`077` (one branch per boss; a phased boss gets its harder attacks in its last phase in place of a panic attack).
Idea list and contact sheet: `temp/077-elec-devil/` in the main checkout.

## Owner answers (2026-11-18)

- Phases: phase 1 is the current loop plus Eye Spread and Live Wire. Phase 2 picks each cycle from Launch, Spark
  Serpent, Circuit Floor and Storm Rain, then Charge / Hand, with quicker timings.
- Hits to kill: 1-for-1 with `master` over the 2 phases (item rule of 2026-11-15): each damage value is 2 × the
  `master` value.
- Serpent path: Tiled polylines in the boss room, referenced from the `ElecDevil` object (`serpent_paths`); the boss
  picks one each time.
- Shots against a Serpent cube: the current rule of a flying `ElecDevilBodyPiece` (the shared projectile contact
  rule), with a `TODO` that points to the backlog candidate (electrified scythes and axes).
- Art: Claude Fable 5.2 agents draft the frames in `temp/077c-elec-devil/` from the current frames; the owner picks
  and fixes them before they go into the atlas.
- No Blackout attack: the room is already dark. Keep the unclimbable side blocks (no wall slide in the room).

## Current state (scan)

- `entities/bosses/ElecDevil.kt` (689 lines) with `ElecDevilBody`, `ElecDevilBodyPieces`, `ElecDevilBodyPiece`,
  `ElecDevilConstants`, `ElecDevilState`. One fixed `Loop`: `LAUNCH, APPEAR, STAND, CHARGE, STAND, HAND, STAND,
  TURN_TO_PIECES`. No health phases, no tuning rows, no tests.
- Only the eye circle (`DAMAGEABLE`) takes damage, in `STAND`, `CHARGE` and `HAND`. The hand is `SHIELD` and
  `DAMAGER` in `HAND`. Each body cell is a `DAMAGER`; a flying piece is `PROJECTILE` and `DAMAGER`.
- Damage to the boss: `AbstractBoss` default plus `Axe` 2 (`damageOverrides`). Damage to the player: the size rule;
  no `PlayerDamageNegotiator` entry.
- Piece speed and launch delay scale with health; every 3rd launch has random target rows.
- Eye shot: one `ElecBall` at the target player with ±0.5 tile noise (`shootFromEye`).
- Light: the room is dark; body cells (`lightUp`) and flying pieces (`ILightSource`, radius 3) light it.
- Arena: `WilyStage1_v2.tmx` `boss_room` 429 (16 × 14 tiles, one screen); `ElecDevil` 448 with `left` 449, `right`
  450, `start_x` 451, `room` 429, `light_source_keys` 1. The bodies (5 tiles wide) stand at x 2.5 and x 12.5, so the
  player has a free strip of about 5 tiles in the middle. `room10` holds the boss gate and a player spawn.

## In scope

1. Health phases: `healthPhases = 2`, `onHealthPhaseStart(2)` (a tell: flash, sound), the phase-2 bar region, the
   damage table at 2 × `master`. Tuning rows per difficulty and phase (`ElecDevilTuning`, the `077a` pattern).
   Damage to the player in `PlayerDamageNegotiator` (the `077b` rule; values equal to `master`).
2. Attack choice: the fixed `Loop` becomes a cycle with a phase-2 pick (seeded `services.random`).
3. Eye Spread (both phases): 3 `ElecBall`s with mixed aims (lead, current, near random), in place of the single
   charge shot; the eye colour telegraph before it.
4. Live Wire (both phases): during `LAUNCH`, some consecutive flying pieces are linked by a damaging arc; a gap in
   the chain lets the player through.
5. Spark Serpent (phase 2): the cells leave the body and form an arc-linked chain that follows one of the
   `serpent_paths` polylines, then the cells form the body on the far side. The eye cell is the head and the only
   damageable part. Shots against cubes: the flying-piece rule, with a `TODO`.
6. Circuit Floor (phase 2): some pieces embed in the floor in the middle strip; a lit tell, then a current runs along
   them in sequence.
7. Storm Rain (phase 2): pieces rise off screen and fall as bolts with the 4 mixed aims (lead, current, near random,
   wide random); each target spot is lit before the hit.
8. Art polish (drafts by agents, owner review): horn prongs with an arc, a larger eye with a colour telegraph,
   bolder traces with a current pulse, a claw hand, a cell-by-cell assemble and split, a death of several frames,
   new art for the arc, the floor current and the bolt. README credit lines.
9. TMX: the `serpent_paths` polylines in `boss_room` of `WilyStage1_v2.tmx`; a new `RefactorTest077.tmx` with an
   `ElecDevil` room and `LevelDefinition.REFACTOR_TEST_077`.
10. Tests: a fight rig (as `077b` `DesertManRig`) for the attack choice per phase, the phase start, the aims and
    the serpent path read.

## Out of scope

- Player shots that a cube destroys, and electrified `MoonScythe` and `Axe` (backlog candidate in `TODO.md`).
- The boss room art (walls, cables, generator): item `078`.
- A move of the fight into the `118` routines (`118d`).

## Affected areas and risks

- `ElecDevil*.kt`, a new `ElecDevilTuning.kt`, maybe new entities for the arc, the floor current and the bolt;
  `PlayerDamageNegotiator.kt`; the `ElecDevil` frames and atlas; `WilyStage1_v2.tmx` (boss object and new polylines
  only); a new test map; `LevelDefinition.kt`; `README.md` credits.
- The middle strip is about 5 tiles: Circuit Floor and Storm Rain must leave a readable safe spot.
- Co-op (`O2-R5`): aims go at `targetPlayer`; the fight must not assume one player.

## Overlap with other branches

See `NOTES.md`: `078` (Wily Stage 1 art, the boss room), `118d`. `047k`, `060k`: no shared files.

## Objective check

| Rule | Check |
|---|---|
| Always-on rules | no effect |
| `O1-R4` Typed state, not new property keys | helps: the serpent paths are read once into a typed field |
| `O1-R5` Code only with a production caller | no effect: each new type has a caller in the fight |
| `O2-R2` Random only from the seeded source | helps: every new pick uses `services.random` |
| `O2-R5` No logic assumes one player | no effect: aims use `targetPlayer` |
| `O4` | helps (richer fight) |
| `O4-R1` Content from data | helps: the serpent paths are map data |
| `O4-R3` State machines | waiting (`015`): the fight keeps its own state model |
| `O5-R1` No per-frame allocation | no effect: arcs and aims use owned fields or pools |
