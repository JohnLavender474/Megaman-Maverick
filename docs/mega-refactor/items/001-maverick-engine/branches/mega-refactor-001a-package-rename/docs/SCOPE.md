# Scope: mega-refactor-001a-package-rename

What this branch changes and why. This is a draft from a research pass. The branch does not
exist yet, and the user has not approved this scope. The research is in
`items/001-maverick-engine/NOTES.md`.

- Status: draft
- Approved: no

## Goal

Rename the engine root package to a neutral "Maverick Engine" identity, in one mechanical commit,
while no other `mega-refactor-` branch is active.

## Item

Item `001` (Maverick Engine), the naming and package rename part:

- Rename the root package `com.mega.game.engine` to one neutral root (for example
  `com.maverick.engine`). About 304 package lines and 11,376 import lines change in about 760
  files. Do it in one mechanical commit and add it to `.git-blame-ignore-revs`.
- Keep the class names. Do not add a `Maverick` prefix to every class. Use the prefix only on new
  entry-point types. The package solves collisions with LibGDX and Java (`Animation`, `Timer`,
  `Body`, `Fixture`, `Properties`, `Event`, `Pool`).
- Rename the Gradle module or artifact to `maverick-engine` and fix `eclipse.project.name` in
  `engine/build.gradle:2`. Decide whether `GameEngine` keeps its name.
- Do not rename the `core` package. `EntityType.PACKAGE_PREFIX` builds class names from it for
  TMX spawns.

## Current state

- `engine/` has no notion of a host and no game-specific names. A search for `megaman` or
  `maverick` finds nothing. Only the package root `com.mega` suggests Mega Man.
- The engine is MIT-licensed and has no assets.
- No engine package string is built at runtime. Runtime class lookups build only core package
  strings (`EntityType.kt:20`).
- Counts and file lists are in `items/001-maverick-engine/NOTES.md`, section "Rename cost and
  safety".

## In scope

- Rename the engine root package in `engine/src/main` and `engine/src/test`, and update all
  imports in `core/` and `lwjgl3/`.
- Update the KDoc links and fully qualified names (`LooseQuadtreeWorldContainer.kt:20`,
  `IContactFilter.kt:12`, `:21`, `CullableOnUncontainedTest.kt:14`, `:33`) and the path in the
  root `CLAUDE.md`.
- Rename the Gradle module or artifact base name to `maverick-engine`. Fix
  `eclipse.project.name` in `engine/build.gradle:2`.
- Add the rename commit to `.git-blame-ignore-revs`.
- Optional, if the user decides so: rename `GameEngine` (for example to `EntityEngine`) in the
  same commit.

## Out of scope

- A rename of the `core` package (`com.megaman.maverick.game`). It breaks all TMX spawns.
- A `Maverick` prefix on existing engine classes.
- The host contract. That is branch `mega-refactor-001b-host-contract`.
- Publishing the engine (JitPack, Maven Central, own repo), unless the user adds it.
- Any gameplay change.

## Affected areas and risks

- About 760 files change. Any branch that is active at that time gets a conflict in almost every
  file. Do it when no other branch is active, or freeze the other branches.
- The change cannot change behavior if it only changes package names. Check:
  `./gradlew engine:test core:test lwjgl3:jar` and one game run.

## Overlap with other branches

No `mega-refactor-` branch is active (2026-10-22). Local branch `overhaul_1` is 9 commits ahead
and 957 behind `master`, with no engine changes.

## Open questions

1. Publishing: this decides the root package (`com.maverick.engine` or
   `io.github.johnlavender474.maverick`). Decide before the rename, so that the package changes
   only once.
2. `GameEngine`: keep the name, or rename it in the same commit?
