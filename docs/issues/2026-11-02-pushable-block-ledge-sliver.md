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

A block with a support of 16 px or less leaves the ledge at once, at all tiers, with no dependence on rest. A block with more support stays. The value needs the owner's decision.

## How to reproduce

`./run-game-alpha.sh --level RODENT_MAN --room room8`, go to `room9`, stand left of block 219, and shoot it 4 times with no push. The block stops over the hole, about 13 px from the wall. A 5th shot drops it.

## Notes

Depends on `fix/quicksand-two-players`: `PushableBlock.kt` filters the block's own inner block from its feet there, and without that a narrower `FEET` has no effect.

Proposed fix, not approved, in `fix/pushable-block-ledge-sliver` after the quicksand merge: a `FEET` fixture that is 16 px shorter at each end, plus a block filter on the block. With no feet on a block, and a corner overlap of 16 px or less, the filter stops the up push and slides the block by the depth it sank. Only `PushableBlock.kt` changes: 1 constant, 1 changed line and about 8 new lines. Calculated: clear of the ledge in at most 0.107 s, largest x move in 1 step 7.4 px. With this, `velocity.y` can clear at rest, which closes `2026-11-02-pushable-block-velocity-builds-at-rest.md`.

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
