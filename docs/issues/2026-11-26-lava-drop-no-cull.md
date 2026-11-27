---
status: open
verified: no
area: core
gameplay-change: no
found-by: mega-refactor-188b-dragon-fight (2026-11-26)
related-items: [188]
---

# `LavaDrop` has no cull, so a drop that falls into a pit falls forever

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/hazards/LavaDrop.kt`: `init()` adds the updatables, body, sprites and animations components, and no `CullablesComponent`, although the class implements `ICullableEntity`. Callers: `LavaDropSupplier` (placed in old Inferno Man maps; the live map `InfernoMan_16x14_v3.tmx` has none) and, on branch `188b`, `MechaDragon` (it cleans up its own drops).

## What happens

A `LavaDrop` dissipates only when its `FEET_ON_GROUND` sense is on. A drop that falls past all blocks (over a pit, or out of the room) never senses ground. It has no cull, so it falls and updates for the rest of the session, and it stays damaging and in the engine and its systems.

## What should happen

A drop that leaves the camera or the room is destroyed, as the other hazards do with a `CullablesComponent` and `getStandardEventCullingLogic` (see `LavaDropSupplier`, `AcidGoopSupplier`, `FireballBar`).

## How to reproduce

Found by reading the code. Place a `LavaDropSupplier` over a gap in a room (the supplier is in `InfernoMan.tmx` and the older Inferno Man maps), let a drop fall through the gap, and look at the entity count with `--debugText` or `--diagnostics`: the drop never goes away. Not yet checked in the game (`verified: no`).

## Notes

- `LavaDropSupplier` spawns and holds a `lavaDrop`; a culled drop must not break its `lavaDrop = null` logic (it sets it to null on `dropLavaDrop`, and on its own `onDestroy`).
- The cull must keep the drop alive while it hangs at the spawn point under the supplier (gravity off), and on screen.

## Fix

Branch `fix/lava-drop-cull`. `LavaDrop` gets a `CullablesComponent`:

- the standard event cull (`getStandardEventCullingLogic`: room transition, gate opening, level reset), the same as `LavaDropSupplier`, so a hanging drop and its supplier go away together;
- a camera cull (`getGameCameraCullingLogic`, 1 s out of the view), put by `setToFall()` and removed in `onSpawn`, so only a falling drop is culled for leaving the camera.

What changes: a drop that falls into a pit or out of the room is destroyed after it has been outside the camera for 1 s.

What must not change:

- the hanging drop (gravity off, never culled by the camera), the fall, and the dissipate and splat on `FEET_ON_GROUND` (updatables, sound and smoke puff);
- the timing of `LavaDropSupplier` (no change to the supplier: it drops the reference when it calls `setToFall()`, and a hanging drop is culled only together with the supplier);
- the clean-up of the dragon's own drops on branch `188b`.

Checks:

- Automated (CI): `./gradlew core:test core:smokeTest`. `LavaDropCullTest` fails if the drop has no camera cull after `setToFall()`, if a hanging drop is culled off camera, or if the cull stays after a respawn. `EntityConstructorsTest`, `SpawnAllocationGuardTest` and the `entity-tags.txt` check must stay green.
- Manual: no live or test map has a `LavaDropSupplier` (only the old `InfernoMan*.tmx` maps do, and none is a `LevelDefinition` map), so there is no play check; the unit test and CI are the check.

