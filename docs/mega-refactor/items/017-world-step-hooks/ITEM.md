# 017: Shared entity logic as systems and world-step hooks

- Status: planned
- Type: refactor
- Depends on: none
- Related: `010`, `015`, `018`, `048`
- Branches: none yet

## Goal

Move repeated per-entity lambdas (gravity, culling, damage blink) into systems and physics step hooks.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Move culling and damage blink to systems that read data components. They run per frame, so this is safe now.
- Add ordered world-step hooks to `WorldSystem`. They run inside each physics sub-step with an explicit `dt`.
- Replace the "feet on ground → gravity" lambdas with a `GravityRule` component and a hook. Gravity stays inside the fixed physics step.
- Keep `body.preProcess` for special cases. Remove the reads of `getPerformance().fixedStep`.

## Owner's input

Answer to review item 16 (2026-10-22, verbatim):

> yes, but let's be careful since certain things (like gravity) are done through the world system's fixed-step process

## Open questions

- Do some entities change gravity rules in the middle of a state (for example bosses)?
- Must water, gravity-change zones and `GravityChange` entities drive the `GravityRule` through the hook?

## Sources

- `NOTES.md` in this folder.
