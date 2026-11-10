# TODO: mega-refactor-146a-player-marker

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- The last `playerSlot!!` reads on a player: `players/PlayerAirDash.kt:48` (`airDashButton`, receiver
  `AbstractHealthEntity`; the change spreads to the public `airDashInputPressed` and `airDashInputJustPressed`) and
  `entities/megaman/components/ControllerComponent.kt:118` (`060a` changes this file). Bind them to `IPlayer` and read
  `slot`, after `060a` merges.
- Tag changes at runtime (owner, 2026-11-10: advisable). `TagsComponent` has `add` but no `remove` (no caller yet).
  `MegaGameEntities` files an entity under its tags at spawn and removes it under its tags at destroy, so a tag added
  or removed while the entity is live leaves the index out of date. The first caller needs the index to follow the
  component (for example the component tells the index, or the index keeps the tags it filed).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `entities/special/DarknessV4.kt:572`: `while (!lightSourceQueueIter.hasNext())` is inverted. With entries in the
  queue, the loop never runs, so no light source reaches the darkness; with an empty queue, `next()` throws. Same on
  `master` (found by the review before the play test, 2026-11-10). Not checked in play.
- `players/PlayerRules.kt` `playerAllowsDamageFrom` returns `false` for a player that is not live. `WarpSpotTest.hurts`
  uses it, so if `SlotRespawn` tests spots for a dead player, a damager fixture never counts as unsafe. Not confirmed.

- (Fixed in step 12.) `entities/enemies/TellySaucer.kt:102`: `canSpawn` chooses the side from `targetPlayer`, which is `targetFor(this)`
  and reads the saucer's own body. On the first spawn of an instance there is no body (the body is added in `init()`,
  after `canSpawn`), so `targetFor` gives the first live player; on a pooled reuse the body is from the last life.
  With 2 players the saucer can choose the side from the wrong player. Fix: take the position from the spawn
  `BOUNDS` center, which line 101 already reads. Found by the spawn-rules probe, 2026-11-10. Not checked in play.
- (Fixed in step 11.) `entities/MegaEntityFactory.kt:42`: the preload calls `entity.init()` but does not set `initialized = true`, so the
  engine runs `init()` a second time at the first spawn of a preloaded entity (`GameEngine.kt:122`). Each `init()`
  side effect (components, region loads, listeners) runs twice. Found by the spawn-rules probe, 2026-11-10.
- `entities/items/HeartTank.kt:109`: `onSpawn` adds each spawn-on-contact map object to `spawnOnContact`, and nothing
  clears the array (`onDestroy` clears only `lightSourceKeys`). A pooled `HeartTank` that spawns again runs its
  contact spawns twice. Fix: clear `spawnOnContact` in `onSpawn` or `onDestroy`. Same on `master` (found by the third
  review, 2026-11-10). Not checked in play.
