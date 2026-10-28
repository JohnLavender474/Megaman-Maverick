---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-064a-instance-identity (2026-10-27)
related-items: [064, 065]
---

# `SpiderWeb.onDestroy` does not call `super.onDestroy()`

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/SpiderWeb.kt:118-132` (the `onDestroy` override).
It is the only `onDestroy()` override in `core/src/main` that skips the `super` call (`MegaGameEntity` is the base).

## What happens

The base `onDestroy` chain does not run for a destroyed `SpiderWeb`:

- it stays in `MegaGameEntities` (tag, type and id lookups still return it);
- its `runnablesOnDestroy` never run (for a pooled entity this includes the pool return; how `SpiderWeb` is fetched is
  not checked);
- its negative `id` is never freed to the random id pool;
- `AbstractProjectile.onDestroy` does not clear `owner`.

## What should happen

`SpiderWeb.onDestroy` calls `super.onDestroy()`, like every other override.

## How to reproduce

Read the code. In the game: fight an entity that shoots `SpiderWeb`s (find the shooter and its room in the live TMX
files) and watch whether old webs keep effects that depend on the registry.

## Notes

Owner decision (2026-10-27): a `fix/` branch adds the call, then a permanent test checks that every `onDestroy()`
override calls `super.onDestroy()`. `064a` kept the old behavior (a `SpiderWeb` never has a group).
