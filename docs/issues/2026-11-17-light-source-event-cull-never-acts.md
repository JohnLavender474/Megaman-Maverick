---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: `mega-refactor-045b-spawn-allocations` (2026-11-16)
related-items: [045]
---

# `LightSourceEntity` and a `Spike` with `cull_events` never cull on events, and leak one listener per reuse

## Where

`LightSourceEntity.onSpawn` and `Spike.onSpawn` (`cull_events`): `getStandardEventCullingLogic` runs after
`super.onSpawn`, which has already run the `runnablesOnSpawn` entries. A `TODO` is at `LightSourceEntity`.

## What happens

The cullable of this spawn is never added as an event listener, so the event cull (for example on a room change) never
acts. On a pooled reuse, the cullable of the last spawn is added and never removed: one leaked listener per reuse.

## What should happen

The event cull acts on the spawn it belongs to, and no listener stays after the destroy.

## How to reproduce

Read the code. `045b` fixed the same leak in `Block` with `cull_room` (its `CONTRACT.md`, "Intended changes").

## Notes

A fix turns the cull on, so these entities start to cull on events: a gameplay change.
