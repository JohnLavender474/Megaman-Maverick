# Process log: mega-refactor-161a-random-call-lint

## Status

- State: active
- Goal: A type-aware check for direct random calls
- Brief: https://claude.ai/artifact/FKart68KnwoJ2K6RE64dw8 (decisions `D1` to `D4`: Claude's choices, pending owner
  confirmation)
- Done: scan, research, spike, SCOPE/CONTRACT/PLAN, brief, implementation, automated checks, review before the play
  test
- Next: the owner's diff review and confirmation of `D1` to `D5` (no play check)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-16

- Branch start on `master`. The owner asked for the work end to end by a subagent (2026-11-16). Merge target: directly into `master` (Claude's recommendation; the owner can change it).

## 2026-11-16 (subagent, end to end)

- Research: detekt `1.23.8` has Kotlin `2.0.21` inside (no fit for Kotlin `2.3.20`); detekt `2.0.0-alpha.3` has
  `2.3.21`. Konsist has no type resolution. Comparison in `docs/NOTES.md`.
- Order note: the spike (build config and one planted file) ran before `SCOPE.md` and `CONTRACT.md`, because the tool
  choice needed a measured result. No code was committed before the docs.
- Spike: `detektMain` passes on `engine`, `core`, `lwjgl3` (0 issues). A planted file in `core/src/main` failed
  `core:detektMain` with 31 issues, one or more on each forbidden line and none on the allowed lines. The file then
  became the fixture `core/src/detektFixture/kotlin/com/megaman/maverick/game/RandomRuleFixture.kt`.
- Fixture check `core:verifyRandomRule` added; it failed as expected with one config entry removed, and passes with
  the full list.
- `RandomSourceGuardTest` deleted (`D2`). README of the random package, repo `CLAUDE.md` and `ci.yml` updated.
- Brief rendered (one `✗` fixed: a long chip scrolled sideways at 390px) and published.
- Automated checks: `engine:test` 698 tests + `allocationTest` 2, `core:test` 2139, `core:smokeTest` 468, all pass;
  `lwjgl3:jar` builds; `detektMain` (3 modules) and `core:verifyRandomRule` pass. Task times in `docs/NOTES.md`.

## 2026-11-16 (review before the play test)

- One general-purpose review subagent, read-only, no Gradle, no game launch. Scope: the branch diff, the Gradle
  wiring, gaps in the forbidden list, the excludes, docs, `CODE_STYLE.md`, writing rules. Result: no bugs; 2
  should-fix, 4 nits.
- Fixed: `SeededRandom(...)` was not forbidden, so code could make a second, unseeded stream. It is on the list now
  (`D5`); the game's one call has `@Suppress("ForbiddenMethodCall")`; the fixture has it in `forbidden`.
- Fixed: `java.util.random.RandomGeneratorFactory.create` added to the list and to the fixture.
- Fixed: the golden key now has the file name (`RandomRuleFixture.kt:33:19 ...`).
- Fixed: a dead `outputs.upToDateWhen` line in `verifyRandomRule` removed (the task has no outputs, so it always
  runs).
- Fixed (docs): the limits are written in `NOTES.md`, `SCOPE.md` and the random package README: only `.kt` files are
  read, and a call that does not resolve is skipped.
- Deferred to the merge (a `master`-only file): `items/045-pool-ownership/ITEM.md:29` names the deleted test as a
  model (`docs/TODO.md`).
- Brief updated (marked "changed 2026-11-16"), rendered with no `✗`, published again to the same link.
- Automated checks after the fixes: `engine:test` 698, `allocationTest` 2, `core:test` 2139, `core:smokeTest` 468,
  0 failures; `lwjgl3:jar` builds; `detektMain` on 3 modules passes (0 issues); `core:verifyRandomRule` passes with
  33 findings. The fixed parts were read again after the run.
- No play check: the branch changes no game behavior (`CONTRACT.md`, "Manual playthrough").
