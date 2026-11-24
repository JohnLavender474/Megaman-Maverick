---
status: open
verified: no
area: core
gameplay-change: yes
found-by: fix/quicksand-two-players (2026-11-02)
related-items: [068, 086, 116]
---

# On the last 1.6 px of a ledge, Mega Man stays up in the jump pose

The owner saw the pose on an ordinary ledge with no sand. It is rare and difficult to do.

Everything in this file comes from a read of the code at `e841cf4d7` (`fix/quicksand-two-players`) and from a calculation. Nothing was run. A statement that says "calculated" comes from a step model of `Body.process`. A statement of what the code does comes from the read.

## Where

All paths are under `core/src/main/kotlin/com/megaman/maverick/game/`, except the engine paths.

- `entities/megaman/components/BodyComponent.kt:91-92`: `FEET`, 0.5 tile wide.
- `entities/megaman/components/BodyComponent.kt:145-166`: the `feetGravity` `CONSUMER` fixture and `bodyOverGround`.
- `entities/megaman/components/BodyComponent.kt:319-332`: the width and the offset of `feetGravity`.
- `entities/megaman/components/BodyComponent.kt:354-364`: the choice of gravity.
- `entities/megaman/sprites/MegamanAnimationKeySupplier.kt:95`: the jump pose.
- `entities/megaman/components/SpritesComponent.kt:80`: the sprite goes 0.25 tile lower.
- `entities/megaman/MegamanAButtonTask.kt:13-23` and `entities/megaman/behaviors/JumpBehavior.kt:49-51`: the `A` button.
- `engine/src/main/kotlin/com/maverick/game/engine/world/collisions/StandardCollisionHandler.kt:31-43`: the push up or to the side.
- `engine/src/main/kotlin/com/maverick/game/engine/world/WorldSystem.kt:164-206`: the order in one cycle.

## What happens

Let `o` be the width of the body that is still over the block. The body is 32 px wide. `feetGravity` is 0.9 tile wide, so it stops 1.6 px from each body edge. `FEET` stops 8 px from each body edge.

| Zone | `o`         | `FEET_ON_GROUND` | `bodyOverGround` | Result                                                                    |
|------|-------------|------------------|------------------|---------------------------------------------------------------------------|
| A    | above 8 px  | true             | true             | Normal ground                                                             |
| B    | 1.6 to 8 px | false            | true             | Ground gravity, stand or run pose, can jump. This is the ledge tolerance. |
| C    | 0 to 1.6 px | false            | false            | Fall gravity, jump pose, sprite 0.25 tile lower, no jump                  |

In zone C, the body sinks by `h` in a step. `StandardCollisionHandler` pushes it up while `o > h`, and it does not change the velocity. When `h` reaches `o`, the handler pushes the body to the side by `o`, away from the block, and the body falls. So zone C ends by itself if the sink per step can reach `o`. It holds with no time limit if friction limits `h` to less than `o`.

Calculated for `o` = 1.6 px, with no x motion:

| Case                           | `VERY_LOW` (1/75 s)         | `LOW` (1/112.5 s)           | `MEDIUM` (1/150 s)          | `HIGH` (1/225 s)            |
|--------------------------------|-----------------------------|-----------------------------|-----------------------------|-----------------------------|
| Normal fall, `FALL_GRAVITY`    | 5 steps, 0.067 s            | 10, 0.089 s                 | 19, 0.127 s                 | 51, 0.227 s                 |
| Moon, `gravity_scalar` 0.35    | 16, 0.213 s                 | 52, 0.462 s                 | holds for `o` above 1.42 px | holds for `o` above 0.94 px |
| Water, gravity `-37.5 * 0.5`   | 29, 0.387 s                 | holds for `o` above 1.36 px | holds for `o` above 1.01 px | holds for `o` above 0.67 px |
| Sand friction, 36.8 per second | holds for `o` above 1.10 px | holds above 0.68 px         | holds above 0.49 px         | holds above 0.31 px         |

The fall speed also builds while the body is held, up to 361 px/s in the normal `HIGH` case. The body takes this speed with it when it leaves the block.

- Moon: 10 `player` spawns in `MoonMan_16x14_v5.tmx` set `gravity_scalar` 0.35. The 23 values of 0.25 in that map are on `PopupCanon` objects.
- Water: the gravity is the same in zones B and C (`BodyComponent.kt:356-359`). In water, `A` gives `SWIM` in zone C, not an air dash.
- Sand: the feet must touch `SAND`. Level-designer Rule 14 puts the sand 1 tile below the blocks, so a map block cannot do this. A `PushableBlock` in sand can, but only `RefactorTest047.tmx` has both of them.
- Snow: not a live case. `FEET` must overlap `SNOW` while it is at least 6.4 px past the block edge. In `GlacierMan_16x14_v2.tmx`, 0 of 28 `GroundSnow` objects overhang a block (script over the map objects).
- One-way blocks: no hold. A body with no feet contact and `getY() < getMaxY()` skips the collision (`MegaCollisionHandler.kt:62`), so it falls through.

The `A` button in zone C gives an air dash. If the air dash is used, `A` does nothing, because `JumpBehavior.kt:51` needs `bodyOverGround`.

A side block changes the zones (`BodyComponent.kt:324-332`). With a block at one side, `feetGravity` moves 3.2 px. Zone C is then up to 4.8 px on the wall side. On the other side, `bodyOverGround` stays true up to 1.6 px past the body edge. During a wall slide, `feetGravity` is 0.65 tile wide, so zone C is up to 5.6 px. These cases need a block position that is not on the tile grid.

## What should happen

Owner's decision is needed. The proposal below is that Mega Man does not stay in zone C: the fall starts at once when he stops there. Zones A and B do not change. The tolerance at every ledge stays.

## How to reproduce

The window is 1.6 px in 32 px, so it takes several tries. The moon case is the one that holds on a real map:

```
./run-mega-man-maverick-alpha.sh --level MOON_MAN --performance high --autoPerf false
```

Stand near a ledge and tap toward it until Mega Man shows the jump pose and does not fall. In a normal level, the same steps give a jump pose of at most 0.227 s (calculated).

If the owner saw a longer pose on a normal ledge in a normal level, this file does not explain it (see "Questions for the owner").

## Notes

### Owner's words

2026-11-02 (verbatim): "I think the 'nudge' fixtures is a reasonable option to explore. Use /subtask to explore and probe the proposal, as well as determine if it should be its own fix/item."

2026-11-02 (verbatim): "I saw it occur only briefly a while ago. It's rare but still occurs, probably in Moon Man's stage in below-MEDIUM performance"

### Proposal b1: a block filter, with no new fixture

Not approved. The proposal `a1` was a new body-width `CONSUMER` fixture and a branch in `preProcess`. The probe found that a nudge works, and that it needs no fixture.

Existing hook: `MegaCollisionHandler.kt:33-36` calls the block filters of the dynamic body once for each block that the body overlaps, in `resolveCollisions`. If a filter returns true, `StandardCollisionHandler` does not run. `AbstractItem.kt:151` and `MoonScythe.kt:173` use this hook. `Megaman` has no filter today.

The nudge does the same translate as the end of zone C today (`StandardCollisionHandler.kt:41-42`), but on the first step. So in any false trigger, the result is what the handler does later anyway, unless the player acts first.

Sketch, not compiled:

```kotlin
// BodyComponent.kt, in defineBodyComponent()
val ledgeBodyRect = GameRectangle()
val ledgeBlockRect = GameRectangle()
body.addBlockFilter { other, _ ->
    other is Block && !bodyOverGround && canNudgeOffLedge(other) &&
        nudgeOffLedge(body, other.body, direction, FEET_GRAVITY_INSET * ConstVals.PPM, ledgeBodyRect, ledgeBlockRect)
}

private fun Megaman.canNudgeOffLedge(block: Block): Boolean {
    val velocity = body.physics.velocity
    val blockDelta = block.body.getPositionDelta()
    val (alongSurface, intoSurface, blockMove) = when (direction) {
        Direction.UP -> Triple(velocity.x, -velocity.y, blockDelta.x)
        Direction.DOWN -> Triple(velocity.x, velocity.y, blockDelta.x)
        Direction.LEFT -> Triple(velocity.y, velocity.x, blockDelta.y)
        Direction.RIGHT -> Triple(velocity.y, -velocity.x, blockDelta.y)
    }
    return alongSurface == 0f && intoSurface > 0f && blockMove == 0f &&
        !isBehaviorActive(BehaviorType.CLIMBING) &&
        !body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
        !block.body.hasAnyBodyLabel(COLLIDE_DOWN_ONLY, COLLIDE_UP_ONLY, PRESS_UP_FALL_THRU)
}

// New plain function, so that a test can call it with bare bodies.
fun nudgeOffLedge(
    body: IBody, block: IBody, direction: Direction, maxOverlap: Float, bodyOut: GameRectangle, blockOut: GameRectangle
): Boolean {
    val a = body.getBounds(bodyOut)
    val b = block.getBounds(blockOut)
    val overlapX = min(a.getMaxX(), b.getMaxX()) - max(a.getX(), b.getX())
    val overlapY = min(a.getMaxY(), b.getMaxY()) - max(a.getY(), b.getY())
    if (overlapX <= 0f || overlapY <= 0f) return false

    val vertical = direction.isVertical()
    val across = if (vertical) overlapX else overlapY
    val down = if (vertical) overlapY else overlapX
    val feetOnBlock = when (direction) {
        Direction.UP -> a.getCenterY() > b.getCenterY()
        Direction.DOWN -> a.getCenterY() < b.getCenterY()
        Direction.LEFT -> a.getCenterX() < b.getCenterX()
        Direction.RIGHT -> a.getCenterX() > b.getCenterX()
    }
    if (!feetOnBlock || across > maxOverlap || across <= down) return false

    if (vertical) body.translate(if (a.getCenterX() > b.getCenterX()) across else -across, 0f)
    else body.translate(0f, if (a.getCenterY() > b.getCenterY()) across else -across)
    return true
}
```

`hasAnyBodyLabel`, `getCenterX` and `getCenterY` are shorthand here. The Triple is shorthand too: `O5-R1` needs plain local values in the real code. `getPositionDelta()` allocates, but it runs only after the `bodyOverGround` gate. It is the read that `FeetBlockRule.kt:35` uses.

Each design question:

- **Side:** the body moves away from the block. The body-center and block-center test gives the side, and it needs no ledge search. A 1-tile pillar gives the correct side, because the test uses the center of the block.
- **Two blocks under him:** any block inside `feetGravity` makes `bodyOverGround` true, so the filter does nothing.
- **A block on each side, and a gap narrower than the body:** not possible on the tile grid. A 1-tile gap fits the body exactly, so `o` can be above 0 on one side only. See "Remaining false triggers".
- **Translate or velocity:** translate. A velocity nudge loses to sand friction (a factor of `exp(-36.8 * dt)` for each step). It also adds run speed that the player did not ask for. A translate is one step, by `o`, so at most 1.6 px.
- **What ends it:** the translate. After it, `body` and `block` touch and do not overlap, and `Intersector.overlaps` is strict. `SIDE` starts 0.35 tile above the feet, so it touches nothing until the body has fallen that far.

### Why not `a1`

| Check                                     | `a1` (fixture and `preProcess`)                                                                                                                           | `b1` (block filter)                                                                                            |
|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| New fixtures                              | 1, added to the 13 of `Megaman`                                                                                                                           | 0                                                                                                              |
| Added grid queries                        | 1 for each step, for each player: 75 to 225 per second (2.5 per frame)                                                                                    | 0                                                                                                              |
| Added contacts                            | 1 or 2 for each step on the ground, each with `CONSUMER` receiver calls                                                                                   | 0                                                                                                              |
| Added call                                | `preProcess` branch on each step                                                                                                                          | 1 lambda call for each body-block overlap, about 1 to 3 in each step. It stops after 2 tests in zones A and B. |
| Value of `bodyOverGround`                 | From the prior cycle                                                                                                                                      | From this cycle, because `processContacts` runs before `resolveCollisions` (`WorldSystem.kt:196-200`)          |
| Block that the body left                  | The set keeps it for 1 cycle after a side push                                                                                                            | The filter runs only on a real overlap                                                                         |
| Fixture sees a block with no body overlap | Yes. A fixture of `feetGravity` height reaches 1.6 px below the body, so it can touch a block while the body is still in the air. It needs a second test. | No                                                                                                             |
| Private sand floor of another player      | Must copy the `Receivers.FILTER` test from `BodyComponent.kt:147-149`                                                                                     | No code. Static filters run first (`MegaCollisionHandler.kt:28-31`).                                           |
| `physics.collisionOn` false               | Needs a test                                                                                                                                              | No code. `MegaCollisionHandler.kt:15` returns first.                                                           |
| Exact `o`                                 | Must compute it from the current bounds                                                                                                                   | Computes it at the collision                                                                                   |
| `O1-R3`                                   | Reads contact data across a cycle                                                                                                                         | Reads it in the same cycle                                                                                     |
| `O1-R4`                                   | A property or a typed field for the set                                                                                                                   | No new key. `BLOCK_FILTERS` exists.                                                                            |

The existing `BODY` contact (`MegaContactRules.kt:31`) can also give the block, through `Receivers.HIT_BY_BLOCK` on the body fixture (`BlockBodyRule.kt:23`, `:33`). It adds no fixture, but contact order is not fixed against `feetGravity` in the same cycle, and the block must wait for `preProcess`. So it has the stale value of `a1`. `NO_BODY_TOUCHIE` blocks also skip it. A world query in `preProcess` also adds no fixture, but it adds one grid query for each airborne step. A "position did not move while the velocity says fall" test needs no block, but then it cannot give the side.

### Why the guard on velocity along the surface

Without it, the nudge breaks a landing. A body that jumps toward a ledge at run speed moves 0.85 to 2.56 px for each step. Its first overlap can be in zone C with a small `h`. Today the handler pushes it up, and the next step puts it in zone B, so it lands. A nudge would push it back on each step until it hits the side of the block.

It also stops a nudge at each run off a ledge, and at each run over a 1-tile pit.

`alongSurface == 0f` is a clean test, because `BodyComponent.kt:284-285` sets a speed below 0.8 px/s to 0. The block friction in `StandardCollisionHandler.kt:32-33` stops a slow creep in a few steps, and then the nudge fires.

### False triggers

"No" means that the code does not reach the translate.

| Case                                         | Triggers                         | Result                                                                                                                                        |
|----------------------------------------------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Jump start at a ledge                        | No                               | The body rises, so `intoSurface` is false, and there is no overlap                                                                            |
| Jump apex at a ledge, with x motion          | No                               | Along-surface guard                                                                                                                           |
| Jump apex, straight down on the last 1.6 px  | Yes                              | He falls at once. Today he falls within the table times. See "Remaining false triggers".                                                      |
| Wall jump, wall slide                        | No                               | Side guard. A wall gives `overlap.height` above `overlap.width`, so the `across <= down` test also stops it.                                  |
| Ladder, climb                                | No                               | `CLIMBING` guard, so a climb down past a corner stays as it is                                                                                |
| `LadderTop`, one-way blocks                  | No                               | Label guard. No hold exists on them.                                                                                                          |
| `CartV2` (a `Block`)                         | Moving: no. Still: yes           | Moving: block guard. Still: as a ledge.                                                                                                       |
| Conveyor                                     | No while the force moves him     | Along-surface guard. The `BODY`-to-`FORCE` contact gives x speed.                                                                             |
| Ice                                          | No while he slides               | Along-surface guard. It fires when he stops, as on a ledge.                                                                                   |
| Moving platform, along the surface           | No                               | Block guard. A landing on the front edge stays as it is.                                                                                      |
| Rising or sinking block, `FeetRiseSinkBlock` | Yes, if in zone C                | He falls off the edge. The handler does this today when `h` reaches `o`.                                                                      |
| Private sand floor of the other entity       | No                               | Static filter first                                                                                                                           |
| Own private sand floor                       | Yes, if in zone C                | As a ledge. The floor spans the sand, so its edges are at walls.                                                                              |
| `PushableBlock`, inner block                 | Moving: no. Still: yes           | As a ledge                                                                                                                                    |
| `CameraEdgeBlock`                            | Yes, only on its top             | Static filter allows a player. The push is small, and the same as today.                                                                      |
| Knockback                                    | No                               | Along-surface guard                                                                                                                           |
| Damage, stun, frozen                         | Yes, if `x` speed is 0 in zone C | He falls. Today he falls within the table times, or 3 to 29 steps with `iceGravity`.                                                          |
| Teleport, spawn                              | No                               | Velocity is 0 and gravity is off (`BodyComponent.kt:340-344`), so `intoSurface` is false                                                      |
| Room transition, camera turn                 | No                               | `gravityOn` is false and velocity is 0 (`Megaman.kt:160-161`, `:400`)                                                                         |
| Rush Jet, air dash, jetpack                  | No                               | Gravity is off and `intoSurface` is false                                                                                                     |
| Ground slide, crouch                         | No                               | Slide: gravity is 0, and the behavior ends at `!bodyOverGround` (`GroundSlideBehavior.kt:49`). Crouch needs `FEET_ON_GROUND`.                 |
| Swim                                         | Rising: no. Sinking: yes         | Sinking in zone C is the water hold. This is the intended case.                                                                               |
| Turned gravity, all 4 directions             | Same as `UP`                     | The code uses rotated bounds and world axes. The feet are at `+x` for `LEFT` and at `-x` for `RIGHT` (gravity signs in `Megaman.kt:167-189`). |
| Slopes                                       | Not today                        | `grep` finds no `Slope` in `core/src/main`. See "Items".                                                                                      |
| 2 players                                    | No shared state                  | One filter on each body. Dynamic bodies do not collide with each other (`MegaCollisionHandler.kt:17-24`).                                     |
| Low tiers                                    | Same                             | At `VERY_LOW` a run step is 2.56 px, so a run can skip zone C.                                                                                |

### Remaining false triggers

Each one is an accepted edge case for the fix, so it gets a `TODO` at the code.

- A body that lands straight down on the last 1.6 px falls at once. Today the player has up to 0.227 s to tap back onto the ledge. It is the only case in which a nudge takes something from the player.
- A gap narrower than the body, with overlap on both sides, makes the filter push both ways. The body then falls through with no up push. It needs a block position that is not on the tile grid, for example a `PushableBlock`.
- A hold on a block that moves along the surface stays as it is today, for example on a moving platform on the moon. The fall still comes if the platform leaves.
- A hold with a side block stays as it is today. It needs a block position that is not on the tile grid.

### Effect

- Normal play: a stopped body in zone C leaves in 1 or 2 steps, in place of 5 to 51 steps (calculated: at most 0.027 s at `VERY_LOW` and 0.009 s at `HIGH`, in place of 0.067 to 0.227 s). A body that moves through zone C does not change.
- Held windows: moon, water and sand all end in 1 or 2 steps, because the translate does not depend on `h`. A translate does not fight the sand friction.
- The fall starts slower than today. The body leaves with the speed of 1 or 2 steps of gravity, not up to 361 px/s.

### Other changes that were considered

- **Pose only:** in `MegamanAnimationKeySupplier.kt:95` and `SpritesComponent.kt:80`, also test `BODY_TOUCHING_BLOCK`. This is about 2 lines and no physics. It hides the pose, but the gravity and the `A` button stay wrong: he stands, and `A` gives an air dash. A press into a wall in the air also sets the sense, and `BlockBodyRule.kt:44` clears it at any block contact end, so it needs a velocity test too. It is smaller, but it does not fix the cause.
- **Do nothing:** a valid choice. In normal levels, the pose ends by itself in at most 0.227 s, and the window is 1.6 px.
- **Rejected before this probe, do not reopen:** a nudge in zone B (it removes the tolerance at every ledge), a wider `feetGravity`, a wider `FEET`, a narrower body bottom.

### Measured size of `b1`

- Production: 1 new file with 1 function of about 25 lines, and about 20 lines in `BodyComponent.kt`, with 1 new constant for the 0.05 tile inset. `MegaCollisionHandler.kt` and the engine: 0 lines.
- Call sites: 1 new `addBlockFilter` (10 lines use it today). 1 new reader of `bodyOverGround` (21 lines today).
- Per frame: 0 new fixtures, 0 new grid queries, 0 new contacts, 0 allocations in zones A and B.
- Overlap: `fix/quicksand-two-players` changes `BodyComponent.kt:147-149`, a different hunk.

### Tests

- `LedgeNudgeTest` in `core/src/test/.../world/collisions/`, with bare `Body` objects as in `MegaCollisionHandlerTest`. It calls `nudgeOffLedge`, in all 4 directions:
  - `o` = 1 px and a small sink: true, and the body ends at the block edge.
  - `o` = 2 px: false, and the body does not move.
  - The block above the body: false.
  - `overlap.height` above `overlap.width` (a wall): false.
- `MegamanLedgeNudgeTest`, with `testServices()` and the `WorldSystem` setup of `QuickSandFloorsTest`, on a body with the real filter. `MegamanRunTest` shows how to build a `Megaman` with chosen components. Whether `defineBodyComponent()` runs under these mocks is not checked. It runs real cycles at 1/225 s, with gravity `-75 * 0.35`, and it asserts:
  - `o` = 1.2 px, x speed 0: the body has left the block within 2 cycles. This fails today, and the calculation says that it holds.
  - `o` = 2 px: the body stays for 300 cycles.
  - `o` = 1 px, moving toward the block: the body lands. It is not pushed away.
  - A one-way block, a moving block, a block with the side sense, and `CLIMBING`: no change from today.
  - After the quicksand merge: the private floor of another entity gives no nudge.
- `core/src/test/CLAUDE.md`: break the code and confirm that each new test can fail.

### Rules for the fix

- `O5-R2`: `resolveCollisions` is a hot path. Measure it with `--diagnostics` before and after, and write the numbers in the short contract.
- `O5-R1`: no `Triple` and no `Vector2` copy before the gate.
- `O1-R3`, `O1-R4`, `O1-R5`, `O2-R5`: no conflict found. Each new API has a production caller in the same branch.
- `O7-R3`: the fix has no level check.

### Own fix or item

This is a bug fix: `fix/ledge-sliver-nudge`, off `master`, with a short contract. It changes one existing behavior and adds no feature. It is not an item.

- Do not put it in `fix/quicksand-two-players`. That contract does not name it, the pose occurs with no sand, and it needs its own named checks.
- Start it after the quicksand merge, because one named check needs `PrivateSandFloor`. There is no other dependency.
- It waits for no item. `084` is `done`. `086` is an `idea`, and this code reads no gravity or movement scalar.

### Items

- `068`: `Slope` extends `Block`, so the filter sees it. A slope branch must exclude `Slope`, in the same way as the one-way labels. O1-R5 stops the fix from adding this now, so it goes in a `TODO`.
- `116`: `isSensingAny` keeps its signature, so no change. The `Open questions` of `116` name coyote time. That is a separate idea: it changes time, and this changes position.
- `079`: the `Met` ledge check looks ahead of an enemy. It has no shared code with this fix.

### Questions for the owner

1. Answered (2026-11-02): brief, and probably in `MOON_MAN` below `MEDIUM`. This agrees with the moon row of the table: at `VERY_LOW` and `LOW`, the pose ends by itself in at most 0.213 s and 0.462 s. No other cause is needed. It is not proof, because nothing was run.
2. Is the fix worth it, against the pose-only change or no change? The pose in normal levels ends by itself, and the holds are only on the moon, in water and in sand.
3. Is the moon hold a defect? A player can stand there on purpose, and it is no use to them. The proposal removes it.
4. Does the loss of the tap-back on a straight landing (first entry in "Remaining false triggers") matter?
