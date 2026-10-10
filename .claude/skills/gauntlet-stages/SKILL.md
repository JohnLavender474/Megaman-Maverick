---
name: gauntlet-stages
description: Use when you design, build or edit a gauntlet stage (a stage whose rooms lock until waves of enemies are beaten, using GauntletMaster), for example Gauntlet1_v2.tmx.
---

# Gauntlet Stages

A gauntlet stage is a series of rooms. In a gauntlet room, a `GauntletMaster` spawns waves of
enemies, and the exit gate stays locked until the last wave is dead. Use this skill together with
`level-designer` (design rules 6–13) and `level-builder`.

Reference stage: `assets/tiled_maps/tmx/Gauntlet1_v2.tmx` (design notes in
`temp/level_drafts/Gauntlet1_v2/overview.txt`).

## Room rules

1. **Every room with enemies is a gauntlet room.** Its enemies come only from its
   `GauntletMaster` waves. Never put enemies in the `enemies` layer of a gauntlet stage.
2. **A room with only hazards is not a gauntlet room**, because most hazards cannot be destroyed.
   Rest rooms (checkpoint, items) have no enemies either. The first room of the stage (start
   spawn `0`) is a blank room.
3. **Gauntlet rooms are single screens (16×14).** Then no enemy fights from off-screen.
   Hazard-only rooms and rest rooms can be larger than 16×14.
4. **Every gauntlet room exits through a horizontal `Gate`** with `gauntlet_key` = the master's
   `key`. A gauntlet room must not have a vertical exit (ladder or drop), because nothing can lock it.
5. **Entering a gauntlet room by a drop is allowed.** But if the player can climb back up (wall
   jump), the gauntlet restarts each time they come back. To make the drop one-way, give the shaft
   walls `fixture_labels=no_side_touchie`.
6. **Keep ground enemies at least 4 tiles from the entrance** (gate, doorway, drop).
7. **Fit each enemy to the geometry** (`level-designer` Rule 10): jumpers need headroom and runway;
   chargers and walkers need flat floor with no pit; never put a big enemy between low platforms.
8. **Put hazards where the player must pass.** A hazard in a part of the room that the player
   does not need to cross has no effect. Force the route through the hazards (for example, lower
   the ceiling to make a corridor).
9. **Stage-specific bans** go in the stage's `overview.txt` (Gauntlet1: no FlameThrowers).
10. **Difficulty rises across the whole stage, not only inside each room.** The first wave of a
    gauntlet room is about as hard as the middle wave of the gauntlet room before it, never a reset
    to "two Mets". Inside a room, each wave is harder than the one before. Score each wave to check
    it: easy enemy 1 (Met, Bat, Screwie, Peat, FloatingCan), medium 2 (SniperJoe, ColtonJoe,
    turrets, HeliMet, SpikeCopter, Ratton, Elecn), hard 3 (GreenUziJoe, LampeonBandito, PetitDevil, BikerKibbo,
    BuilderKibbo, DrillTankXT), big jumper 4 (BigJumpingJoe, Robbit). Use cheap hole drops (Mets)
    as filler next to harder enemies, not as a whole wave.

## GauntletMaster (`specials` layer)

```xml
<object id="482" name="GauntletMaster" x="816" y="352" width="32" height="32">
 <properties>
  <property name="key" value="g0"/>
  <property name="room" value="room2"/>
  <property name="spawn_type" value="spawn_now"/>
  <property name="wave_1_1" type="object" value="389"/>
  <property name="wave_1_2" type="object" value="391"/>
  <property name="wave_2_1" type="object" value="390"/>
 </properties>
</object>
```

- `room` (required): the room it controls. `key` (required): matches the exit gate's `gauntlet_key`.
- `wave_<w>_<n>` (object refs): enemy `n` of wave `w`. Waves start at 1 and must have no gaps.
- `start_delay` (default 1.5 s) and `wave_delay` (default 1 s). In the stage's spawn room, use a
  longer `start_delay` (about 2.5 s) so that wave 1 does not spawn during the READY sequence.
- The referenced enemies go in the `children` layer (not built). The object `name` is the enemy
  class; its custom props go to the enemy. The master adds `cull_out_of_bounds=false` and
  `cull_events=false`.

## Enemy entrances

- **Ground or air spot**: give the enemy `spawn_blink` (float, e.g. `1`). It blinks and is
  invincible and harmless for that time. The AI is not paused, so an enemy that attacks at once
  (for example PetitDevil, which flies straight at Mega Man) also needs its own start delay:
  PetitDevil takes `delay` (float, default 0); set it equal to `spawn_blink`. It stays still and
  spawns its orbiting children only after the delay.
- **Ceiling hole**: leave a gap in the ceiling and cover it with an `AbstractBlock`
  (`block_filters` = the enemy tags that drop through, `spawn_type=spawn_now`,
  `cull_out_of_bounds=false`). Put the enemy object in the hole cell, bottom-aligned, with
  `spawn_blink=1` too: a falling enemy can hurt Mega Man at once, so it must not drop onto him
  without warning.
- **Every wave enemy that can hurt Mega Man on contact gets `spawn_blink=1`**, whatever its
  entrance. The only exception is an enemy with its own harmless entrance (for example the
  shielded HeliMet fly-in).
- **Fly-in (HeliMet)**: place it 1 tile outside the side wall with a `target_1` object ref to a
  `Target` object in `children`. One target per HeliMet.
- A child more than 2 tiles outside the room is destroyed and counts as killed.

## Reset behaviour (how the code works)

| Event | Result |
|---|---|
| Player leaves the room during a wave (`BEGIN_ROOM_TRANS`) | All enemies are destroyed; the master resets. Re-entering starts again from wave 1. |
| Player leaves a cleared room and comes back | The master stays cleared; the gate stays open. |
| Player dies (`PLAYER_JUST_DIED`) | Every master resets, cleared ones too. On `PLAYER_SPAWN` every gauntlet gate locks again. |
| Level start or respawn in a gauntlet room (`SET_TO_ROOM_NO_TRANS`) | The master starts after `start_delay`. |

## Hazard-only rooms

Use hazards that the player must pass through (Rule 8). Patterns that work:

- **InfernoOven corridor** (see `level-designer` Rule 13): a 7-tile-tall corridor with a floor
  oven post, a ceiling oven post over a recessed spike pit, and a buried floor vent. All ovens in
  a room fire together (1 s cold, 1 s warning, 1 s hot); there is no phase prop.
- **Swinging saws** attached to anchor blocks, placed over the platforms and ladders that the
  player must use.
- Other self-contained hazards: `FireballBar` (needs `spawn_room`, pivot block), `CeilingCrusher`
  (needs a floor below), `LavaDropSupplier`, `PipePuff` (`direction` + `spawn_room`), `Saw` with
  `type=t` and a `trajectory`. Avoid `SpikeBall` and `Electrocutie` (no culling).
