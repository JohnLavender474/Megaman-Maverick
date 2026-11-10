# `O5`: Plays well on weak hardware

- Status: planned

## Problem

- `P1`: The game must play well on weak hardware, not only on the owner's laptop. Frame drops change how the game
  plays, and they can make it impossible to play for those players.
- `P2`: Some work is wasted on each frame. For example, the physics container is rebuilt about 3.5 times per frame
  (`020`), and Wily 3 draws about 10,000 quads (`046`).
- `P3`: Per-frame allocation causes GC pauses, and the project has no baseline for it (`009`).

## Goal

The game plays well on weak hardware. An optimization does not need proof of a slowdown on the owner's machine before
it is worth doing. Measure to show the gain and to choose between options.

## Done means

- A named weak machine holds the target frame rate in the rooms that cost the most.
- A per-frame allocation number exists for each common gameplay path, and CI or a tool can compare two branches.

## Rules

### `O5-R1`: No per-frame allocation in common paths

- Rule: new code in a common gameplay path does not allocate on each frame.
- Problem: `P3`
- Check: review the branch diff for new collections, lambdas and `props(...)` calls in update paths.
- Applies: `always-on`
- Active: `now`

### `O5-R2`: Measure hot paths

- Rule: a branch that changes a hot path measures it before and after, for example with `--diagnostics`, and writes
  the numbers in its process log.
- Problem: `P1`, `P2`
- Check: read the branch process log.
- Applies: `always-on`
- Active: `now`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O5-R1` | `holds` for reviewed diffs | The last 60 first-parent code commits were searched, and the hits in 14 diffs were read. New per-frame code uses fields, pools or a value class (`CameraController.kt:35-40`, `InputFrame.kt:6`). The search does not see lambdas or boxing, and no runtime number exists. | `009`, `026`, `044` |
| `O5-R2` | `holds` for `134a` (2026-11-09) | The first branch under this rule: 2 `--diagnostics` runs in `REFACTOR_TEST_134` `open`, flag off and on, level frames only: mean 1.57 / 1.58 ms, p95 2.22 / 2.34 ms, `SpritesSystem` 0.04 / 0.04 ms (branch `process-log.md`). Before it: 0 of 54 process logs have before-and-after numbers. `051b` changed the per-frame camera path with no measure. | `009` |
| `O5-R2` | not measured for `134b` (2026-11-09) | The owner approved the merge with no `--diagnostics` capture. The branch adds one function call per drawn entity in `SpritesSystem` and no allocation (review: `O5-R1` holds). | `009` |
| `O5-R2` | accepted gap (`068a`, owner 2026-11-03) | `068a` added slope checks to `MegaCollisionHandler` (every static-dynamic collision in every level) and to `BlockSideRule`, `HeadBlockRule` and the feet contact, with no before-and-after measure. Only one sanity run in `REFACTOR_TEST_068` `shallow`: `resolveCollisions` mean 0.02 ms, `processContacts` mean 0.05 ms, no outlier. | `009` |

## Redo risk

None found. Numbers for merged hot-path work (`014c`, `031c`, `031d`, `051b`, `051d`) do not exist, so this audit cannot exclude a merged slowdown.

## Items

| Item | Status | Note |
|------|--------|------|
| `009` | planned | [Allocation and GC baseline per branch](../items/009-gc-baseline/ITEM.md) (main) |
| `018` | idea | [Event-driven system membership; component IDs when needed](../items/018-ecs-membership/ITEM.md) (main) |
| `020` | planned | [Cheaper broad phase with stable contact order](../items/020-broad-phase/ITEM.md) (main) |
| `026` | planned | [Per-level render layers and an allocation-free draw queue](../items/026-render-layers/ITEM.md) (main) |
| `044` | planned | [Lazy debug and log strings](../items/044-lazy-logging/ITEM.md) (main) |
| `046` | planned | [Backgrounds as pure functions of the camera, with visible-cell drawing](../items/046-backgrounds/ITEM.md) (main) |
| `067` | planned | [PPM 16 with 1x pixel art](../items/067-ppm-16/ITEM.md) (main) |
| `010` | idea | [One simulation clock: fixed tick for gameplay, separate present phase](../items/010-fixed-tick/ITEM.md) |
| `017` | planned | [Shared entity logic as systems and world-step hooks](../items/017-world-step-hooks/ITEM.md) |
| `033` | planned | [Per-level asset manifests and texture budgets](../items/033-per-level-assets/ITEM.md) |
| `045` | planned | [Clear ownership rules and debug checks for `GameObjectPools`](../items/045-pool-ownership/ITEM.md) |
| `116` | idea | [Body senses as typed state, then a sense history](../items/116-body-senses/ITEM.md) |
| `125` | idea | [Spatial or room index for spawners, if a measure shows a cost](../items/125-spawner-index/ITEM.md) (main) |
| `128` | idea | [Cull near players, not only by the camera view](../items/128-cull-near-players/ITEM.md) |
| `134` | done | [Draw bodies and the camera between physics steps (render interpolation)](../items/134-render-interpolation/ITEM.md) |
| `147` | idea | [Render interpolation for riders and for bodies that move once per frame](../items/147-interpolation-riders-frame-moved/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-01: a conflict with `O2-R3` was decided in favor of one fixed tick. Tiers and auto-performance stay, but they
  change only the frame rate and cosmetic budgets. `010` measures the cost on a weak machine first.

## Open questions

- Which weak machine or specification is the reference?
- Which paths are the "hot paths" of `O5-R2`? `009` can give the list.
