# Contract: mega-refactor-084b-effects

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-31

The branch moves the trail sprites, the bubbles, the death orbs and the out-of-bounds kill out of `Megaman.kt`. The same effects must appear at the
same times and places, for Mega and for each boss.

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - `BubbleEmitterComponent`: the timer does not move when `canEmit` is false; one `emit` at the delay; the time carries across frames with `canEmit` false; the timer resets after `emit`;
    `reset()` keeps the time.
  - `TrailSpriteComponent`: the timer always runs; `trySpawn` runs only when the timer is finished; a `false` keeps the timer finished; a `true` resets it; `reset()` keeps the time.
  - `isOutsideLevel`: each of the 4 sides, on the limit and past it; a left or bottom result does not read the map.
  - `ExplosionOrb`: the velocity with no `ConstKeys.SPEED` uses the default, and with the prop it uses the override.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] No old state is left: `grep -nE 'trailSpriteTimer|underWaterBubbleTimer|explosionOrbTrajectories|spawnBubble|tryToSpawnTrailSprite' core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt` finds nothing.
- [ ] The old orb names are gone: `grep -rnE '\bEXPLOSION_ORB_SPEED|EXPLOSION_ORB_TRAJS' core/src/main/kotlin` finds nothing.
- [ ] The parts do not depend on `Megaman`: `grep -lE 'Megaman\b|MegamanValues|MegamanWeapon|SoundAsset' <the 2 new components, LevelBounds.kt>` finds nothing.
- [ ] A short launch (about 15-20 s) of the new test room starts with no exception, on the baseline and at the end of the branch.

## Frame order

Each old line runs at this position. Review the final diff against this table. Lines are at `876b90a6e`.

| Old line in `M/Megaman.kt` | New position |
|---|---|
| `:631-637` out-of-bounds check | `isOutsideLevel` in the same `if`, first in the lambda |
| `:657-669` bubbles | `BubbleEmitterComponent.update`, same position |
| `:671-675` trail | `TrailSpriteComponent.update`, same position, right after the bubbles |
| `:516-525` orbs | `ExplosionOrb.spawnRing`, same position in `onHealthDepleted`, before the `MARIO_LEVEL` sound |

In `AbstractBoss.spawnExplosionOrbs`, `DEFEAT_SOUND` stays before the spawn.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084b-effects`

The branch has no new gameplay, so the experimental checks are the ones with a new test room or a new calculation (the orbs). The control checks follow them.

Experimental: out of bounds:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room out_of_bounds`: walk into the pit. Mega falls past the bottom of the map, and dies with no explosion orbs. Mega respawns. The log has the error
  "Megaman is below game bounds, kill him".

Experimental: death orbs and boss defeats (each boss command starts in the room before the fight):

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die from damage. 8 orbs move out in a ring from Mega's center, at the same speed as before. Die in the pit too. After the respawn, the
  trail and the controls work.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14`: beat the boss. The defeat sound plays, and 8 orbs move out in a ring, at the same speed as before.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: beat the boss. This boss moves the center of the ring.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room10`: beat the boss. This boss runs its own code before the ring.

Control: trail:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room trail`: ground slide and air dash, on both sides. Trail sprites appear behind Mega at the same rate as before. Do this while Mega shoots too: the
  sprite shows the shoot frame.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room trail --weapons NEEDLE_SPIN,RODENT_CLAWS`: with `NEEDLE_SPIN`, a shooting slide shows no trail. With `RODENT_CLAWS`, the trail shows no shoot frame.

Control: bubbles:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: stand in the pool with the head under water. A bubble appears each 2 seconds, in front of Mega, on both sides. With the head above the water, no
  bubbles appear.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: in the water of a real stage, bubbles appear and move as before.

## Behaviors to check

Control:

- One real stage: `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms with slides and dashes.

## Intended changes

None. `ExplosionOrb` gets an optional speed prop, but no caller passes another speed.
