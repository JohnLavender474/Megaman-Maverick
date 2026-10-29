---
status: open
verified: question
area: core, content
gameplay-change: yes
found-by: owner (2026-10-29)
related-items: []
---

# Refine the final Dr. Wily boss fight

This issue is a design refinement of the final boss fight (`WilyFinalBoss`, the last room of `WILY_STAGE_3`). The owner
asks to flesh out the fight. The owner gives two candidates. The owner decides which candidates to do and in which form.

## Where

- Boss: `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/WilyFinalBoss.kt`. Three phases
  (`WilyFinalBoss.kt:157-165`): phase 1 is the plane, phase 2 is the capsule with tentacles, phase 3 is the capsule that
  appears at random points together with one fake capsule.
- Fake capsule: `core/src/main/kotlin/com/megaman/maverick/game/entities/hazards/WilyFakeCapsule.kt`.
- Missile: `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/HomingMissile.kt`. Only
  `WilyFinalBoss` and `WilyFakeCapsule` use it.
- Energy ball: `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/BigAssMaverickRobotOrb.kt`. It is
  shared: `BigAssMaverickRobot`, `BigAssMaverickRobotHand`, `GutsTank`, `HeliMet` and `Met` also use it.
- Map: `assets/tiled_maps/tmx/WilyStage3_v4.tmx` (the live map of `LevelDefinition.WILY_STAGE_3`). The boss room is
  `wily_room` (object 636, x 11360-11872, y 3744-4192, 16 x 14 tiles). The boss object is `WilyFinalBoss` (object 641,
  layer `children`).
- The two air platforms are objects 656 and 657 on the `blocks` layer. They have no name and no properties, so the
  blocks builder makes a plain `Block` (`SpawnersLayerBuilder.kt:69-71`).

  | Object | x (px) | y (px) | Size (tiles) | Top above floor |
  |---|---|---|---|---|
  | 656 | 11424-11488 | 3968-4000 | 2 x 1 | 6 tiles |
  | 657 | 11744-11808 | 3968-4000 | 2 x 1 | 6 tiles |

  The floor is object 655 (top at y 4160). Objects 660 and 668 are 1-tile steps on the floor. Objects 669 and 670 are
  2-tile pillars at the side walls. The side walls 653 and 654 are `AbstractBlock`.

## What happens

### Candidate 1: the air platforms stop the boss projectiles

The two air platforms are plain `Block` objects with no fixture labels. When a projectile fixture touches a `BLOCK`
fixture, `ProjectileRule` calls `hitBlock` (`world/contacts/rules/ProjectileRules.kt:28-46`). It skips the call only
when the block fixture has `FixtureLabel.NO_PROJECTILE_COLLISION`. The boss projectiles then end:

- `HomingMissile.hitBlock` calls `explodeAndDie()` (`HomingMissile.kt:137-140`).
- `BigAssMaverickRobotOrb.hitBlock` calls `getHit()`: the orb stops and plays its hit animation
  (`BigAssMaverickRobotOrb.kt:125-132`, `:143-153`).
- The phase 1 `Bullet` also ends on a block (`Bullet.kt:150-153`).

A clear case from the map: in phase 3 the capsules appear at the `Phase3Spawn` points (objects 720-724). At 720 and 721
(the two high side points, also the intro points), the fake capsule's bottom missile starts straight above platform 657
or 656. The missile starts 1 tile above the platform top and moves down at 5 tiles/s, so it explodes on the platform
after about 0.2 s.

Mega Man cannot jump up through the platforms today: his head stops on them (`HeadBlockRule.kt:17-24` skips the stop
only for `COLLIDE_DOWN_ONLY`).

### Candidate 2: the phase 3 missiles crash into the ground when the capsule is low

In phase 3, `Phase3Handler` picks 2 of the 5 `Phase3Spawn` points for each cycle: one for the real capsule, one for the
fake capsule (`WilyFinalBoss.kt:2854-2873`, `FAKE_CAPSULE_COUNT = 1` at `:2765`). In the `ATTACK` state, only the fake
capsule fires missiles, once per cycle (`WilyFinalBoss.kt:3050-3053`). The real capsule fires energy balls.

`WilyFakeCapsule.shootMissiles()` (`WilyFakeCapsule.kt:133-172`) fires 3 missiles:

- left cannon: 3 tiles left, 0.75 tiles below the center, angle 225 (down-left);
- bottom cannon: 2 tiles below the center, angle 180 (straight down);
- right cannon: 3 tiles right, 0.75 tiles below the center, angle 135 (down-right);
- all with an init delay of `MISSILE_INIT_DELAY = 0.75f` (`WilyFakeCapsule.kt:66-69`).

`HomingMissile` (`HomingMissile.kt:58-63`, `:198-217`):

- `SPEED = 5` tiles/s, a fixed speed.
- During the init delay, the missile flies straight on its launch angle.
- After the delay, `recalcTimer` (`RECALC_DELAY = 0.25f`) starts. It is updated only after the init delay ends, so the
  first turn comes at 0.75 + 0.25 = 1.0 s after launch.
- Each turn points the missile at the top corner of Mega Man, snapped to 8 directions (45 degree steps)
  (`HomingMissile.kt:188-196`).
- `TTL = 3f`: the missile explodes after 3 s.
- On any `Block` it explodes (`hitBlock`, `HomingMissile.kt:137-140`).

The capsule height depends on the spawn point (center of the 3 x 4 tile capsule body, above the floor top):

| Spawn | Center (px) | Height above floor |
|---|---|---|
| 720 (intro) | 11760, 3856 | 9.5 tiles |
| 721 (intro) | 11472, 3856 | 9.5 tiles |
| 722 | 11616, 3880 | 8.75 tiles |
| 723 | 11456, 4016 | 4.5 tiles |
| 724 | 11776, 4016 | 4.5 tiles |

At 723 and 724 (the low points), by calculation from the values above:

- The bottom missile starts with its lower edge 2 tiles above the floor. It hits the floor after about 0.4 s.
- The two side missiles start with their lower edge 3.25 tiles above the floor. They fall at about 3.5 tiles/s on the
  diagonal and hit the floor after about 0.9 s.
- So all three missiles explode before their first turn at 1.0 s. They never home in on Mega Man.

At the high points, the missiles have enough height to turn, except the bottom missile at 720 and 721, which hits an air
platform (see candidate 1).

For comparison, phase 1 already handles a low position: when the plane hovers in the lower row, it fires its missiles
upward (`shootMissiles(up)`, `WilyFinalBoss.kt:1013-1016`, `:1918-1951`, `MISSILE_ANGLES` and
`TIME_BEFORE_FIRST_RECALC` at `:919-920`).

## What should happen

### Candidate 1

The two air platforms become "collide down only" platforms: Mega Man can jump up through them from below and land on
them from above, and the boss projectiles (missiles and energy balls) go through them.

What "collide down only" means in this code:

- `BodyLabel.COLLIDE_DOWN_ONLY` on the block body. `MegaCollisionHandler` then resolves a collision only when the body
  comes down onto the top of the block (`MegaCollisionHandler.kt:45-80`). A body that moves up passes through.
- `HeadBlockRule` does not stop Mega Man's head on such a block (`HeadBlockRule.kt:18`, `:31`). Mega Man's feet and
  gravity code ignore such a block while he moves up (`megaman/components/BodyComponent.kt:129`, `:173`).
- `CollideDownOnlyBlock` (`entities/blocks/CollideDownOnlyBlock.kt`) is a `Block` that sets
  `BodyLabel.COLLIDE_DOWN_ONLY` and the fixture labels `NO_SIDE_TOUCHIE` and `NO_PROJECTILE_COLLISION`. The live map
  `SMB3_1.tmx` uses it 11 times. A map can also set the same labels as properties on a plain `Block`:
  `body_labels` = `collide_down_only` and `fixture_labels` = `no_side_touchie,no_projectile_collision` (older maps such
  as `GlacierMan_v2.tmx` do this).
- `COLLIDE_DOWN_ONLY` alone does not let projectiles through. `ProjectileRule` checks only
  `NO_PROJECTILE_COLLISION`. Some projectiles check the body label themselves, for example `Fireball` ignores such a
  block (`Fireball.kt:232`). `HomingMissile`, `BigAssMaverickRobotOrb` and `Bullet` do not.

Possible ways (the fix branch decides):

1. Map only (smallest change): set objects 656 and 657 to `CollideDownOnlyBlock` (object name), or add the two label
   properties above. No code changes. Effect: every projectile goes through the platforms. This includes Mega Man's
   own shots and the phase 1 `Bullet`, not only the missiles and energy balls. Only these 2 objects change, so no other
   room or entity changes.
2. Map and projectile classes: give the platforms only `COLLIDE_DOWN_ONLY` (without `NO_PROJECTILE_COLLISION`), and
   make `HomingMissile` and `BigAssMaverickRobotOrb` ignore `COLLIDE_DOWN_ONLY` blocks in `hitBlock` (as `Fireball`
   does). Mega Man's shots then still stop on the platforms. Note: `BigAssMaverickRobotOrb` is shared with 5 other
   entities (see `Where`), so a change in its class also changes those fights wherever they have
   `COLLIDE_DOWN_ONLY` blocks. A spawn property or a check on the owner can limit the change to this fight.

Must not change: the platform positions and sizes, the floor, the walls, and the other rooms of `WILY_STAGE_3`.

### Candidate 2

When the capsule is low, the missiles must stay a danger to Mega Man: they turn toward him before they reach the
ground. When the capsule is high, the missiles act as they do today.

Possible directions (not chosen; the fix branch decides with the owner):

- A launch angle that depends on the height: for example, fire sideways or upward from a low point. Phase 1 already
  does this for the lower row (`WilyFinalBoss.kt:1918-1931`).
- An init delay that depends on the height above the ground: the fake capsule passes a shorter delay when it is low.
- Start the first turn at once when the init delay ends (today the missile waits one more `RECALC_DELAY`, so the first
  turn is at 1.0 s, not at 0.75 s).
- Ground-aware homing: the missile turns before it reaches a block below it, or it does not pick a direction into the
  ground.
- A per-spawn value in the map: a property on each `Phase3Spawn` object (angles or delay), so the level sets it.

## How to reproduce

From the main checkout:

```
./run-game-alpha.sh --level WILY_STAGE_3 --room room8 --weapons all --heartTanks all --healthTanks all
```

`room8` is the last room with a player spawn before the boss. Go down through `room9` into the boss room.

- Candidate 1: in phase 1, phase 2 and phase 3, watch the missiles, the energy balls and the bullets that touch the two
  air platforms: they end on the platforms. Jump up into a platform from below: Mega Man's head stops on it.
- Candidate 2: beat phase 1 and phase 2. In phase 3, wait for a cycle where the fake capsule appears at one of the two
  low points (left or right, about 4.5 tiles above the floor). Watch its 3 missiles: they hit the floor before they turn
  toward Mega Man. Compare with a cycle at a high point.

## Notes

- This is a design refinement, not a refactor. A `fix/<slug>` branch off `master` does it, with a short contract that
  says which candidates the owner chose, what changes, and what must not change. If the work grows (for example new
  attacks or a new phase pattern), make it a small item instead.
- The heights and times in `What happens` come from reading the code and the map, not from a play test.
- If the fix uses string label properties in the map, write them without spaces (`no_side_touchie,no_projectile_collision`).
  `Block` strips spaces with `replace("\\s+", "")`, which treats the pattern as literal text (`Block.kt:128`, see
  [2026-10-22-literal-whitespace-regex.md](2026-10-22-literal-whitespace-regex.md)).
- Other refinements found while reading. They are NOT requested; they are optional ideas for the owner only:
  - The outer cannon of a fake capsule at a side spawn point (720, 721, 723, 724) is 3 tiles from the center, at the
    side wall line. That missile flies down into the side wall (`AbstractBlock`, which lets projectiles through) and out
    of view before it can turn.
  - `HomingMissile` turns only in 45 degree steps and keeps a fixed speed, so it is easy to dodge once it turns.
  - In phase 1, when the plane is in the lower row, the outer upward missiles pass close to the bottom of the air
    platforms. Not checked in the game.
