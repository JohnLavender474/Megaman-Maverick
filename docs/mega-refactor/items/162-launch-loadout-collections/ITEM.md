# 162: LibGDX collections in the launch options and level overrides

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `024`
- Branches: none yet

## Goal

`LoadoutOverride` and `ResolvedLaunch` hold `weapons` as an `ObjectSet` (since `024b`), while `heartTanks`,
`healthTanks` and `enhancements` stay Kotlin `Set` and `Map`. One collection style in both types, as
`docs/CODE_STYLE.md` asks.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Move `heartTanks`, `healthTanks` and `enhancements` in `LoadoutOverride` and `ResolvedLaunch` to LibGDX
  collections, with their readers, in one change.
- A `LevelDefinition` entry that holds an override keeps it read-only (no caller changes it at run time).

## Owner's input

## Open questions

## Sources

- Backlog candidate from `mega-refactor-024b-character-loadouts` (`docs/TODO.md`, 2026-11-15).
