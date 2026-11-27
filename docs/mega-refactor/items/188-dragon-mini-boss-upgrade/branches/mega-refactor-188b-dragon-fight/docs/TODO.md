# TODO: mega-refactor-188b-dragon-fight

## Tasks

- Play test (checks 1 to 3 of the contract) and the CI result of the PR (Gradle could not run in the cloud).
- The owner can repack `MechaDragon.tps` and `Bosses1.tps` with TexturePacker on a laptop (`Basic`): `Bosses1` then
  drops the old, unused `MechaDragon/...` regions.

## Backlog candidates

- Item `158` (shared lead-aim helper): `OrbAim` now has 3 callers (`BigAssMaverickRobot`, `GlacierMan`,
  `MechaDragon`).
- Charge art (`charge_windup`, a real `charging` strip) for a proper charge tell (IDEAS move F); the tell uses the
  existing frames today.
- Lava Gargle Mortar (IDEAS move D) as a second ground move.
- A strict NES palette pass on the dragon (open question of `188a`).

## Bug candidates

- `LavaDrop` has no cull: a drop that falls into a pit falls for ever (the dragon destroys its own drips; a
  `LavaDropSupplier` over a pit would not).
