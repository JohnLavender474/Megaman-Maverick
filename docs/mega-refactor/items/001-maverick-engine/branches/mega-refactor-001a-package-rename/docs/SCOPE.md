# Scope: mega-refactor-001a-package-rename

What this branch changes and why. The user approves this file before `CONTRACT.md` is written.

- Status: approved
- Approved: 2026-10-22

## Goal

Rename the engine root package from `com.mega.game.engine` to `com.maverick.game.engine`, so that
the engine has its own "Maverick Engine" identity and does not suggest Mega Man by name. Nothing
else changes. This branch is also the first practice round of the branch process.

## Item

Item `001` (Maverick Engine), the package rename part, with the decisions of 2026-10-22:

- Rename the root package. Keep the class names. Do not add a `Maverick` prefix to existing
  classes.
- The engine stays in this repo and is not published (decision L), so the package needs no
  owned domain.
- `GameEngine` keeps its name (decision N).
- Do not rename the `core` package. `EntityType.PACKAGE_PREFIX` builds class names from it for
  TMX spawns.
- Add the rename commit to `.git-blame-ignore-revs`.

## Current state

Scan of 2026-10-22 on `master` at `f595ec73a`:

- 303 source files declare `package com.mega.game.engine...`: Kotlin files in
  `engine/src/main/kotlin/com/mega/game/engine/` and `engine/src/test/kotlin/com/mega/game/engine/`,
  plus one Java file (`engine/.../common/enums/Axis.java`).
- 11,376 lines import `com.mega.game.engine...` in `engine/`, `core/` (main and test) and
  `lwjgl3/`.
- A few fully qualified names outside imports: KDoc links (`LooseQuadtreeWorldContainer.kt:20`,
  `IContactFilter.kt:12`, `:21`) and test code (`CullableOnUncontainedTest.kt:14`, `:33`).
- No string in code names the engine package. No `Class.forName`, `mockkStatic` or
  `qualifiedName` use builds an engine package name at runtime. The only runtime package string
  is the `core` one in `EntityType.kt:20`, which does not change.
- No Gradle file, CI workflow, script, `utils/` tool or project skill names the engine package.
  The Gradle project stays `:engine` in `settings.gradle`, `core/build.gradle:5` and
  `lwjgl3/build.gradle:34`.
- `engine/build.gradle:2` sets `eclipse.project.name = appName + '-core'`, the same name as
  `core/build.gradle:2`. That is a copy-paste error.
- Docs that name the engine path: the root `CLAUDE.md` (module table), and some notes and issues
  in `docs/`.
- No `mega-refactor-` branch is active, so no other branch gets conflicts.

## In scope

- Move `engine/src/main/kotlin/com/mega/game/engine/` and
  `engine/src/test/kotlin/com/mega/game/engine/` to `.../com/maverick/game/engine/` with
  `git mv`, so that the history follows each file.
- Replace `com.mega.game.engine` with `com.maverick.game.engine` in every source file (package
  lines, imports, KDoc and fully qualified names). This is one mechanical change.
- Set `eclipse.project.name = appName + '-engine'` in `engine/build.gradle`.
- Update the engine path in the root `CLAUDE.md`.
- Add `.git-blame-ignore-revs` with the rename commit, so that `git blame` skips it.

## Out of scope

- Any other code change, including the known engine bugs in `docs/issues/`.
- A rename of the Gradle project `:engine` or of the `engine/` folder.
- A rename of `GameEngine` or of any class.
- A rename of the `core` package.
- Docs history: old notes and process-log entries keep the old package name. Issue files in
  `docs/issues/` change only on `master`, so their paths are updated on `master` after the merge.

## Affected areas and risks

- About 760 files change, but only in package and import text. Behavior cannot change if the
  build compiles and no runtime string names the engine package (confirmed by the scan).
- Risk: a missed reference makes the build fail. That is found at once by the compile.
- Risk: IntelliJ caches and run configurations still point to the old paths. The user may need
  to reload the Gradle project.

## Overlap with other branches

None. No other `mega-refactor-` branch is active (2026-10-22).

## Open questions

None open.

- Root package (2026-10-22): `com.maverick.game.engine`. The owner prefers it because it says
  clearly that this is a game engine.
