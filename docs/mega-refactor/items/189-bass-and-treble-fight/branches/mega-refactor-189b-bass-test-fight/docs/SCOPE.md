# Scope: mega-refactor-189b-bass-test-fight

What this branch changes and why, and its contract. Write this file after the scan and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: draft
- Approved: <date, or "no">

## Goal

## Item

The item (`ITEMS.md` ID) and the parts of its `ITEM.md` that this branch takes, copied here in full.

## Current state

What the scan found: the classes, call sites, events and data involved, how they connect, and
their problems. Use `file_path:line` references.

## In scope

## Out of scope

## Affected areas and risks

The files and systems this branch touches, and what could break.

## Overlap with other branches

## Objective check

The always-on rules in `objectives/README.md` and the rules of the linked objectives that this branch
can affect. Effect: `helps`, `no effect`, `waiting` or `conflict`. A conflict goes to the owner before
approval.

| Rule | Effect | Note |
|------|--------|------|

## Open questions

## Steps

Only for a small branch (see "Planning" in `docs/mega-refactor/CLAUDE.md`). Other branches use `PLAN.md`.

# Contract

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Do the checks before the branch merges, and write the result in the branch process log.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

| Level | What to check |
|-------|---------------|

## Behaviors to check

Behaviors that are not tied to one level, for example the pause menu, death and respawn, game
over, or the save state.

## Intended changes

Gameplay changes that this branch makes on purpose. Leave empty if there are none.
