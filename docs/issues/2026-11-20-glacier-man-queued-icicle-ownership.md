---
status: open
verified: yes
area: core
gameplay-change: no
found-by: `mega-refactor-077g-glacier-man` (2026-11-20)
related-items: [077]
---

# Glacier Man takes any queued icicle in its list as its own

## Where

`GlacierMan.isOwnLiveIcicle` (`entities/bosses/GlacierMan.kt`, `TODO` at the code).

## What happens

An icicle in `SPAWN_QUEUED` counts as own with no owner test, because the owner is set only in `onSpawn`. The list
is pruned only in `spawnIcicle`. If a pooled icicle of the boss is destroyed and another owner (`IcicleTelly`)
queues it again before the next prune, a defeat or a destroy of the boss in that frame destroys or shatters that
icicle.

## What should happen

The boss acts only on the icicles that it spawned in their current life.

## How to reproduce

Read the code. No live map puts an `IcicleTelly` near Glacier Man.

## Notes

Found in the review before the play test of `077g`.
