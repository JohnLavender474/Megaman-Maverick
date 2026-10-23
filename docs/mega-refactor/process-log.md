# Process log

Work that went into `master` for the mega refactor. Change this file only on `master`. Add one
entry for each merged or dropped branch, and for each docs-only change to the setup. Put the
newest entry at the bottom.

## Entry format

```
## YYYY-MM-DD — <branch or "master (docs)">

- Summary: what changed and why.
- Contract check: which checks in the branch `docs/CONTRACT.md` were done, and the result.
- Follow-up: new items added to `ITEMS.md`, bugs filed, or open questions.
```

## Entries

## 2026-10-22 — master (docs)

- Summary: set up the docs for the mega refactor in `docs/mega-refactor/`. Added the root
  `CLAUDE.md` with a pointer to this folder, and added `.worktrees/` to `.gitignore`.
- Contract check: not needed (docs only).
- Follow-up: fill in the levels and behaviors in `CONTRACT.md`. Pick the first branch from
  `BACKLOG.md`.

## 2026-10-22 — master (docs)

- Summary: removed the shared `CONTRACT.md`. Each branch now writes its own
  `docs/CONTRACT.md` during planning, before it changes code. Added `CONTRACT.md` to
  `branches/_template/docs/`.
- Contract check: not needed (docs only).
- Follow-up: pick the first branch from `BACKLOG.md`.

## 2026-10-22 — master (docs)

- Summary: added the X6 decomp (`mmx-demake-tools`, `~/mmx-data/x6`) as a source of ideas in
  `CLAUDE.md`. Added the X-style camera bounds item to `BACKLOG.md`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: expanded the camera item in `BACKLOG.md`. It now includes auto-scroll, the X6 facts
  for camera zones and `AutoScroll`, and the research questions to answer before the design.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: added a planning phase for each branch: scan, then `SCOPE.md`, `CONTRACT.md` and
  `PLAN.md`, each approved by the user before the next. Added `SCOPE.md` and `PLAN.md` to the
  branch template. Removed the "In scope" section from the template `TODO.md`. Branch specs and
  plans go in the branch folder, not in `docs/superpowers/`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: added `branches/mega-refactor-maverick-engine/` with the research findings and a
  draft scope for two parts: rename the engine package to a "Maverick Engine" identity, and add
  the engine host contract (`IMaverickGame` and `MaverickGame`). No branch was created, and no
  further work was started.
- Contract check: not needed (docs only).
- Follow-up: the user answers the open questions in the branch `docs/SCOPE.md` and approves the
  scope.

## 2026-10-22 — master (docs)

- Summary: changed the docs to one folder per item. `BACKLOG.md` and `ROADMAP.md` are replaced
  by `ITEMS.md`, an index of every item and branch with status, start and finish dates, and the
  open decisions. Items have 3-digit IDs; branches are `mega-refactor-<id><letter>-<slug>`.
  Created 56 item folders (`000`–`055`) from the old backlog and the review of 46 candidates.
  Each `ITEM.md` has the goal, the proposed scope, the owner's answers (verbatim) and the open
  questions; each `NOTES.md` has the review text. `000` lists the bugs until a bug tracker is
  chosen. The Maverick Engine docs moved to `items/001-maverick-engine/`, split into branch
  folders `001a` (package rename) and `001b` (host contract). Branch folders may now be created
  on `master` ahead of time. Templates moved to `_templates/item/` and `_templates/branch/`.
- Contract check: not needed (docs only).
- Follow-up: the user answers the open decisions in `ITEMS.md` (A–O), and the `idea` items are
  discussed.

## 2026-10-22 — master (docs)

- Summary: recorded the owner's answers to decisions A, B, E (claw), G, H, I, J, K, M and O in
  `ITEMS.md` and in the "Decisions" section of each affected `ITEM.md`. Added open decision P
  (one project or a separate engine). Added the decision K research to `019` and created the
  branch folder `mega-refactor-019a-remove-second-spawn-pass` ahead of time. `001a` is blocked by
  P. The root `CLAUDE.md` now describes the engine as an engine for Mega Man-like games.
- Contract check: not needed (docs only).
- Follow-up: decisions C, D, F, L, N and P; the open question in the `019a` draft scope.

## 2026-10-22 — master (docs)

- Summary: recorded decisions P (one repo, keep the module split, no copyrighted assets in the
  engine), L (no publishing) and N (keep `GameEngine`). Folded the layer-agnostic spawning steps
  into `021`, `022` and `048`. Moved the bug list to `docs/issues/` (25 files and a README with
  the rules and an index) and set `000` to `done`.
- Contract check: not needed (docs only).
- Follow-up: start branch `001a` as the first practice round of the branch process.

## 2026-10-22 — master (docs)

- Summary: added a "Worktrees" section to `CLAUDE.md`. The main checkout stays on `master`, and
  each `mega-refactor-` branch works in its own worktree in `.worktrees/<branch>/`, with the
  commands to create, list and remove one. Branch `001a` moved into a worktree.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: the merge rule now says to squash-merge `mega-refactor-` branches into `master`, to
  move any `.git-blame-ignore-revs` entry to the squash commit, and to delete the branch with
  `git branch -D` after the worktree is removed.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — mega-refactor-001a-package-rename

- Summary: renamed the engine root package from `com.mega.game.engine` to
  `com.maverick.game.engine` (304 files moved, about 11,400 import lines changed), fixed the
  engine Eclipse project name, and added `.git-blame-ignore-revs`. Squash commit `1a61734b5`.
  This was the first practice round of the branch process. During the work, the plan changed:
  the move became its own commit so that every file was a 100% rename. The squash merges them
  again, so for 26 tiny files `git log --follow -M20%` is needed to see their older history.
- Contract check: all automated checks matched the baseline (427 engine and 49 core tests pass,
  the JAR builds, the same 133 compiler warnings); all source checks passed; the owner's play
  test passed (intro stage at 60-61 FPS; start, pause and resume work).
- Follow-up: filed `docs/issues/2026-10-22-jdk25-test-jvm-target.md`. Item `001` continues
  with `001b` (blocked by `016`, `010`, `032`).

## 2026-10-22 — master (docs)

- Summary: removed the "Decisions" section from `ITEMS.md`, so it holds only the items table and
  the branches table. Every decision was already in the "Decisions" section of the `ITEM.md` it
  affects; the open parts of decisions C and F moved into "Open questions" of `003` and `026`.
  `CLAUDE.md` now explains the `ITEMS.md` columns and where decisions and open questions go.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — master (docs)

- Summary: the merge rule now requires the user's explicit approval for each branch, one merge at
  a time; a branch merges `master` in and runs its checks again if `master` changed since it
  started. Started wave 1: `019a` (remove the second spawn pass), `002a` (CI) and `008a` (dev
  launch options), each in its own worktree. The owner accepted the `019a` rare cases.
- Contract check: not needed (docs only).
- Follow-up: plan each wave 1 branch.

## 2026-10-22 — mega-refactor-008a-dev-launch-options

- Summary: added the dev launch options `--screen`, `--level` (staged when the screen is not the
  level screen), `--room`, `--logTags` and `--autoPerf`, with checks that exit with code 2 and the
  usage. `LaunchOptions` is a plain data class; `resolveLaunchOptions` checks and resolves it (the
  owner asked for this split during the work). `--runType test` is a deprecated alias. Fixed the
  `--logLevels` whitespace parse. Squash commit `c731cf9ab`.
- Contract check: clean build; 427 engine and 77 core tests pass (28 new: 15 for the resolver, 13
  for `PlayerSpawnsManager`); 133 compiler warnings as before; the error runs checked by me; the
  owner's play test passed after one fix (`--room` lost its start spawn to spawn `0` on the first
  frame, because `startAt()` put spawn `0` back).
- Follow-up: the launcher part of `docs/issues/2026-10-22-literal-whitespace-regex.md` is fixed;
  the `TODO` in `PlayerSpawnsManager.startAt()` must be revisited in `036`. Later `008` branches
  are listed in `items/008-dev-launch-options/ITEM.md`.

## 2026-10-22 — mega-refactor-019a-remove-second-spawn-pass

- Summary: removed the second `spawnsMan.update(delta / 2f)` after `engine.update`; the remaining
  pass gets the full `delta`. Research (decision K) showed the second pass never gave a
  same-frame spawn and no live entity needed it. A `TODO` names the two accepted rare cases.
  Squash commit `21df34661`.
- Contract check: clean build; 427 engine and 77 core tests pass; 133 compiler warnings as
  before; the owner's play test passed (Desert Man `room13`/`room14`, `room10`; Wily Stage 1
  `room8`, `room3`/`room4`).
- Follow-up: `019b` (destroy notification) is a `planned` branch. The owner's play test showed the
  old misplaced-object errors, filed as `docs/issues/2026-10-22-misplaced-tmx-objects.md`.

## 2026-10-22 — master (docs)

- Summary: relaxed merge step 2 (owner): merge `master` into a branch before its merge only when
  `master`'s new changes touch the same files or systems as the branch. Added step 9: run the
  build and tests on `master` after every squash merge.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-22 — mega-refactor-002a-ci-all-branches

- Summary: one `ci.yml` runs on every branch push and on PRs to `master` (one Gradle job plus the
  `utils` jobs); Kotest 5.9.1 and MockK 1.14.11 from `gradle.properties`; JVM target 17 in every
  compile task; removed `gradleVersion`. Squash commit `52dde921f`. Item `002` is done.
- Contract check: 427 engine and 77 core tests pass with JDK 17, the same with JDK 21; class files
  target Java 17; only one Kotest and one MockK version on the test classpaths; CI run
  `39854839528` passed on GitHub. `SpriteSystemTest` had passed without checking under Kotest 4
  and was fixed. The owner approved the merge.
- Follow-up: JDK 25 stays unsupported (issue updated). The pushed branch
  `origin/mega-refactor-002a-ci-all-branches` can be deleted on GitHub.


## 2026-10-22 — mega-refactor-014a-fixture-type

- Summary: the engine types fixtures as `IFixtureType` (one property, `index: Int`) instead of
  `Any`: `IFixture`, `Fixture`, `FixtureBuilder`, `Body.fixtures`, `IBody.getFixtures`,
  `BodyBuilder`. `FixtureType` implements it (`index = ordinal`). Engine tests use a test enum in
  place of string types. Small change outside the scope: the run scripts pass extra arguments
  through. Squash commit `a7335e90d`.
- Contract check: clean build; 427 engine and 77 core tests pass; 133 compiler warnings as
  before; no fixture type typed `Any` in `engine/.../world/body/`; the owner's smoke run passed.
  The owner approved the merge.
- Follow-up: `014b` (typed fixture receivers) waits for scope review; `014c` (rule table, with
  `FixtureTypeSet`) needs `014a` and `014b`.

## 2026-10-22 — mega-refactor-014b-typed-fixture-receivers

- Summary: one typed receiver registration in place of the hand-written extension trios. Engine:
  `PropertyKey<T>`, `PhasedPropertyKey<T>` and typed `IPropertizable` accessors. Core: `Receivers`
  (17 keys, function types) with `setReceiver`/`receiver`; all 185 registration sites and the 96
  reads in the contact listener and filter use them; the old functions and the unused `hitByLaser`
  kind are deleted. Squash commit `fe5563675`.
- Contract check: clean build; 436 engine (9 new) and 77 core tests pass; compiler warnings 133 to
  115 (the old unchecked casts); the contract greps find no old receiver code; the owner's play test
  passed (Glacier Man: conveyor belts, ice blocks, water, death fixtures; Desert Man dark sections:
  quicksand, carts). The owner approved the merge.
- Follow-up: `014c` (rule table, with `FixtureTypeSet`) is next for item `014`. The
  `setHitByLaserReceiver` part of `docs/issues/2026-10-22-unused-contact-hooks.md` is resolved.

## 2026-10-22 — mega-refactor-014c-contact-rules-table

- Summary: the contact `if/else` chain is replaced by an engine rule table. Engine: `FixtureTypeSet`,
  `IContactRule`, `ContactRulesBuilder`, `ContactRuleTable`, `ContactRuleDispatcher`; `IContactListener`
  takes the two fixtures; the `Contact` type helpers are deleted. Core: 24 rule files and
  `MegaContactRules.kt` (35 registrations), bodies moved unchanged; `MegaContactListener` is deleted.
  Squash commit `4208cb5c7`.
- Contract check: clean build; 454 engine and 79 core tests pass; 114 compiler warnings (115 before);
  `ContactRuleSelectionTest` matches the reference file made from the old chain for all 2523 pair and
  phase lines, and the file is unchanged since the stage-0 commit; 69 of 80 bodies unchanged by script,
  11 checked by hand; the owner's play test passed. The owner approved the merge.
- Follow-up: `014d` (receivers for the `Megaman` checks, `MegaCollisionHandler` body flags,
  `SurfaceRule`, wall-slide flag). `docs/issues/2026-10-22-wrong-contact-log-texts.md` is closed.

## 2026-10-22 — mega-refactor-014d-contact-receivers

- Summary: the player code that the contact rules ran under `is Megaman` moved into Megaman, behind four
  typed receivers (`FEET_BLOCK_CONTACT`, `WATER_CONTACT`, `ICE_FRICTION_FILTER`, `SIDE_ICE_FRICTION`) and two
  capabilities (`IGateTrigger`, `ICartRider`), called at the old points. `FeetSandRule` takes a splash
  supplier. `MegaCollisionHandler` reads two body keys instead of `game.megaman` and the controller.
  `PlayerItemRule` and `BodyPlayerRule` keep the `Megaman` type with `TODO`s for `029`. Squash commit
  `5f90642a9`. Item `014` is done.
- Contract check: clean build; 454 engine and 95 core tests pass (16 new, pinning the call points); 114
  compiler warnings as before; the selection reference file is unchanged; only Megaman registers the new
  receivers and keys; the owner's play test passed. The owner approved the merge.
- Follow-up: `029` re-types `HIT_BY_PLAYER` and `ItemEntity.contactWithPlayer`, and can fold the
  capabilities into `IPlayer`; `031` replaces the input read in Megaman's fall-through key.

## 2026-10-23 — master (docs)

- Summary: `CLAUDE.md` has a new section "Test maps". Manual contract checks use small targeted test
  maps where practical: one map per item (`RefactorTest<id>.tmx`, `LevelDefinition`
  `REFACTOR_TEST_<id>`), one room per check, reached with `--level` and `--room`. A subagent builds
  them with the `level-builder` skill in the first plan steps, and they are checked on the baseline
  code before the change. Small `Test...` entities are allowed when no existing entity can trigger a
  behavior. Real levels stay where the behavior depends on real content or the level type, plus one
  short sanity pass. Test maps stay on `master` for reuse. The owner asked for this.
- Contract check: not needed (docs only).
- Follow-up: `029a` scope and contract use the first test map (`RefactorTest029.tmx`).

## 2026-10-23 — master (docs)

- Summary: test maps must paint their blocks: a `tiles` tile layer with a tile on each block cell,
  because block objects alone are invisible in the game. Added to "Test maps" in `CLAUDE.md` and to
  the `level-builder` skill (iteration 2). The owner asked for this after the first test map had no
  tile layer.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-23 — mega-refactor-029a-player-seam

- Summary: the seam for the player contract. Engine: `PlayerSlot`, `PlayerView` (slot, entity, present),
  `IPlayers`/`Players` with deterministic `inline` helpers (slot order, ties to the lower slot), `slotOf`,
  `registeredSlots`, and `ComponentKey` with `require`; no player assumption beyond slot and entity. Core:
  `MegaPlayer` (requires `BodyComponent`), `MegaPlayers` (capacity 1), `nearestTo`, `game.players` with `Megaman`
  in slot 0, `IMegaGameEntity.players`. The player package reads `this` instead of the global `megaman`. The
  player events carry `PlayerEventKeys.SLOT`; `STUN_PLAYER` carries target `SLOTS`. First test map
  (`RefactorTest029.tmx`, `REFACTOR_TEST_029`) and test entity (`TestStunPulser`); the dev start applies a level's
  override. Squash commit `59f912af9`.
- Contract check: clean build; 475 engine and 102 core tests pass (28 new); 114 compiler warnings, the same list
  as before; the contact selection reference file is unchanged; the contract greps pass (the dead V1 trail
  sprite is the one logged exception); the owner checked every test room on the baseline and again at the end,
  plus `SMB3_1` and `TIMBER_WOMAN`. The owner approved the merge.
- Follow-up: the backlog candidate (delete the dead `MegamanTrailSprite` V1) went into `039` as a known
  candidate, with a note that `029f` must remove it if `039` has not. Next for item `029`: `029b`, `029c` or
  `029d` (in any order).

## 2026-10-23 — master (direct code change)

- Summary: `IComponentBucket.require` threw an `IllegalStateException` whose message said "Entity", but the
  function is on `IComponentBucket`, not on an entity. It now throws a new engine `MissingComponentException`
  (no message; it holds the missing component `type`). `ComponentKeyTest` and `MegaPlayersTest` check the type.
  Made directly on `master` at the owner's request: a small cleanup of a smell in code that `029a` had just merged,
  too small for a branch. `CLAUDE.md` now has a rule for such changes ("Small ad-hoc changes on `master`").
- Contract check: clean build; 475 engine and 102 core tests pass; 114 compiler warnings, as before.
- Follow-up: none.
