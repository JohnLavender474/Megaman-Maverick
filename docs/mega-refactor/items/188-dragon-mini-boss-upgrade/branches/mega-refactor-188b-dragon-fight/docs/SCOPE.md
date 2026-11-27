# Scope: mega-refactor-188b-dragon-fight

What this branch changes and why, and its contract. This is Claude's record. If the scope changes later, update this
file.

- Status: approved (the owner approved the art of `188a` and asked for the implementation; the decisions are in the
  main session's brief to the implementation agent, 2026-11-26, and in `NOTES.md` "Decisions")
- Approved: 2026-11-26

## Goal

Put the `188a` art into the game and give `MechaDragon` its new fight: it lands on its side ledge, sweeps a flame
breath, chomps the pocket under its chin, takes off again, and aims its fireball volley with the item `077` aim mix.
The player sees legs on every pose, a grounded half of the fight and a panic mode under 25% health.

## Item

`188` Dragon mini-boss upgrade (legs and new attacks), branch `188b` of the proposed split in `ITEM.md`: "`188b`
(code): the new states and attacks in `MechaDragon.kt` and its fight plan, with the new frames wired in." The fight
plan (`FightPlans.miniBossPlan`) needs no change: the dragon stays a mini boss with no bar, no freeze and no music.

## Current state

- `MechaDragon.kt` (808 lines): states `INIT`, `HOVER_IN_PLACE`, `FLY_TO_TARGET`, `CHARGE_TO_OTHER_SIDE`,
  `TURNING_AROUND` on `EnumStateMachineBuilder`. Volley of 1 to 3 `SpitFireball`s, all at the player with a 10 degree
  spread (`MechaDragon.kt:734-797`). Charge to a random charge target of the other side (`:675-690`), also the low
  lane. 8x8-tile sprite centered on the body; `fire1` cut as `AnimationDef(4, 4)` (`:121`).
- The regions come from `Bosses1` (`TextureAsset.BOSSES_1`); no other code reads `MechaDragon/...` (`MechaDragon_OLD`
  is commented out; `BossesFactory` has only a commented line).
- The `188a` strips in `assets/sprites/frames/Bosses1/MechaDragon/` are not in any atlas yet: airborne cells are now
  128x144, the grounded cells 128x128, `fire1` is 5x2.
- The map: `InfernoMan_16x14_v3.tmx`, object `399` in the `children` layer; `mini_boss_room` (boss room, `event=boss`)
  under `room8`. `room8` holds player spawn `4`.
- `OrbAim` (`bigassmaverickrobot/OrbAim.kt`) already has 2 callers (`BigAssMaverickRobot`, `GlacierMan`, which
  imports it as is). Item `158` (shared lead-aim helper) is not started.
- Random values: `services.random` (`GameRandom`, seeded; rule `O2-R2`).

## In scope

- New atlas `MechaDragon` (`TextureAsset.MECHA_DRAGON`, `sprite_sheets/MechaDragon.txt/.png`, `texture_packer/
  MechaDragon.tps`). The frames move to `assets/sprites/frames/MechaDragon/MechaDragon/` (so the region names stay
  `MechaDragon/<strip>`), and `flame_puff.png` becomes `frames/MechaDragon/MechaDragonFlame.png` (region
  `MechaDragonFlame`, the `TAG` of its entity). The `Bosses1.tps` entries of the moved files are removed; the packed
  `Bosses1` atlas keeps its old, now unused `MechaDragon/...` regions until a repack on a laptop.
- `MechaDragon`: new states `LAND`, `BREATH`, `CHOMP`, `TAKEOFF`; air and ground fixture layouts; leg fixtures;
  head fixtures from the head boxes of `ANCHORS.json`; the aim mix for the volley; high charge lane only; a charge
  tell; panic below 25%; hard-mode rows.
- New package `entities/bosses/mechadragon/`: `MechaDragonArt` (cell layout, anchors, the angle-to-frame map),
  `MechaDragonDirector` (move choice), `MechaDragonTuning` (normal, hard, panic rows).
- New projectile `MechaDragonFlame` (`entities/projectiles/`).
- `PlayerDamageNegotiator`: entries for `MechaDragon` and `MechaDragonFlame` (3 each).
- Map: objects `LeftLandTarget` and `RightLandTarget` in `children`, and the spawn props `left_land_target`,
  `right_land_target` on object `399`. New key `ConstKeys.LAND`.
- README `CREDITS`: the Inferno Dragon entry with the original and the update.
- Tests (see the contract).

## Out of scope

- No new art, no new sounds. No charge art (the tell uses the existing frames, see `NOTES.md`).
- No move to a shared lead-aim helper (item `158`): `OrbAim` is used as is, as `GlacierMan` does.
- No repack of `Bosses1` (TexturePacker is not installed in the cloud).
- No change to `FightPlans`, `SpitFireball`, `LavaDrop`, `MagmaExplosion`.
- The low charge-lane objects stay in the map (unused).

## New API and its first caller

| API | First caller |
|---|---|
| `TextureAsset.MECHA_DRAGON` | `MechaDragon.init`, `MechaDragonFlame.init` |
| `ConstKeys.LAND` | `MechaDragon` spawn-prop names |
| `MechaDragonArt` (anchors, `sweepCell`, `cellAt`, offsets) | `MechaDragon` (fixtures, sprite, flame spawn) |
| `MechaDragonDirector.afterHover/afterBreath/chargeChanceAfter` | `MechaDragon.endHover`, `MechaDragon.endBreath` |
| `MechaDragonTuning.of(hard, panic)` | `MechaDragon.tuning` |
| `MechaDragonFlame` | `MechaDragon.emitFlame` |

## Affected areas and risks

- `MechaDragon.kt` is rewritten in large parts: the airborne behavior must stay as before (hover, sway, volley
  timings, turn, charge speed). Risk: a move that never ends (stuck state). Each phase ends on a timer or a reached
  target.
- The sprite placement changes (8x9 airborne, 8x8 grounded, by the bottom). Risk: a pose a few pixels off; the draw
  offsets are whole NES pixels.
- New atlas: risk of a region name or size mismatch. A test reads the atlas file and the frame strips.
- The code was not compiled in this session (Gradle blocked); CI is the first compile.

## Overlap with other branches

`188a` (parent of this branch, art only). No other active branch touches `MechaDragon`, `PlayerDamageNegotiator`
entries of the dragon, or `InfernoMan_16x14_v3.tmx` (checked the branch folders of the active items on 2026-11-26).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | Session reads go through `services`. |
| `O1-R2` Components, not inheritance | no effect | `MechaDragonFlame` extends `AbstractProjectile`, as every projectile. |
| `O1-R3` No new fragile order | no effect | |
| `O1-R4` Typed state, not property keys | no effect | One new map key (`land`), for map data. |
| `O1-R5` Code only with a production caller | helps | Each new API has its caller (table above). |
| `O2-R2` Random only from the seeded source | helps | Every roll goes through `services.random`. |
| `O2-R4` Stable order | no effect | |
| `O2-R5` No logic assumes one player | helps | The pocket check takes any live player; aims use `targetPlayer` as before. |
| `O2-R7` Gameplay does not depend on rendering | no effect | The head boxes come from the state timer, not from the drawn frame. |
| `O5-R1` No per-frame allocation | helps | New code uses owned fields and pool scratches; the old `flyToTarget` `cpy()` is replaced. |
| `O6-R1` CI passes | waiting | Gradle cannot run in the cloud; CI of the PR is the check. |
| `O6-R2` Named checks | helps | Tests below. |
| `O4-R1` Mechanics from Tiled | helps | The land points are map objects. |
| `O4-R2` A test room for each mechanic | no effect | A boss move, not a level mechanic; the checks use the real room (it depends on its geometry). |
| `O4-R3` Patterns as states | waiting | After `015`; the dragon keeps the `EnumStateMachineBuilder` pattern with sub-phases. |

## Open questions

- None blocking. Play-test questions are in `NOTES.md` "For the play test".

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new specs `MechaDragonDirectorTest`, `MechaDragonArtTest`,
  `MechaDragonAtlasTest`, `MechaDragonFlameTest`, `MechaDragonTuningTest`.
- [ ] `./gradlew core:smokeTest` passes (`MechaDragon` and `MechaDragonFlame` find every region in the real atlas;
  `entity-tags.txt` lists `MechaDragonFlame`).
- [ ] `./gradlew lwjgl3:jar` builds; `detektMain core:verifyRandomRule` pass.
- [ ] The atlas check script (`temp/dragon-code/check_atlas.py`, not committed): every region equals its source strip
  pixel for pixel.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

From the worktree. `room8` is the room above the dragon room (it holds player spawn `4`); drop through the floor hole.

1. `./run-mega-man-maverick-alpha.sh --level INFERNO_MAN --room room8 --heartTanks all --healthTanks all`: the
   dragon flies in with legs that dangle, hovers and spits volleys (ball 1 at you, ball 2 ahead of you, ball 3 near
   you). Within 2 hovers it flies over its own ledge, hangs with legs out, stomps down (screen shake), 3 lava drips
   hang from the ceiling and fall (at you, ahead of you, near you). It sweeps its flame breath from high to low; under
   its chin (near half of the center block) and the far ledge are safe. If you stand under its chin, it often chomps
   the floor in front of it (a fire burst). Then it crouches and takes off. The charge uses the high lane only, after
   a tell (fast wing beats, a short drift back, a roar). Below 25% health it roars once, the breath sweeps back up,
   and it chomps every time you are under its chin. The legs hurt you on its ledge. The fight ends and the gate opens.
2. `./run-mega-man-maverick-alpha.sh --level INFERNO_MAN --room room8 --heartTanks all --healthTanks all --difficulty hard`:
   shorter tells and a faster sweep; the breath always sweeps back up.
3. `./run-mega-man-maverick-alpha.sh --level INFERNO_MAN --room room8 --heartTanks all --healthTanks all --weapons FRIGID_SHOT`:
   control: freeze the dragon with Frigid Shot in the air and on its ledge: it shows the frozen pose
   (on the ledge: standing on its dangling feet), then goes on. The defeat fades out as before, also on the ledge.

## Behaviors to check

- The mini-boss flow: no bar, no world freeze, no boss music, the exit gate opens on defeat (`FightTest` covers the
  plan).

## Intended changes

- Legs on every pose; the airborne sprite is 8x9 tiles (top edge as before), the grounded sprite 8x8 on the ledge.
- New moves: landing stomp with lava drips, sweeping flame breath, chin chomp, takeoff.
- The volley aims with the `077` mix (current, lead about 1 s, near) in place of a 10 degree spread.
- The charge uses the high lane only and has a tell.
- Panic below 25% health; hard mode has shorter tells and sweeps and always the return sweep.
- `fire1` plays its 10 cells (5x2).
