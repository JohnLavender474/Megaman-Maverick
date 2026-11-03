---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: fix/quicksand-two-players play test (2026-11-02)
related-items: [117]
---

# A `PushableBlock` stays over a hole on a sliver of the ledge

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/blocks/PushableBlock.kt`: the full-width `FEET` fixture, and `preProcess`.
- `engine/src/main/kotlin/com/maverick/game/engine/world/collisions/StandardCollisionHandler.kt:31-43`: the push up or to the side.
- `assets/tiled_maps/tmx/RodentMan_16x14.tmx`: rooms `room9`, `room10`, `room11`. Each of the 5 blocks has a hole of exactly its own width, with a wall on the far side.

## What happens

The owner's words (2026-11-02, verbatim): "In the playthrough of `./run-game-alpha.sh --level RODENT_MAN --room room8`, there was one instance where a block looked like it should fall into the hole, but it levitated, and one more bullet was required from Mega Man in order to trigger it to fall in."

From a read of the code and a calculation, not from a run:

Let `s` be the width of the block that is still on the ledge, and `h` the sink per step. The handler pushes the block up while `s > h`, and sideways into the hole when `s <= h`. `h` grows with rest time, because y velocity grows by 48 px/s per second, so the tolerance depends on the rest and on the tier. At 4 s of rest it is 2.77, 1.80, 1.33 and 0.88 px at `VERY_LOW`, `LOW`, `MEDIUM` and `HIGH`. A sliver of 4 px at `MEDIUM` holds for 12.3 s.

A push can leave any `s`. Each shot moves the block 20.3 to 21.0 px, so 4 shots from the spawn of block 219 leave 12.1 to 14.9 px, and block 301 has 11.0 to 11.7 px after 1 shot.

There is no soft lock: a block on a sliver stays grounded, so a push or a shot moves it. `master` has the same state.

## What should happen

A block with a support of 16 px or less leaves the ledge at once, at all tiers, with no dependence on rest. A block with more support stays. The first fall has a fixed speed.

Owner decisions, 2026-11-02 (verbatim): "go with 16 px and the fixed first fall". So the tolerance is 16 px, and this fix also clears `velocity.y` at rest, after the tolerance is in, and closes `2026-11-02-pushable-block-velocity-builds-at-rest.md`.

## Short contract

Branch: `fix/pushable-block-ledge-sliver`, off `master`, in `.worktrees/fix/pushable-block-ledge-sliver`. This fix also closes `2026-11-02-pushable-block-velocity-builds-at-rest.md`. The dependency in "Notes" is met: `44a4c7e61` has the quicksand fix.

Intended changes:

- `FEET` of a `PushableBlock` is 32 px wide, not 64 px. So the block is grounded only with more than 16 px of support.
- A block filter on the block: with no feet on a solid block, and a corner overlap of 16 px or less, the filter stops the up push and slides the block away from the ledge by the depth it sank. A `PrivateSandFloor` does not count as solid, so a sliver next to sand also ends (found in the final review). The block leaves the ledge at all tiers, with no dependence on rest.
- `velocity.y` clears while the block rests on a feet block, with its bottom at that block's top. A clear at the first `FEET` contact stopped the block in the air above a plate, and the block jittered on the plate in `room9` (owner's play test, 2026-11-02). So the first fall has a fixed speed, and the sink per step at rest no longer grows. The `TODO` at `PushableBlock.kt:213-215` goes.
- From the point above: a block on a support of 16 px or less cannot be pushed back, because `velocity.x` is 0 while it is not grounded (`PushableBlock.kt:185`).
- Block 219 drops on the shot that leaves 16 px or less (4 shots from spawn, not 5). Block 301 drops on 1 shot, not 2.
- A block no longer crosses a level gap of its own width. Rooms 9 to 11 have no such gap on a block's path.

Only `PushableBlock.kt` and `PushableBlockTest.kt` change, and `core/build.gradle` sets the `core` test heap to 1 GB: before this branch the suite already held 521 of 524 MB, so new tests ran out of memory. Production change: 1 new constant, 1 changed `FEET` line, the filter and its function (about 8 lines), 1 line for the velocity clear, and 3 `TODO` lines removed. `MegaCollisionHandler.kt` and `engine/` do not change.

Not changed:

- A block with more than 16 px of support stays, at all tiers. The smallest support at spawn is 32 px (block 301).
- A grounded block rides a `FeetRiseSinkBlock` as it does today, because `FeetBlockRule` moves it by the delta of the block under it. So the clear does not detach it. The plate stays down under the block, and the narrower `FEET` still touches it: each hole is 64 px wide, and its plate is 96 px wide.
- Push by a player, a shot impulse, a `SwitchGate` stop, the room reset, and an entity that rides the block.
- `Megaman` and all other dynamic bodies. The jump pose at a ledge is `2026-11-02-ledge-sliver-jump-pose.md`, in its own branch.

Accepted, with a `TODO` at the code:

- With 2 players, a player on the far side no longer stops a sliver move of up to 16 px, because the `velocity.x` test does not see a translate.
- With support of 16 px or less on both sides of a gap, the two filter calls cancel, so the block has no up push and can sink through both ledges. This needs a gap narrower than the block. On the tile grid it needs a 1-tile gap and exactly 16 px on each side. A block path in `RodentMan_16x14.tmx` has no such gap.
- The filter runs before the one-way block rules in `MegaCollisionHandler.kt`, so a one-way block acts as a solid ledge for this test. No `PushableBlock` in `RodentMan_16x14.tmx`, `RefactorTest029.tmx` or `RefactorTest047.tmx` can reach one. The one `LadderTop` in rooms 9 to 11 is behind the wall of block 308.

Checks, in `PushableBlockTest`, at all 4 fixed steps:

- `s` of 0.5 to 16 px leaves the ledge within 0.15 s after no rest, and after 900 steps of rest. This includes the case with a wall on the far side of the hole.
- `s` of 16.5 to 32 px stays for 900 steps.
- The speed of the first fall is the same after 1 s and 8 s of rest.
- A block that rests on a `FeetRiseSinkBlock` stays on it while it sinks, and the plate stays down.
- A block pushed from a ledge onto sand starts to sink.
- `s` of 0.5 to 16 px next to sand leaves the ledge and sinks within 60 steps.
- All tests in `PushableBlockTest` that exist today pass with no change.
- Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Always-on rules:

- `O5-R2`: `resolveCollisions` with `--diagnostics`, `REFACTOR_TEST_029` room `push_flip`, a capture of about 1000 entries with the block at rest. Before: 1 capture, mean 0.0121 ms, p50 0.008 ms, p95 0.030 ms. After: 2 captures, mean 0.0133 and 0.0147 ms, p50 0.009 and 0.010 ms, p95 0.030 and 0.038 ms. The 1 capture before gives no noise range, so the rise of 0.001 to 0.003 ms is not proved to be from this change.
- `O5-R1`: the filter allocates nothing. It reads floats, and the lambda is made once in `defineBodyComponent()`.
- `O1-R3`: no new order. The filter reads `FEET_ON_GROUND` in the same cycle, because `processContacts` runs before `resolveCollisions`.
- `O1-R4`, `O1-R5`, `O7-R3`: no conflict. No new key, no code with no production caller, and no level check.
- `O2-R5`: the first accepted case above. It is a gap, not a new assumption of one player: the far-side test exists today and only misses the translate.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/pushable-block-ledge-sliver`. Rooms 9 to 11 have no player spawn, so `RODENT_MAN` checks start in `room8`, the nearest room with one.

- `./run-game-alpha.sh --level RODENT_MAN --room room8 --performance low --autoPerf false`: in `room9`, stand left of block 219 and shoot it 4 times. The block drops on the shot that leaves it over the hole, and it does not float.
- `./run-game-alpha.sh --level RODENT_MAN --room room8 --performance high --autoPerf false`: the same steps, with the same result.
- `./run-game-alpha.sh --level RODENT_MAN --room room8`: in `room9`, push block 219 slowly on foot, and stop near the hole. The block stays or drops, and it never floats with more than a thin edge past the ledge in view. Then in `room10`, drop blocks 229 and 279 into their holes. In `room11`, block 301 drops on 1 shot, and block 308 drops into its hole.
- `./run-game-alpha.sh --level RODENT_MAN --room room8`: control. Each block that lands on its plate holds the plate down, the `SwitchGate` of the plate acts as before, and Mega Man can stand on the block and on the plate. Play from `room9` to `room12` with no stuck block.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room sand_entities`: shoot the block so that it stops with a thin edge on the ledge and the rest over the sand. It leaves the ledge and sinks, and it does not bob.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room sand_entities`: control. The block sinks in the sand, carries a rider, and goes when its top is 0.5 tile under.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room push_flip`: control. Push and shoot the block from both sides. It moves as before.

## How to reproduce

`./run-game-alpha.sh --level RODENT_MAN --room room8`, go to `room9`, stand left of block 219, and shoot it 4 times with no push. The block stops over the hole, about 13 px from the wall. A 5th shot drops it.

## Notes

Depends on `fix/quicksand-two-players`: `PushableBlock.kt` filters the block's own inner block from its feet there, and without that a narrower `FEET` has no effect.

Chosen fix, in `fix/pushable-block-ledge-sliver` after the quicksand merge: a `FEET` fixture that is 16 px shorter at each end, plus a block filter on the block. With no feet on a block, and a corner overlap of 16 px or less, the filter stops the up push and slides the block by the depth it sank. Only `PushableBlock.kt` changes: 1 constant, 1 changed line and about 8 new lines. Calculated: clear of the ledge in at most 0.107 s, largest x move in 1 step 7.4 px. With this, `velocity.y` can clear at rest, which closes `2026-11-02-pushable-block-velocity-builds-at-rest.md`.

Sketch, not compiled:

```kotlin
private const val LEDGE_TOLERANCE = 0.5f

GameRectangle().setSize((BODY_WIDTH - 2f * LEDGE_TOLERANCE) * ConstVals.PPM, 0.1f * ConstVals.PPM)

body.addBlockFilter { other, _ -> other is Block && slideOffLedge(other.body) }

private fun slideOffLedge(ledge: IBody): Boolean {
    if (body.isSensing(BodySense.FEET_ON_GROUND)) return false
    val across = min(body.getMaxX(), ledge.getMaxX()) - max(body.getX(), ledge.getX())
    val down = ledge.getMaxY() - body.getY()
    if (down <= 0f || across > LEDGE_TOLERANCE * ConstVals.PPM || across <= down) return false
    body.translate(if (body.getX() > ledge.getX()) down else -down, 0f)
    return true
}
```

Rejected:

- A sensor in the pit: 1 new class and 5 map objects, no cover for a wide pit, and a silent failure if a designer forgets it. A y teleport alone does not help, because the handler needs an overlap height of at least `s` and then adds a sideways jump of `s`.
- A fixed minimum sink: it stops a block at any level seam.
- A wider hole: a hold at `s > 0` stays, and the block wobbles in the hole.

Effects to accept:

- A block no longer crosses a level gap of its own width. 0 sites in maps.
- A block cannot rest on a support of 16 px or less. The smallest support at spawn is 32 px (block 301).
- One shot now drops block 301. Today it needs 2.
- With 2 players, a player on the far side no longer stops a sliver move of up to 16 px. This needs a `TODO` at the code.

Tests, in `PushableBlockTest`, at all 4 steps: `s` of 0.5 to 16 px leaves the ledge within 0.15 s after no rest; `s` of 16.5 to 32 px stays for 900 steps. Measure `resolveCollisions` with `--diagnostics` before and after (`O5-R2`).
