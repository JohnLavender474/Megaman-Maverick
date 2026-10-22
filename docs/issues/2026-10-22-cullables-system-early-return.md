---
status: open
verified: yes
area: engine
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [017]
---

# `CullablesSystem` skips culling for the remaining entities after one entity without a cullable

## Where

`engine/.../cullables/CullablesSystem.kt:26`

## What happens

A `?: return` inside the per-entity loop is a non-local return. It exits `process` for all remaining entities in that frame, and also skips `diagnostics?.endEntry()`.

## What should happen

Skip only the current entity (`?: continue` or `return@forEach`).

## How to reproduce

Read the code. An entity in the system without the expected cullable stops culling for every entity after it in iteration order. `CullablesSystemTest` does not cover this case.

## Notes

Fixing it can make some entities cull that do not cull today, so it is a gameplay change. Check the result in play.
