# 164: Move the text-scan guards to detekt

- Status: idea
- Type: tooling
- Objectives: `O6`, `O2`, `O5`
- Depends on: `161`
- Related: `045`, `100`
- Branches: none yet

## Goal

Check the code rules that are text searches today with the type-aware detekt setup of `161`, so that a call written
another way (no receiver, a line break, a helper) cannot pass.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- One rule at a time, each with a fixture and golden findings like `core:verifyRandomRule`:
  - `O2-R1` (input only through `InputFrame`): `Gdx.input` and `controllerPoller` in gameplay code
    (`ForbiddenMethodCall` or `ForbiddenImport`).
  - `PoolOwnershipGuardTest` (`045a`): no pool fetch in a default parameter. The text scan misses a default split over
    2 lines, or one that calls a scratch helper.
  - `SpawnAllocationGuardTest` (`045b`): no constructor call in `onSpawn`, `onDestroy` or `onEvent` of a pooled
    entity, with its baseline file (a `TODO` at the test points here). It may need a custom rule.
- Move from detekt `2.0.0-alpha.3` to a stable `2.0.0` when it is released.

## Owner's input

None yet. From the `161a` `docs/TODO.md` backlog (2026-11-17) and the open questions that `045a` and `045b` added to
item `161`.

## Open questions

- Does a rule that needs data flow (a constructor inside a function body of a pooled entity) need a custom detekt rule
  module, and is that worth it compared to the text scan with a baseline?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/161-random-call-lint/branches/mega-refactor-161a-random-call-lint/docs/NOTES.md` (the
  detekt setup and the tool comparison).
