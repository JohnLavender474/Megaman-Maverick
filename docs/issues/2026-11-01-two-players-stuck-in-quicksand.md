---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-047a-second-slot play test (2026-11-01)
related-items: [047]
---

# With `--players 2`, two players on one quicksand area cannot jump out

## Where

All code paths are under `core/src/main/kotlin/com/megaman/maverick/game/`. Line numbers are from `mega-refactor-047a-second-slot` at `dfdc64b78`. `FeetRiseSinkBlock.kt`, `QuickSand.kt`, `world/` and `DesertMan_16x14_v4.tmx` have no differences between that branch and `master` at `dc246a0c2`.

- `entities/blocks/FeetRiseSinkBlock.kt:25`: `feetSet`, the one state that all players share
- `entities/blocks/FeetRiseSinkBlock.kt:71-73`: `shouldMoveDown()` and `shouldMoveUp()`, which test only whether `feetSet` is empty
- `entities/blocks/FeetRiseSinkBlock.kt:98-103`: `shouldListenToFeet()`, which accepts each player and `PushableBlock`
- `world/contacts/rules/FeetSandRule.kt:53-56`: the sand friction, which limits a jump in the sand
- `world/contacts/ContactValues.kt:5`: `SAND_FRICTION = 1.15f`
- `assets/tiled_maps/tmx/DesertMan_16x14_v4.tmx`, `WilyStage1_v2.tmx`, `Test1.tmx`: the sand areas

## What happens

The owner saw this in a play test: when both players stand on quicksand in `DESERT_MAN`, neither player can get out.

The stated cause is correct, but not complete. One block holds one height, and one player on it keeps it down. The second half of the cause is the jump: the sand friction makes a jump too short to leave the sand from any depth past 0.65 tiles, so a player needs the block to rise under them.

### From a read of the code

1. Each sand area has one `QuickSand` and one `FeetRiseSinkBlock`, with the same x, width and top. The `QuickSand` does not move, and it has only sprites and a `SAND` fixture. The block has no sprite. It is the floor in the sand.
2. The block stores the feet that touch it in `feetSet` (`FeetRiseSinkBlock.kt:105-113`). With any feet in the set, it sinks at `fall`, which is 0.5 tiles per second. With an empty set, it rises at `rise`, which is 3 tiles per second (`:53-60`).
3. A body that stands on the block moves with it (`FeetBlockRule.kt:62-67`).
4. While the feet touch sand, `FeetSandRule` sets the friction of the body to 36.8 per second (`SAND_FRICTION * PPM`). This also applies during a jump. `JumpBehavior.kt:56` removes the friction for the first world cycle only.
5. One player escapes because of the block. When they jump, `feetSet` goes empty, and the block rises under them. They land higher than they started, and the next jump starts from there.
6. With two players, one player stays in `feetSet` while the other jumps. So the block does not rise, and it continues to sink.

### From a calculation, not from a run

The calculation uses the world step of 1/150 s, `JUMP_VEL`, `JUMP_GRAVITY`, `FALL_GRAVITY` and the friction above. Steps of 1/75 s to 1/225 s give a jump of 0.75 to 0.65 tiles.

| Value | Result |
|---|---|
| Jump height in the sand | 0.67 tiles |
| Deepest start from which a jump reaches the surface | 0.6 tiles reaches it, 0.7 tiles does not |
| Time on the block before a player passes this depth | about 1.3 s |
| Jump from depth 1.0, alone | lands at depth 0.46 after 0.18 s |
| Jump from depth 1.0, the other player stays on the block | lands at depth 1.27 after 0.53 s |

So a jump alone loses depth when the other player stays on the block. Only jumps with an overlap in the air let the block rise. Airtime below the surface is about 0.18 s, so the two jumps must start at almost the same time. This needs a run to confirm.

### The result is death, in most areas

17 of the 20 sand areas have a `Death` sensor 1.5 tiles below the surface, above the `min` depth of 2 tiles. The feet fixture reaches 0.125 tiles below the body, so the feet touch `Death` at depth 1.375, about 2.75 s after a player lands on the surface. These 3 areas have no sensor, and the block stops at `min`: `DesertMan` 656 (`min` 1), `WilyStage1` 408 (`min` 0.5) and `Test1` 192 (`min` 2). In `WilyStage1` 408, the depth is less than one jump, so the trap does not occur there.

- If slot 0 dies, the level resets for both players.
- If slot 1 dies first, its contact ends (`WorldSystem.kt:218`), `feetSet` goes empty, and the block rises under slot 0.

## What should happen

Each player and each sinkable entity sinks and rises on their own, as if alone on the sand (owner decision, 2026-11-01). The sinkable entities are the ones that `shouldListenToFeet()` accepts: players and `PushableBlock`.

Without `--players 2`, nothing changes.

## How to reproduce

Run from the `047a` worktree, or from `master` after `047a` merges:

```
./run-game-alpha.sh --level DESERT_MAN --room room1 --players 2 --logTags FeetRiseSinkBlock
```

Walk both players onto the first sand area (`QuickSand` 76, at x 1184). Stay on it for 2 s. Then jump with one player only. The player does not get out, and the block continues to sink. Then try two jumps at the same time.

## Notes

### Which entities use the block

- Only players and `PushableBlock` can sink it (`FeetRiseSinkBlock.kt:98-103`). Enemies, items and projectiles do not enter `feetSet`.
- Enemies and items only rest on it, or ignore it. `DarkSerket` and `DesertMan` set `STICK_TO_BLOCK` to false (`DarkSerket.kt:276`, `DesertMan.kt:370`). `DarkSerket` turns gravity off on sand (`DarkSerket.kt:375-378`). `AbstractItem` stops on sand (`AbstractItem.kt:188-191`), and `HeartTank` filters the block (`HeartTank.kt:113-116`, `:165`). `Arigock` is `ABSTRACT`, so the block does not hold it.
- Projectiles hit the `SAND` fixture (`ProjectileRules.kt:61-62`). All 25 blocks in the maps have `no_projectile_collision`.
- `PushableBlock` needs the shared state, but not on sand. The 5 `FeetRiseSinkBlock` objects in `RodentMan_16x14.tmx` are plates under `FloorButton` objects, with no `QuickSand`. No `PushableBlock` is on a sand block in any map.

So only sinkable entities need their own block: players and `PushableBlock`. Enemies, items and projectiles do not sink the block, and they need none.

### Measured size

| Thing | Count |
|---|---|
| `QuickSand` objects (each with 1 block) | 20: `DesertMan` 13, `WilyStage1` 5, `Test1` 2 |
| Sand areas with `Death` at 1.5 tiles | 17: `DesertMan` 12, `WilyStage1` 4, `Test1` 1 |
| `FeetRiseSinkBlock` plates with no sand | 5, all in `RodentMan` |
| `InfernoChainPlatform` objects (a subclass) | 23, all in `InfernoMan` |
| Sand objects in other live maps | 0 |

### Recommended fix: one block for each sinkable entity

This is the owner's first idea, keyed by entity and not by slot. It is the only design here that meets the rule in "What should happen" and keeps the single-player code path.

- The map block stays as it is. Slot 0 and all entities that cannot sink use it.
- Each other sinkable entity gets its own copy of the block, with the same x, width, top and spawn props. The copy sinks and rises from the feet of its owner only, with the logic that `FeetRiseSinkBlock` has today.
- A block with an owner does not act on another sinkable entity, and that entity does not act on it.
- With no second sinkable entity on the sand, no copy spawns. So without `--players 2`, the code path is the same as today. The sand does not draw from the block, so entities at different depths look correct.

What the design must solve:

- `addBlockFilter` removes only the collision (`MegaCollisionHandler.kt:28-36`, as in `CameraEdgeBlock`). It does not remove contacts. Each player would still touch the block of the other with `FEET`, `SIDE`, `HEAD` and `BODY`, and with the `CONSUMER` fixture that drives `bodyOverGround` (`BodyComponent.kt:146-147`). So a player could jump in mid-air from the block of the other player. The fix needs a block owner field and one new test in `MegaContactFilter.filter`, a hot path, or a `Receivers.FILTER` on each of these fixtures.
- The copy needs its own lifecycle. Map objects make only one entity per object, so the block must spawn the copy, with a guard against a spawn loop. It must pass on all its spawn props, and each cull and destroy must include the copy. The copy must change when a player joins or leaves (`047f`).
- The copy must exist before its owner reaches the surface. If not, a second entity falls through to the sunken map block. This is open: spawn at level start for each player, or spawn when the feet touch the `SAND` fixture. A `PushableBlock` needs the second way. No map has one on sand, so the first test of it needs a test room.
- It must apply only to sand. A plate in `RodentMan` and an `InfernoChainPlatform` must stay shared, so `FeetRiseSinkBlock` needs a new flag or a subclass, and 20 map objects or a map check must set it.
- It adds 1 body per sand object per extra sinkable entity, on the objective for weak hardware. Spawn on contact limits this to the sand areas in use.

Accepted edge case: an entity that cannot sink rests on the map block, so with 2 players it moves with slot 0 only.

Dependency: the test and the play check need capacity 2 and `--players 2`, so the fix comes after `047a` merges. The fix needs its own contract, because the size and the spawn choice are not settled. This design has no calculation or run yet.

### Alternatives rejected

**Let an airborne player in the sand lift the shared block.** A player in the sand above the block and not on the ground makes the block rise, also when another player stands on it. It changes 1 production file, with 0 map objects and 0 engine changes. The owner rejected it, because the players keep one shared height: a jump by one player lifts the other, so they do not rise and sink on their own.

**Per-body sink state in one block or in `QuickSand`.** One rectangle cannot be at two heights. `StandardCollisionHandler.kt:27-30` and `WorldSystem.collectContacts` read the real shapes. A per-body height needs a new collision path, and a new way to set `FEET_ON_GROUND`, because a feet fixture at another height does not touch the real block. If `QuickSand` takes over and the blocks go, 20 map objects change and the single-player sink has a new implementation, with no proof that it stays the same.

**A rule on the count of bodies.** If the block stops with 2 bodies on it, players past depth 0.65 stay trapped, and in an area with no `Death` nothing ends the trap. If the block rises with 2 bodies, two players float up with no input.

**Leave it.** It is acceptable for `047a`, because it occurs only with `--players 2`. It is not acceptable for co-op in `DESERT_MAN` and `WILY_STAGE_1`, because 17 sand areas become a timed death for both players.

### Relation to `047a`

This does not need to block the `047a` merge. It is a good entry for "Known wrong with `--players 2`" in the `047a` `CONTRACT.md`, with a link to this file. The fix can be `fix/quicksand-two-players` off `master`, after `047a` merges, or a part of a later `047` branch.
