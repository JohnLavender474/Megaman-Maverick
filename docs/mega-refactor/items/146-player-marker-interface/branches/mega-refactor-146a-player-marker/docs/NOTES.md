# Notes: mega-refactor-146a-player-marker

## Decisions

## Overlap with other branches

## Overlap with `081f` (review of the tag steps, 2026-11-10)

`mega-refactor-081f-x6-intro-fixes` adds 3 entities written for the old tag API:
`decorations/D1000BombMarker.kt`, `decorations/D1000DebrisWarning.kt` and `hazards/D1000Shockwave.kt`. Each has
`override fun getTag() = TAG` (a compile error without `ITaggable`) and an `init()` with no `super.init()` (no
`TagsComponent`, so `MegaGameEntities.add` throws at spawn). Whichever branch merges second makes the fix: delete the
3 overrides (each `TAG` equals its class name), add `super.init()` to the 3 `init()` overrides, and add the 3 classes
to `core/src/smokeTest/resources/entity-tags.txt`. `git merge-tree` also shows 1 text conflict, the import block of
`bosses/D1000.kt`: keep both imports. `060a`: no conflict, and no tag or `init()` code.

## Spawn flow research (2026-11-10)

4 read-only agents; full notes in `temp/mega-refactor-146a-player-marker/spawn-design/` (not committed).

The engine flow is `spawn()` -> `canSpawn()` -> queue -> next update -> `spawnNow()` -> `init()` on the first spawn
-> `onSpawn()`. So `canSpawn` cannot read `init()` data on the first spawn, and on a pooled reuse it sees the last
life's per-spawn state. Three kinds of data: class-level facts (tags, type, spawn rules), instance setup (`init()`,
the same for every spawn) and per-spawn state (`onSpawn()`).

| Option | First-spawn gap | Stale last-life state | Finding |
|---|---|---|---|
| `init()` when the pool makes the instance | fixed | not fixed | misses 5 direct constructions |
| `init()` at the top of `spawn()` | fixed | not fixed | 1 engine line, covers every path |
| Class-level facts (`021` `EntityDef`) | fixed for tags | not fixed | 446 rows; no reflection on TeaVM |
| `canSpawn` inside `spawnNow` | fixed | not fixed | breaks 4 result readers, a `GauntletMaster` soft lock, 8 count limits |

Facts: no `init()` reads session or level state (2 agents); no spawn path bypasses `engine.spawn`; 27 `canSpawn`
overrides read props, session, the registry or class facts; 4 read or write the instance (`HeartTank`, `HealthTank`,
`TellySaucer`, `Cart`). X6 (`mmx-demake-tools/docs/engine.md`, "Spawning and despawning"): the spawner decides from
record data only, and the object's set-up runs on each spawn into a cleared slot.

Decision (owner, 2026-11-10): layers 1 and 2 in `146a` (`init()` before `canSpawn`; a `canSpawn` contract and the
fixes of the 3 instance users that matter); layer 3 (class facts and spawn rules as data) in `021`.
