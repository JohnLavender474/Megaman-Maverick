---
status: open
verified: yes
area: core
gameplay-change: no
found-by: `mega-refactor-045b-spawn-allocations` (2026-11-16)
related-items: []
---

# `DemonMet` crashes when its map object has no `target` property

## Where

`DemonMet.onSpawn`: `target.set(targetsPQ.poll())`.

## What happens

With no `target*` property on the map object, `poll()` gives `null`, and the call throws a null pointer error.

## What should happen

Fail fast with a clear message that names the missing property, or use a stated default.

## How to reproduce

Read the code; place a `DemonMet` with no `target` property in a test map.
