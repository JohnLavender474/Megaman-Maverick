---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: owner play test of `INTRO_STAGE` (2026-11-14)
related-items: []
---

# Projectiles explode on spikes and on every other `DEATH` fixture

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/world/contacts/MegaContactRules.kt:30`: `DEATH` x {`FEET`, `SIDE`,
  `HEAD`, `BODY`} goes to `DeathRule`, in `BEGIN` and `CONTINUE`
- `core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules/DeathRule.kt:20-31`: after the
  `ENTITY_KILLED_BY_DEATH_FIXTURE` and `DEATH_LISTENER` opt-outs, the branch
  `entity is IProjectileEntity -> entity.explodeAndDie()`
- `assets/tiled_maps/tmx/IntroStage_v3.tmx`: `BuilderKibbo` 364 in `room3`, and the spike pit, a `sensors` `Death`
  object 412 at (4480,2816 128x32)

## What happens

Each projectile with a `FEET`, `SIDE`, `HEAD` or `BODY` fixture explodes when it touches a `DEATH` fixture. Lava and
spikes use the same fixture. So a `KibboHammer` that `BuilderKibbo` throws in `INTRO_STAGE` `room3` explodes on the
spike pit.

About 25 projectile classes have such a fixture, for example `KibboHammer`, `Picket`, `CaveRock`,
`BoulderProjectile`, `FallingIcicle`, `JoeBall` and `Bullet` (its own `explodeAndDie` disintegrates).

## What should happen

A projectile ignores `DEATH` fixtures. A damageable projectile still loses its health on a `DEATH` fixture, as before.
Only the projectiles that are made to die in lava explode: `MagmaMeteor`, `Fireball`, `MagmaOrb`, `MagmaWave` and
`MagmaGoop`.

## How to reproduce

`./run-game-alpha.sh --level INTRO_STAGE --room room3`: let `BuilderKibbo` throw hammers that fall into the spike pit.
Each hammer explodes on the spikes.

## Notes

History:

- `dd775871f` (owner, 2026-06-21, "Add feet to meteor so that it dies on lava contact") gave `MagmaMeteor` a `FEET`
  fixture.
- `37e9da1e3` (owner, 2026-06-21, "Update 'continue death contact' listener to handle projectiles"), the commit after
  it, added the `IProjectileEntity -> explodeAndDie()` branch. Its aim was the meteor in lava. Before it, a projectile
  that is not damageable did nothing on a `DEATH` fixture. The branch acts on every projectile, so the hammer on the
  spikes is a side effect.
- `Petal` and `DeadlyLeaf` opt out today with `ENTITY_KILLED_BY_DEATH_FIXTURE` or `DEATH_LISTENER`.
- `09414c76f` turned off the feet and body of a `Fireball` that a `FireballBar` holds. This stays.

Options:

1. Keep the branch, and add an opt-out to each projectile that must not explode (about 25 classes, and each new
   projectile must remember it).
2. Give spikes a fixture of their own, apart from lava. This changes the map data and the sensor classes.
3. A projectile ignores `DEATH` fixtures by default, and a class opts in to explode. Lava and spikes still share the
   fixture, so an opt-in class also explodes on spikes.

Owner decision (2026-11-14): option 3, with the opt-in classes `MagmaMeteor`, `Fireball`, `MagmaOrb`, `MagmaWave` and
`MagmaGoop`. An opt-in class that explodes on spikes is accepted.
