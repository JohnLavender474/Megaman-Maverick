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

## 2026-10-23 — master (direct code change)

- Summary: removed the engine `ComponentKey<T>`, its `get` operators on `IComponentBucket` and `PlayerView`, and
  their tests. `require` moved to `IComponentBucket.kt`; its tests are now `RequireTest`. Reason: `ComponentKey`
  was a design mistake that both the owner and Claude let through the `029a` scope, plan and review. It copied
  `PropertyKey<T>` by analogy, but `PropertyKey` exists because property keys are untyped strings; `KClass<C>` is
  already a typed key, so `ComponentKey(X::class)` added no type safety. Nothing outside its own tests used it.
  It was speculative (a possible later array-storage speed-up). If the engine ever moves to integer component ids,
  that item adds a key type. Made directly on `master` under "Small ad-hoc changes on `master`".
- Lesson: when a new type mirrors an existing one, check that the reason for the original also holds for the
  copy, and do not add an API that no caller in the branch needs.
- Contract check: clean build; 472 engine (3 `ComponentKey` tests removed) and 102 core tests pass; 114 compiler
  warnings, as before.
- Follow-up: `048` no longer names `ComponentKey`.

## 2026-10-23 — mega-refactor-029b-enemy-players

- Summary: every enemy reaches the player through the players service. Core `PlayerStateComponent` (`ready`,
  `damaged`), `MegaPlayer` accessors checked at registration, `targetFor` (slot 0, with a `TODO` for the targeting
  policy) and `IMegaGameEntity.targetPlayer`; engine `viewOf`. In 98 enemy files and `FacingUtils`: reads use
  `targetPlayer`, sensor checks use `players.any`, contact and owner identity use `players.viewOf`; the 6 stomp
  bounces keep Mega's jump velocity with a `TODO` for `029g`. Six enemy test rooms in `RefactorTest029.tmx`.
  Squash commit `fd3c9c471`.
- Contract check: clean build; 473 engine and 108 core tests pass; 114 compiler warnings, the same list as
  before; the contact selection reference file is unchanged; the contract greps pass; a check of the scripted step
  showed all 323 changed lines differ only in the reference; the owner checked the new rooms on the baseline and
  played the rooms, `SMB3_1` and `GLACIER_MAN` at the end. The owner approved the merge.
- Follow-up: bug filed: `docs/issues/2026-10-23-gaping-fish-missing-frozen-region.md`. No backlog candidates.
  `AbstractEnemy` and `AbstractBoss` go to `029c` and `029d`. Next for item `029`: `029c` or `029d`.

## 2026-10-23 — master (direct code change)

- Summary: removed the core helper `nearestTo` and its test. It came with `029a`, but nothing outside its own test
  called it: the targeting design moved "nearest" into a future `TargetPolicy` (`047`), and `029b` uses `targetFor`.
  This is the "no API without a real caller" lesson from `ComponentKey` again. `targetFor` stays a member of
  `MegaPlayers` (owner decision): it is a service behavior that will read the service's own policy state. Made
  directly on `master` under "Small ad-hoc changes on `master`", at the owner's request.
- Contract check: clean build; 473 engine and 107 core tests pass (1 `nearestTo` test removed); 114 compiler
  warnings, as before.
- Follow-up: the engine helpers `forEachPresent`, `firstOrNull` and `minByOrNull` have no game caller yet; they
  are part of the standard set that the owner asked the engine base to give out of the box (decision 2026-10-23),
  so they stay. `forEach` and `all` get their first callers in `029c`.

## 2026-10-23 — master (docs)

- Summary: `CLAUDE.md` ("During the branch") has a new rule: write code only when other production code calls
  it; a test is not a caller. Until a real caller exists, describe the API in the docs or leave a `TODO` at the
  place where it will be needed. At the scope and plan steps, list each new API with its first caller. The owner
  asked for this after `ComponentKey`, `nearestTo` and three engine players helpers turned out to have no caller.
- Contract check: not needed (docs only).
- Follow-up: the engine helpers `forEachPresent`, `firstOrNull` and `minByOrNull` have no game caller; the owner
  decides whether to remove them now.

## 2026-10-23 — master (direct code change)

- Summary: removed the engine players helpers `forEachPresent`, `firstOrNull` and `minByOrNull`, and their tests.
  No game code called them. They were added in `029a` as part of the standard helper set that the owner asked for;
  the new rule "write code only when production code calls it" overrides that. Each can come back with its first
  real caller (for example `forEachPresent` when `047` needs "every present player"). `forEach` and `all` stay:
  `029c` gives them their first callers. Made directly on `master` under "Small ad-hoc changes on `master`", at the
  owner's request. Also: `016`'s `ITEM.md` records the option of a generic `LevelContext<out P : IPlayers<out
  PlayerView>>` (docs).
- Contract check: clean build; 470 engine (3 helper tests removed) and 107 core tests pass; 114 compiler warnings,
  as before.
- Follow-up: none.

## 2026-10-23 — master (docs)

- Summary: "Test maps" in `CLAUDE.md` now says that every check the owner runs has a `--room`, also in real
  levels; a level with several checks is listed once for each, with its own room; for a boss, the room before
  the boss room (the one that holds the boss `Gate`), or the nearest room before it with a player spawn; the boss
  room is not named. The owner asked for this after the `029c` play-test list gave boss levels with no room, and
  corrected a first version that also named the boss room.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-23 — mega-refactor-029c-boss-players

- Summary: bosses, `AbstractBoss` and `AbstractEnemy` reach the player through the players service.
  `PlayerStateComponent` holds `shooting`, `canBeDamaged` and the damage listeners; `MegaPlayer` gets settable
  `facing`, `direction` and `canBeDamaged`, plus `invincible` and `shooting`. `IPlayerDamageListener` replaces
  `IMegamanDamageListener`. The Wily defeat and Moon Man gravity writes change every player; the boss intro
  landing checks use `players.all` (with `047` `TODO`s); profile reads keep the global with `029e` `TODO`s. The
  old bosses are commented out. Named locks were discussed and recorded in the scope, not built. Squash commit
  `c07f62cf7`.
- Contract check: clean build; 470 engine and 110 core tests pass; 114 compiler warnings, the same list as before;
  the contact selection reference file is unchanged; the contract greps pass; a check of the scripted step showed
  all 99 changed lines differ only in the reference. The owner checked 6 of the 10 manual checks on the baseline,
  and played the list at the end. The owner approved the merge.
- Follow-up: no bugs and no backlog candidates. Next for item `029`: `029d` (world entities).

## 2026-10-23 — master (docs)

- Summary: "Test maps" in `CLAUDE.md`: the owner's play test of the manual checks runs once, after the
  implementation; no baseline play-through, because the owner knows the baseline behavior. A short launch still
  checks that each new test room loads on the baseline code. Ask for a baseline run only when a behavior is
  unclear. The owner asked for this at the `029d` contract, which has many checks; its contract text follows.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-23 — mega-refactor-029d-world-players

- Summary: world entities, contact rules and world body code reach the player through the players service.
  `WorldGravity` is the one source of the gravity direction: all seven writers (one found during the work,
  `BodyGravityChangeRule`) go through `gravity.set`, and world-direction readers read it; camera rotation stays in
  the player's setter with a `TODO`. The player view lives on its entity (`asPlayer`), replacing `players.viewOf`
  everywhere. Kill zones act on each player; contact entities work for any player; one dark-room light per player.
  `HIT_BY_PLAYER` takes the view. `MegamanDeath` is renamed `PlayerDeath` (30 TMX objects). `GravityCapsule` and
  `next_body_direction` are commented out. Squash commit `8d536baad`.
- Contract check: clean build; 470 engine and 113 core tests pass; 114 compiler warnings, the same list as before;
  the contact selection reference file is unchanged; the contract greps pass (`PlayerItemRule` is the logged
  `029e` exception); the owner played the full list once, after the implementation. The owner approved the merge.
- Follow-up: the backlog candidate became item `058` (camera rotation as its own mechanism). No bugs. The owner's
  preference that targeting is not global is in `029`'s `ITEM.md` and `NOTES.md`. Next for item `029`: `029e`
  (profile).

## 2026-10-24 — mega-refactor-029e-player-profile

- Summary: `GameState` is the one session profile. Lives move off the player into one shared count (not saved);
  the lives calls take the player's slot, so per-player lives change only `GameState` (`047` `TODO`s). Owned
  weapons are a set in `GameState` (not saved; the reward table moves to `levels/LevelRewards`), and the weapon
  handler reads ownership from it (owner design: read, not copy). The max health is set from the heart-tank count
  in `GameState`. Shared data has no slot; data that can become per-player is keyed by slot (owner option (a)).
  Every profile read goes to `game.state`; the `Megaman` profile wrappers are gone. `ItemEntity` takes the player
  view. Squash commit `eca1b3571`.
- Contract check: clean build; 470 engine and 138 core tests pass (25 new); 114 compiler warnings, the same as
  before; the contact selection reference file is unchanged; the save string is unchanged (pinned by a test that
  passed on the old code); the contract greps pass; a review of each reset path. The owner played the full list
  once, after the implementation. The owner approved the merge.
- Follow-up: the backlog candidate became item `059` (max health read through from the profile). No bugs. Next
  for item `029`: `029f` (level screen, UI, removal of `game.megaman`).

## 2026-10-24 — mega-refactor-029f-level-players

- Summary: the level screen, its spawn, death and end-level handlers, the HUD, the pause screen and the
  get-weapon screen reach the player through the players service: the lead player (slot 0 by name, `047`
  `TODO`s) for level rules, `players.forEach` for level-wide writes, `asPlayer` for identity. `MegaPlayer` gets
  health reads, `spawn`, `focus` and a writable `ready`; Megaman-only reads go through the typed accessor
  `MegaPlayer.megaman` (`024`/`029g` `TODO`). `game.megaman` and the `megaman` extension are removed (40 unused
  imports of it went too). `MegamanTrailSprite` (V1) is commented out. The root `CLAUDE.md` no longer describes
  the global player. Squash commit `6716d074f`.
- Contract check: clean build; 470 engine and 142 core tests pass (4 new); 114 compiler warnings, the same as
  before; the contact selection reference file is unchanged; the contract greps pass; a review of the diff (only
  references changed; handler order and checks unchanged; no lead-player read before the registration). The
  owner played the full list once, after the implementation. The owner approved the merge.
- Follow-up: no backlog or bug candidates. Next for item `029`: `029g` (`CharacterDef`).

## 2026-10-25 — mega-refactor-029g-behavior-split

- Summary: the player's 8 behaviors move out of one 787-line function into 8 factory functions in
  `entities/megaman/behaviors/` (one file each). The bodies moved without change (the air dash names its own
  receiver label). `defineBehaviorsComponent` keeps the ordered `addBehavior` list, with a `024` `TODO` for the
  weapon checks. `TODO`s that named `029g` now name `024` or the Proto Man item. The test map gets the room
  `movement`; the `level-builder` skill and CLI learn ladders (`Ladder`, `LadderTop`, tiles; `add-ladder`).
  The branch was planned as `character-def`. After an independent review, the owner cut it to the split:
  `CharacterDef`, a values table and factory parameters would each have one caller and a guessed shape. The
  design is in the item `029` `NOTES.md` ("`CharacterDef` design"). Squash commit `037a5f1a0`.
- Contract check: clean build; 470 engine and 144 core tests pass (2 new: the behavior order, per-instance
  behavior objects); 114 compiler warnings, the same as before; the contact selection reference file is
  unchanged; a script compared each moved body with `master` (whitespace removed); no change in
  `MegamanValues.kt`, `Megaman.kt` or the other player components. The owner played the full list once, after
  the implementation. The owner approved the merge.
- Follow-up: the backlog candidate became item `060` (Proto Man, with `CharacterDef`). No bug candidates. Item
  `029` is done.

## 2026-10-25 — master (docs)

- Summary: merge step 8 in `CLAUDE.md` now also removes the empty folder that can stay in `.worktrees/` after
  `git worktree remove` (Gradle can leave an empty `.gradle` folder). The 12 empty folders left by earlier
  branches were removed with `rmdir`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-25 — master (direct change)

- Summary: the older ladder in `RefactorTest029.tmx` (id 87, room `enemy_magfly`) gets its `LadderTop` (id 146,
  `blocks`, 32x8 px at the ladder's x and y), as the live stages and the `level-builder` skill do; its tiles were
  already painted. Owner's request after the `029g` merge, which fixed the same gap in the room `movement`. A
  direct change to `master`, not a branch: one object in a test map, no code and no real level. The `029b`
  contract used `enemy_magfly` for `MagFly`; the player can now stand on the ladder top, which does not change
  that check.
- Contract check: the map parses with no duplicate object id; a launch of `--room enemy_magfly` showed no
  exception; build passes; 470 engine and 144 core tests, 0 failures.
- Follow-up: none.

## 2026-10-25 — master (docs)

- Summary: scan and options for item `020` (broad phase) in its `NOTES.md`, from four read-only scans, with the
  owner's feedback (two test suites, Option B and Option D in depth, Option C agreed). The owner put `020` on hold
  for more pressing items; the item stays `planned`. Item `010` `NOTES.md` gets "Trade-offs of doing 010"; its
  `ITEM.md` gets the decision that 60 FPS (`MEDIUM`) is the source of truth for the look and feel, and a new open
  question about simulation work on weak machines. `docs/mega-refactor/CLAUDE.md` gets performance on weak
  hardware as a goal (also in the root `CLAUDE.md`) and the rule for temporary contract test suites.
- Contract check: not needed (docs only).
- Follow-up: filed `2026-10-25-game-line-rotate-origin-y.md` and `2026-10-25-is-performance-subpar-inverted.md`.

## 2026-10-25 — master (docs)

- Summary: item `031` (input frames) gets three owner decisions in `ITEM.md`: keep the NES button names (the frame
  is keyed by button, not intent), a digital threshold for analog sticks (no axes), and at most 2 players on a
  shared screen with 4 slots supported. Its `NOTES.md` gets "Input assignment: cases and options" (12 cases;
  Options A, B, C; sub-choices for disconnect, reconnect and the way out for an unresponsive gamepad).
- Contract check: not needed (docs only).
- Follow-up: the owner decides Option B or C, the single-player disconnect behavior, and the reconnect rule.

## 2026-10-25 — master (docs)

- Summary: item `031` `NOTES.md` gets "Custom key bindings in the proposed setup" (from an agent probe) and the
  owner's answers: per-player maps (also for two pads of the same model), `selectButtonAction` per player, one key
  per button, and the bindings screen goes back to pause. The recommendation is now bindings per slot (Option 4),
  with profiles possible later. `ITEM.md` gets the decisions and the remaining open questions.
- Contract check: not needed (docs only).
- Follow-up: filed `2026-10-25-preferred-controller-code-zero.md` and `2026-10-25-bindings-screen-restarts-level.md`.
  The screenshot trigger change follows as a direct change to `master`.

## 2026-10-25 — master (direct change)

- Summary: the screenshot logic in `MegamanMaverickGame` no longer knows the `P` key. `MegamanMaverickGameParams`
  has `screenshotTrigger: (() -> Boolean)?` in place of `allowScreenshots`. With `--allowScreenshots`, the launcher
  gives `{ Gdx.input.isKeyJustPressed(Input.Keys.P) }`, with a `TODO` that it moves to a system key or a binding
  with item `031`. Owner's request (answer 3 on the `031` bindings questions). A direct change to `master`, not a
  branch: small, no gameplay change (the same key and the same flag), and not part of an active branch.
- Contract check: build passes; 470 engine and 144 core tests, 0 failures; a launch of `TEST_1` with
  `--allowScreenshots` showed no exception.
- Follow-up: the `TODO` in `DesktopLauncher.kt` points to item `031`.

## 2026-10-25 — master (docs)

- Summary: item `031` gets more binding decisions: per-slot settings now with profiles after co-op, `Esc` reserved,
  one constant stick threshold (proposal `0.5`), no revert timer. `Enter` stays an open question (pushback: it is
  today's default `START` key).
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-25 — master (docs)

- Summary: item `031` decisions: reserved keyboard keys (`Esc`, `Enter`, numpad `Enter`, arrows) with `Enter` and
  the arrows as fixed aliases in the keyboard device adapter; `Esc` opens pause in gameplay (with a `TODO`); one
  device per player in co-op, any number in single player; no fixed aliases for gamepads.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-25 — master (docs)

- Summary: item `031`: single player keeps today's behavior when a gamepad is unplugged; the co-op input flows
  (join, claims, disconnect prompt, reconnect) move to `047`, because they have no production caller while
  `MegaPlayers` has capacity 1. The screenshot trigger stays to be decided. Item `047` `ITEM.md` records the moved
  flows.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-25 — master (docs)

- Summary: item `031` decisions: `P` stays the screenshot trigger for now; a `--resetBindings` flag; bindable gamepad
  triggers; `selectButtonAction` defaults to `NEXT_WEAPON` per slot.
- Contract check: not needed (docs only).
- Follow-up: filed `2026-10-25-p-key-binding-takes-screenshots.md`.

## 2026-10-25 — master (docs)

- Summary: item `031`: the touch overlay keeps a fixed layout and is skipped until a mobile build (a `TODO` at the
  touch adapter); no migration of the old binding prefs. Item `034` no longer imports the legacy prefs. All binding
  questions in `031` are answered.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-25 — master (docs)

- Summary: item `031` is `planned` (owner). Its title changes from "Input intents per player (`InputFrame`), sources
  and router" to "Input frames per player (`InputFrame`), devices and bindings", to match the decisions (button
  names, no intents; the co-op router flows moved to `047`).
- Contract check: not needed (docs only).
- Follow-up: the multi-angle scan of `031` goes into its `NOTES.md`.

## 2026-10-25 — master (docs)

- Summary: scan of item `031` from four read-only scans (pipeline and timing, gameplay readers, screens and settings,
  devices with tests and overlaps), with options for the first branch, in its `NOTES.md`.
- Contract check: not needed (docs only).
- Follow-up: the owner picks the first-branch option; six bug candidates are listed in the notes.

## 2026-10-25 — master (docs)

- Summary: the owner chose Option 2 for the first `031` branch and asked to file four bug candidates from the scan.
- Contract check: not needed (docs only).
- Follow-up: filed `2026-10-25-gamepad-index-shift-keeps-old-codes.md`, `2026-10-25-reset-to-defaults-not-saved.md`,
  `2026-10-25-capture-state-left-after-gamepad-disconnect.md` and `2026-10-25-jamepad-version-mismatch.md`.

## 2026-10-25 — master (docs)

- Summary: branch folder `mega-refactor-031a-input-frame` prepared ahead of time (status `planned`, no branch yet),
  with a draft `docs/SCOPE.md` for Option 2. `ITEMS.md` has the branch row.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the draft scope (3 open questions).

## 2026-10-25 — mega-refactor-031a-input-frame

- Summary: one `InputFrame` per player slot (a value class over one `Int`), stored in `MegaPlayers` and read through
  `MegaPlayer.input`, written right after `controllerPoller.run()`. All gameplay input reads moved to it with the
  same method and button: Megaman's 44 reads, `PipePortal` and `SpringBouncer` (contact player), `SpiderWeb` and
  `GroundSlideBlocker` (`targetPlayer`), and pause. The poller stays the only source. At the owner's request,
  `InputFrame` has only `getStatus`, `isPressed`, `isJustPressed` and `isJustReleased`. New test map
  `RefactorTest031`. Squash commit `b73786742`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` (clean build; 470 engine and 157 core tests,
  0 failures; 114 warnings; jar; contact file unchanged; equivalence suite; no gameplay poller reads; the read
  comparison). The owner played every manual check: "All tests pass".
- Follow-up: item `031` stays active; the next branches are `031b`, `031c` and `031d` (item `NOTES.md`). No backlog
  or bug candidates.

## 2026-10-26 — master (direct change)

- Summary: `MegaPlayer` reads only its own slot's input. `3306253a7` gave each view a reader for its slot in place of
  the whole `PlayerInputs` store. That reader was a `() -> InputFrame` lambda, which boxes the value class on each
  read (the bytecode showed `InputFrame.box-impl` in the lambda and `checkcast` plus `unbox-impl` in `getInput`). This
  commit replaces it with `input/InputSource`, a `fun interface` with a non-generic `read()`, which returns a plain
  `Int`. Owner's request, after the review of `031a`. A direct change to `master`, not a branch: two small edits to the
  players view, no change in gameplay. `3306253a7` did not name itself a direct change or log one; this entry covers
  both commits.
- Contract check: build passes; 470 engine and 157 core tests, 0 failures; `lwjgl3:jar` builds; `javap` shows
  `InputSource.read-b3ISFDg()I` and no box or unbox in `MegaPlayer.getInput`.
- Follow-up: `InputSource` is the input type that `031b` uses (see item `031` `NOTES.md`).

## 2026-10-26 — master (docs)

- Summary: the `031b` assessment. Item `031` `NOTES.md` has a new section, "Who drives an entity: driver, slot view
  and side": the owner's "boss becomes playable" case, the three meanings of `asPlayer`, a proposed split (an
  `InputSource` on the entity, a thin slot view, faction), why adding `PlayerStateComponent` to a boss does not work,
  and a boss with two modes. Pointers in `029` and `030` `NOTES.md`. A proposal, not a decision.
- Contract check: not needed (docs only).
- Follow-up: the owner decides the `031b` split (the lock to `012`, `Esc` to a later branch) and the actuator map
  order before the draft `docs/SCOPE.md`.

## 2026-10-26 — master (docs)

- Summary: item `031` `NOTES.md` gets "Long-term shape": the `PlayerStateComponent` review, the two axes (mode and
  source), six design points (with the owner's choices: engine `InputButton` with an explicit `index`, an `InputFrame`
  over a `Long`), and a proposed order of steps. Branch folder `mega-refactor-031b-engine-input` prepared ahead of time
  (status `planned`, no branch yet) with a draft `docs/SCOPE.md`. `ITEMS.md` has the branch row.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the draft scope (4 open questions).

## 2026-10-26 — mega-refactor-031b-engine-input

- Summary: input frames are an engine mechanism. The engine `input/` package has `InputButton` (an explicit `index`)
  with `checkInputButtons`, `InputFrame` (a value class over a `Long`, 2 bits per button, 32 buttons) and
  `InputSource`. `MegaControllerButton` implements `InputButton` (index equal to its ordinal). Each controlled entity
  holds its source on `ControllerComponent`; the slot view binds it. `ControllerSystem` reads each entity's source once
  per update and dispatches in button index order; the actuators lose the poller parameter. `Megaman.input` reads its
  cached `ControllerComponent`, not `asPlayer`. The poller stays the only source of input. Intended change: the
  actuator order is fixed (LEFT, RIGHT, B, SELECT); on `master` it was the hash order of an `ObjectMap`. Squash commit
  `46ad99b55`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` (clean build; 481 engine and 161 core tests,
  0 failures; 114 warnings; jar; contact file unchanged; greps; `javap` shows no boxing in the read path; single
  build point). The branch caches `Megaman`'s `ControllerComponent` for `Megaman.input`. The branch log first gave the
  reason as a `ClassReference` allocation per read; that was wrong (see the correction entry below). The owner played
  the manual checks: "Branch tests pass".
- Follow-up: item `031` stays active; next are `031c` (devices, one edge builder with the lock) and `031d`
  (bindings), and a later menu-input branch (`Esc`, back action, menus and skip screens on frames). No backlog or bug
  candidates. A research agent checked whether components should have keys other than `KClass`; the owner decides
  whether that becomes an item.

## 2026-10-26 — master (docs)

- Summary: correction to the `031b` record. `031b` step 6 said that `getComponent(X::class)` allocates a new
  `ClassReference` on each call because the project has no `kotlin-reflect`. That is wrong: `engine/build.gradle:7`
  declares `api "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"`, and the jar contains `ReflectionFactoryImpl`, so
  `X::class` returns one cached `KClassImpl` with no allocation. Found by a research agent (a benchmark with and without
  `kotlin-reflect`). The cache of `ControllerComponent` in `Megaman` stays: it saves one map lookup per input read
  (`KClassImpl` hashing and `KClass.cast`), not an allocation. The branch process log has the same correction.
- Contract check: not needed (docs only).
- Follow-up: the owner decides whether the component-key research becomes an item.

## 2026-10-26 — master (docs)

- Summary: the owner skips the component-key research for now (no item). Item `031`: the rest of the item splits into
  `031c` (input pipeline, parity), `031d` (devices) and `031e` (bindings), plus a later menu-input branch, with five
  choices for `031c` (decisions of 2026-10-26 in `ITEM.md`). Branch folder `mega-refactor-031c-input-pipeline`
  prepared ahead of time (status `planned`, no branch yet) with a draft `docs/SCOPE.md`. `ITEMS.md` has the branch row.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the draft scope.

## 2026-10-26 — mega-refactor-031c-input-pipeline

- Summary: a new input pipeline with the same behavior. Core `KeyboardInput` and `GamepadInput` give held masks (the
  gamepad adapter keeps `MegaControllerPoller`'s connect, disconnect and prefs logic); slot 0's frame is built from
  their OR by the engine `InputFrameBuilder`, which holds the lock (`TURN_CONTROLLER_*`) with today's sequence. The
  menus and skip screens read `FrameControllerPoller`, a facade over the slot-0 frame, until the menu-input branch.
  The poller, `MegaControllerPoller`, `IControllerButton`, the device reads in `ControllerButton` and the unused
  `enabled` flag are gone; `ControllerUtils` moved to core, so the engine reads no input device. Squash commit
  `d1610c97f`. The squash had one conflict: `input/InputFrames.kt`, deleted on the branch (no caller left) and
  reformatted on `master`; resolved by the deletion.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` (clean build; 489 engine and 165 core tests
  with the temporary suite, 158 after its removal, 0 failures; 114 warnings; jar; contact file unchanged; the temporary
  suite against frozen copies of the old poller on seeded scripts and 6 named cases; greps; `javap` shows no boxing
  or allocation per tick). The owner played the manual checks: "All tests pass".
- Follow-up: item `031` stays active; next are `031d` (devices) and `031e` (bindings), then the menu-input branch.
  No backlog or bug candidates. Note on `629487ce0` ("031 ITEM.md: fix the branches line punctuation"): it was made
  with `git commit -am` and also took in two uncommitted edits from the main checkout (the owner's): a line wrap in
  `input/InputFrames.kt` and `MegaPlayers` passing the slot source as a trailing lambda
  (`MegaPlayer(slot, entity) { inputs[slot] }`, a SAM conversion to `InputSource`). Both are kept.

## 2026-10-26 — master (docs)

- Summary: item `029` `NOTES.md` gets "`MegaPlayer` review and engine patterns": two research reports (the member
  inventory and smells of `MegaPlayer`; how other engines and games give characters a common interface) and the owner's
  questions (no required body; interfaces versus components and functions; immediate events; global functions). A
  proposal, not decided.
- Contract check: not needed (docs only).
- Follow-up: a third research report (functions, components and events instead of classes) goes in `029` `NOTES.md`.

## 2026-10-26 — master (docs)

- Summary: item `029` `NOTES.md` gets "Functions, components and events instead of classes": the third research
  report (free functions and systems in Bevy, flecs, EnTT, Unity DOTS, Sonic Mania, Celeste; locks without base
  classes; immediate versus deferred events with a comparison table; querying a player without an interface;
  static/global functions and their risks; a translation to this codebase; sources). It also records a finding about
  `EventsManager`: a listener gets events in the hash order of its `eventKeyMask`, not in submit order (checked in
  `EventsManager.kt:71-76`). A proposal, not decided.
- Contract check: not needed (docs only).
- Follow-up: the owner decides the direction for `MegaPlayer` (thin view, free functions over components, counted
  locks, a synchronous signal registry) and its order against `031d`.

## 2026-10-26 — master (docs)

- Summary: new item `061` (thin player view; locks and signals as components and functions), from the owner's
  experimental branch `temp-thin-player`, which the owner tested ("Tests pass"). Branch folder
  `mega-refactor-061a-thin-player` prepared ahead of time (status `planned`) with draft `SCOPE.md`, `CONTRACT.md` and
  `PLAN.md` for one review. `ITEMS.md` has the item and branch rows.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the three drafts.

## 2026-10-26 — master (docs)

- Summary: item `061` `NOTES.md` gets "`ready` and `teleporting` for any entity": a research report (teleporting and
  `ready` today, the concepts behind `ready`, other engines, options, a proposed `061b`). A proposal, not decided.
- Contract check: not needed (docs only).
- Follow-up: the owner decides whether `061b` takes the lifecycle and transport components.

## 2026-10-26 — mega-refactor-061a-thin-player

- Summary: a thin `MegaPlayer` (`slot`, `entity`, `input`; nothing required at registration) with its former members
  as free functions read when called; player parts as components (`PlayerComponent`, `PlayerLightComponent`,
  `GravityDeviceComponent`); engine locks (`LockGroup`, `LockReason`, `LocksComponent` with a change hook, free
  functions; the game supplies `MegaLockGroup`, `MegaLockReason`, `behaviorsLockHook`) in place of `canBeDamaged`,
  movement, `allowMovement` and `setAllBehaviorsAllowed`; an engine `Signal<T>` for the player-damaged notification.
  Built first on the experimental branch `temp-thin-player` (owner's request), then merged into this branch with the
  cleanup (locks to the engine, test leftovers, import order). Intended changes: lock overlaps only (damage, movement
  with a spider web, behaviors at the level end). Squash commit `587a53f95`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` (clean build; 501 engine and 159 core tests,
  0 failures; 114 warnings; jar; contact file unchanged; lock, signal and player tests; greps). The owner played the
  manual checks: "Play tests pass".
- Follow-up: item `061` stays active; next `061b` (lifecycle and transport components, no player wrappers), then a
  branch that moves the `MegaPlayerAccess.kt` callers to entity-level functions (decisions of 2026-10-26 in `ITEM.md`).
  No backlog or bug candidates. `temp-thin-player` and its worktree are deleted. The double space in the item `061` row
  of `ITEMS.md` is fixed.

## 2026-10-26 — master (docs)

- Summary: branch folder `mega-refactor-061b-play-phase-transport` prepared ahead of time (status `planned`, no branch
  yet) with a draft `docs/SCOPE.md`. `ITEMS.md` has the branch row.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the draft scope (2 open questions: the name `inPlay`, the package).

## 2026-10-26 — master (docs)

- Summary: item `061` `NOTES.md` gets "'Lifecycle' versus spawning, spawned and dead": a review of the draft
  `Lifecycle` name for `061b` (three axes: existence, play participation, health death; timelines for the player, a
  boss and an enemy; other engines; options). The review recommends a rename to `PlayPhase { PRE_PLAY, IN_PLAY,
  POST_PLAY }` and no consolidation, and finds that `dead` stays `false` after `engine.reset()`/`dispose()`. A
  proposal, not decided.
- Contract check: not needed (docs only).
- Follow-up: the owner decides the name and whether the `dead`/`spawned` disagreement becomes an item or an issue.

## 2026-10-26 — master (docs)

- Summary: the owner chose `PlayPhase { PRE_PLAY, IN_PLAY, POST_PLAY }` for `061b` (decision in item `061`
  `ITEM.md`); the draft scope is updated, and the branch folder is renamed to `mega-refactor-061b-play-phase-transport`
  (no branch existed yet; `ITEMS.md` follows).
- Contract check: not needed (docs only).
- Follow-up: the owner approves the `061b` scope (1 open question: the package). A review of removing the `spawned`
  and `dead` flags is running.

## 2026-10-26 — master (docs)

- Summary: new item `062` (one engine-owned entity existence state), status `idea`. The owner asked to remove the
  `spawned` and `dead` flags and make callers ask the engine, and to make an item if a review found it solid. The
  review (item `NOTES.md`) found the goal solid and recommends one engine-owned `EntityState` field with read-only
  helpers instead of engine queries or a global helper; the owner decides (7 open questions in `ITEM.md`).
- Contract check: not needed (docs only).
- Follow-up: the owner reviews item `062`.

## 2026-10-26 — master (docs)

- Summary: `061b`: the owner chose `TeleportableComponent` (decision in item `061` `ITEM.md`) and approved the plan;
  the folder is renamed to `mega-refactor-061b-play-phase-teleport`; branch start (the `ITEMS.md` row and `ITEM.md`
  set to active).
- Contract check: not needed (docs only).
- Follow-up: the branch work in its worktree.

## 2026-10-26 — mega-refactor-061b-play-phase-teleport

- Summary: the player's `ready` and `teleporting` became entity state in `core/.../entities/state/`:
  `PlayPhase { PRE_PLAY, IN_PLAY, POST_PLAY }` in `PlayPhaseComponent` (read by `playPhase`/`inPlay`, `IN_PLAY` without
  the component; written by `setPlayPhase`, which requires it) and `TeleportableComponent` (the carrying teleporter;
  `teleporter`/`teleporting`; `beginTeleport`/`endTeleport` call the entity's hooks first, then set or clear it). The
  level handlers and the three teleporters write them; every reader asks the entity, with no player wrapper.
  Removed: `Megaman.ready`/`teleporting`, `MegaPlayer.ready`/`teleporting`, `PlayerStateComponent`,
  `ConstKeys.TELEPORTER`, `AbstractEnemy`'s unused teleport hooks and their helpers. No intended changes. Squash
  commit `380acbdc9`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` (clean build; 501 engine and 167 core tests, 0
  failures; 110 warnings; jar; contact file unchanged; play phase and teleport tests including the hook order; greps;
  the one-for-one read check). The owner played the manual checks: "Play tests pass".
- Follow-up: item `061` stays active; next, a branch that moves the `MegaPlayerAccess.kt` callers to entity-level
  functions and deletes the file. No backlog or bug candidates. Item `062` (entity existence state) waits for the
  owner's decisions.

## 2026-10-26 — master (docs)

- Summary: item `062`: the owner chose the engine-owned `EntityState` field (open question 1 closed); the item is
  `planned`. The owner picked the next `061` branch (the `MegaPlayerAccess.kt` callers to entity-level functions) as
  the next work.
- Contract check: not needed (docs only).
- Follow-up: the scan for that branch.

## 2026-10-26 — master (docs)

- Summary: branch folder `mega-refactor-061c-entity-queries` prepared ahead of time (status `planned`) with a draft
  `docs/SCOPE.md`. `ITEMS.md` has the branch row.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the draft scope (4 open questions).

## 2026-10-26 — master (docs)

- Summary: `061c`: the owner chose to replace the player view class with a `PlayerComponent(slot)` and a slot → entity
  registry (decision in item `061` `ITEM.md`); the draft scope is rewritten to include it.
- Contract check: not needed (docs only).
- Follow-up: the owner reviews the rewritten draft scope (4 open questions).

## 2026-10-26 — mega-refactor-061c-entity-queries

- Summary: a player is an entity with an engine `PlayerComponent(slot)`; `Players` is a slot → entity registry, and
  `MegaPlayers` binds the input source at registration. Callers ask the entity through query functions on
  `IGameEntity` (engine: `body`, `facing`, `direction`, behaviors, `input`; core: health, damage, fixtures, light,
  gravity device, `shooting`). `MegaPlayer`, `PlayerView`, `MegaPlayerAccess.kt` and the `megaman` accessor go (explicit
  `as Megaman` casts remain for items `024`/`060`). Squash commit `a5dd1d99e`.
- Contract check: clean build, 505 engine and 160 core tests pass, warnings unchanged (110), `contact-rule-selection.txt`
  unchanged, greps, `javap` and the one-for-one check pass; the owner's play test passes (all manual checks).
- Follow-up: item `061` is done. No backlog or bug candidates. Next options: item `062`, `031d`.

## 2026-10-26 — master (direct change)

- Summary: `Players.register` rejects an entity that is already a player (before, a second `register` replaced its
  `PlayerComponent` silently and left the entity in two slots), and the `PlayerComponent` constructor is `internal`, so
  only the engine registry creates the component. A direct change to `master`, asked for by the owner: a small fix to a
  smell in the code that `061c` just merged, with no gameplay change (the one caller registers Megaman once).
- Contract check: build and tests on `master`: 506 engine and 160 core tests pass (one new `PlayersTest` case), jar
  builds.
- Follow-up: none. Other code can still call `removeComponent(PlayerComponent::class)`; not guarded.

## 2026-10-26 — master (docs)

- Summary: branch start of `mega-refactor-062a-entity-state` (item `062` is `active`).
- Contract check: not needed (docs only).
- Follow-up: scan, then the draft scope.

## 2026-10-26 — master (docs)

- Summary: item `062` decisions from the `062a` scope review (the state name, the true `dead`, the reset fix, the order
  with `019b`); a new branch `062b` for engine guards on the odd spawn and destroy paths, so the readers move to
  `062c` and the removals to `062d`.
- Contract check: not needed (docs only).
- Follow-up: the owner approves the `062a` scope.

## 2026-10-26 — mega-refactor-062a-entity-state

- Summary: one engine-owned `EntityExistenceState` on each entity, written only by `GameEngine` on every path;
  `spawned` and `dead` are read-only values derived from it, and `dead` is the true value on every path. The
  `onQueueToSpawn`/`onQueueToDestroy` callbacks go. The odd spawn and destroy paths keep their actions and log under
  the `GameEngine` tag. Three intended changes (dead after `reset`/`dispose`; a spawn while a destroy is queued; a
  spawn and then a destroy in the destroy phase). The `Met` in `RefactorTest029` `enemy_aim` moved left so it can
  respawn. Squash commit `a30936fa4`.
- Contract check: a temporary old-versus-new suite (deleted before the merge) showed the same actions, return values
  and `spawned` on 10,000 random sequences, and the new `dead` true in 611,549 checks; the two core tests failed on the
  baseline and pass after; 14 permanent engine tests; warnings unchanged (110); `contact-rule-selection.txt`
  unchanged; the owner's play test passes (six checks, with `--logTags GameEngine`: no odd-path line).
- Follow-up: `062b` (the guards) needs a wider search for callers of the odd paths (branch `docs/NOTES.md`). No
  backlog or bug candidates.

## 2026-10-27 — master (docs)

- Summary: branch start of `mega-refactor-062b-spawn-order-guards`.
- Contract check: not needed (docs only).
- Follow-up: scan, then the draft scope.

## 2026-10-27 — master (docs)

- Summary: branch start of `mega-refactor-019b-destroy-notification`, in parallel with `062b` (both change the engine spawn and destroy paths; the
  overlap goes in the branch `docs/NOTES.md`).
- Contract check: not needed (docs only).
- Follow-up: scan, then the draft scope.

## 2026-10-27 — mega-refactor-062b-spawn-order-guards

- Summary: engine guards on the order of spawn and destroy calls: `onSpawn` and `onDestroy` alternate, and the last
  accepted call wins. A spawn of a live entity and a destroy of an unspawned entity are refused; a spawn while a
  destroy is queued is a respawn (new state `RESPAWN_QUEUED`, run after the destroy phase); a destroy waits for a
  queued spawn. Intended changes: the ice shield after a same-frame thaw and freeze; a child's `onDestroy` runs once
  when it and its parent die in the same frame. Squash commit `bcbc42c48`.
- Contract check: a temporary old-versus-new suite with a rules oracle (deleted before the merge): 4,490 clean
  sequences the same as before; 742,744 checks of the last accepted call on the four odd paths, no alternation break,
  no exception; the two core tests failed on the baseline and pass after; warnings unchanged (110);
  `contact-rule-selection.txt` unchanged; the owner's play test passes (eight checks, no `GameEngine` log line).
- Follow-up: bug filed: `docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`. `019b` (active) can now do its
  engine change on the new spawn and destroy paths (overlap in the `062b` `docs/NOTES.md`). Item `062` next: `062c`.

## 2026-10-27 — mega-refactor-019b-destroy-notification

- Summary: engine hooks `onSpawnQueued()`/`onDestroyQueued()` on the entity, called when a `spawn`/`destroy` call
  changes "live after the next update" (`onSpawnQueued` has no game caller, by the owner's decision). Spawners put a
  release runnable on their entity (`runnablesOnDestroyQueued`) and no longer poll `dead`; bounds spawners record the
  camera overlap at the destroy call; one shared spawn camera rectangle per frame; `SpawnsManager.reset` resets its
  spawners; the `019a` `TODO` goes. Intended changes: the two rare `019a` respawn cases and the pooled-reuse hold.
  Squash commit `7a30c8982`.
- Contract check: a temporary old-versus-new engine suite (deleted before the merge) showed no change in engine
  actions, return values or states on 10,000 sequences; 12 hook tests and 9 spawner tests (the three intended-change
  tests failed on the baseline and pass after); warnings unchanged (110); `contact-rule-selection.txt` unchanged; the
  owner's play test passes (six checks).
- Follow-up: `019c` (merge the four spawn queues) recorded in item `019` as its next branch. Bug filed:
  `docs/issues/2026-10-27-wily-stage-1-duoballcanon-on-blocks-layer.md`.

## 2026-10-27 — master (direct change)

- Summary: `IMegaGameEntity` extends the engine `IGameEntity`, so the game entity contract inherits the engine entity
  contract. Both implementers (`MegaGameEntity`, `IProjectileEntity`) were already engine entities, so this only states
  a fact the compiler did not know; no runtime change. A direct change to `master`, asked for by the owner: one line and
  one import, no gameplay change, and it belongs to no active branch.
- Contract check: clean build and tests on `master`: 539 engine and 172 core tests pass, 110 warnings (as before), jar
  builds.
- Follow-up: code that holds an `IMegaGameEntity` can drop casts to `MegaGameEntity` for engine members (for example
  `overlapsGameCamera`); not done here.

## 2026-10-27 — mega-refactor-062c-existence-readers

- Summary: the existence questions are properties of the state type, `EntityExistenceState(liveNow,
  liveAfterUpdate)` with one constructor value per state (owner decision: no more flags on the entity). Every reader
  of `IGameEntity.spawned` and `MegaGameEntity.dead` (about 110 reads in `engine` and `core`) moved to
  `existenceState.liveNow` / `!existenceState.liveAfterUpdate` with the same value on every path; no player-level
  `dead`; both old getters removed; five casts that existed only for `dead` removed. The root `CLAUDE.md` no longer
  names `dead`. Squash commit `32c0a23d3`.
- Contract check: a temporary suite (deleted before the merge) showed the new values equal the old flags in all five
  states; a substitution check script matched 99 renamed lines and listed 25 expected ones (the enum, the removed
  getters, `toString`, two imports, the cast sites), each reviewed; the compiler proves no reader was left; clean
  build, 540 engine and 172 core tests pass; warnings unchanged (110); `contact-rule-selection.txt` unchanged; the
  owner's play test passes (nine checks).
- Follow-up: new item `063` (entity contracts are entity types; `destroy()` on `IGameEntity`) from the backlog
  candidate and two review agents. Bug filed: `docs/issues/2026-10-27-starkner-alive-cleanup-only-in-destroy.md`.
  Item `062` next: `062d` (`engine.contains(..., containedIfQueuedToSpawn)` and the pool return).

## 2026-10-27 — mega-refactor-062d-queued-spawn-query

- Summary: the pool return of a rejected spawn asks one named engine query, `GameEngine.hasClaimOn(entity)` (live now,
  in the entity set, or in the spawn queue), in place of `!liveNow && !engine.contains(entity,
  containedIfQueuedToSpawn = true)`. The flag goes and `contains` becomes `internal`. A sixth state was not added
  (owner decision: one call site). No state or engine action changes. Item `062` is done. Squash commit `5a725afd8`.
- Contract check: a temporary suite (deleted before the merge) showed `hasClaimOn` equal to the old condition in
  2,329,776 checks on 10,000 random sequences, before every call, including inside `onSpawn`/`onDestroy` and with
  rejected spawns; six permanent tests; clean build, 546 engine and 172 core tests pass; warnings unchanged (110);
  `contact-rule-selection.txt` unchanged; the owner's play test passes (three checks).
- Follow-up: none.

## 2026-10-27 — mega-refactor-063a-contracts-are-entities

- Summary: the core entity contracts (`IFreezableEntity`, `IFreezerEntity`, `IFireEntity`, `IFireableEntity`,
  `IScalableGravityEntity`, `ILightSource`, `IOwnable`, `ITeleporterEntity`, `ItemEntity`, `IGateTrigger`,
  `IBossListener`) extend `IMegaGameEntity`; `IDamageable`/`IDamager` extend `IGameEntity`; `IGameEntity` extends
  `IDestroyable`. `existenceState` is a public `var` and `GameEngine.setExistenceState` (with its `GameEntity` cast and
  throw) is gone, so the engine accepts any `IGameEntity` (owner decisions). 31 casts removed;
  `IProjectileEntity.explodeAndDie` calls `destroy()`; `IUpsideDownable` deleted. Squash commit `65a8259bf`.
- Contract check: a temporary suite (deleted before the merge) showed the new `explodeAndDie` destroy step equal to the
  old one from all 5 existence states; new permanent tests `AnyGameEntityTest` (3) and `EntityContractsTest` (14);
  clean build, 549 engine and 186 core tests pass; warnings unchanged (88 unique lines, JDK 17 Corretto); greps clean;
  `contact-rule-selection.txt` unchanged; the owner's play test passes (five rooms).
- Follow-up: backlog candidate `IChildEntity`/`IParentEntity : IGameEntity` folded into `063b` (`063` `ITEM.md`);
  backlog candidate `id`/`group` on `IMegaGameEntity` is new item `064` (idea, owner question open).

## 2026-10-28 — mega-refactor-063b-damage-entity-rename

- Summary: `IDamageable`/`IDamager` renamed to `IDamageableEntity`/`IDamagerEntity` in one mechanical commit (170 files,
  484 replacements); they stay in `engine/.../damage/` (owner decision: item `023` decides their module).
  `IChildEntity`/`IParentEntity` extend `IGameEntity`. Squash commit `550d67569`; `.git-blame-ignore-revs` lists it
  (`bc2908a25`). Item `063` is done. The branch ran in parallel with `064a` (no shared file).
- Contract check: a check script showed each changed file equal to its old content with only the two word
  replacements; the rename commit built and passed the tests alone; clean build, 551 engine and 192 core tests pass;
  warnings unchanged (88, with the old names mapped); no old name left; `git blame` with the ignore file skips the
  rename; `contact-rule-selection.txt` unchanged; the owner's play test passes (three rooms).
- Follow-up: none.

## 2026-10-28 — mega-refactor-064a-instance-identity

- Summary: groups move out of `MegaGameEntity`/`MegaGameEntities` into an engine `GroupsSystem` (an ordered group
  index; `getEntitiesInGroup`) with a `GroupsComponent` (owner decisions: a system, not `GameEngine`; an index like
  `MegaGameEntities`). `GameSystem` gets `onEntityJoined`/`onEntityLeft` hooks. `MegaGameEntity.onSpawn` writes the
  component when the spawn has a group; `GameEntityUtils` loses its cast. Intended change with no effect on current
  content: an entity that skips `super.onDestroy()` and has a group leaves its group. `id` moves to `064b`. Squash
  commit `c86d67421`. The branch ran in parallel with `063b`; `master` (with `063b`) was merged into it before the
  squash, with no conflict.
- Contract check: a temporary suite (deleted before the merge; 200 seeds x 300 operations, 168,925 non-empty
  comparisons, 32,122 lookups from inside a system's update) showed the same ordered group members as the old
  registry, first against the real old code, then against `GroupsSystem`; new permanent tests `GameSystemHooksTest`
  (7), `GroupsSystemTest` (13), `EntityGroupsTest` (4); clean build after the merge of `master`, 571 engine and 196
  core tests pass; warnings unchanged (88); greps clean; `contact-rule-selection.txt` unchanged; the owner's play test
  passes (`MOON_MAN` `room10` and `room6` to `room10`, `WILY_STAGE_3` `room5`).
- Follow-up: bug filed `docs/issues/2026-10-27-spider-web-skips-super-on-destroy.md` (a `fix/` branch is agreed);
  backlog candidate is new item `066` (`GameSystem.reset()` clears pending removes); the `retainAll` note points to the
  existing issue `2026-10-22-mutable-ordered-set-retain-all.md`.

## 2026-10-28 — master (direct change)

- Summary: the `RUMBLECADE_FACTORY` entry in `LevelDefinition.kt` is commented out, so the unfinished Rumblecade level
  (its tileset is not scaled to 32-pixel tiles yet) leaves the Extras menu. The owner asked for this directly on
  `master` (2026-10-28: "Comment it out directly on master"); it is a one-entry change, not part of an active branch.
  Visible change: the Extras menu lists one level fewer. Item `067` counts only art that the game uses.
- Contract check: clean build passes; 571 engine and 200 core tests pass; warnings unchanged (88).
- Follow-up: none.

## 2026-10-28 — mega-refactor-064b-map-object-id

- Summary: the map object id moves from `MegaGameEntity.id` into a `core` `MapObjectComponent`, present only on
  entities spawned with `ConstKeys.ID` (owner decision: `id` is level data, not an instance identity). Readers use
  `IGameEntity.mapObjectId`; `RANDOM_ID_POOL` goes; `MegaGameEntities` indexes only map entities; the 4 casts go;
  `Laser` ignores its owner and reflector by reference and by map id; `DarknessV3` (dead) is deleted. Intended changes
  (accepted by the owner): the `FireFlower` `-1` sentinel matches no entity; a short `Laser` window after its owner or
  reflector is destroyed. Squash commit `897d87f27`. Item `064` is done. The `RODENT_MAN` play test needed the Moon
  Scythe, so the `--weapons` dev option was added first (`fix/weapons-launch-option`, `6ff8437b0`) and `master` was
  merged into the branch.
- Contract check: temporary suites (deleted before the merge) showed the new registry equal to a frozen copy of the
  old id logic in 166,119 comparisons, and `Laser` equal in all 304,906 live comparisons (11,326 differences, all in
  the accepted window); new permanent tests `MapObjectComponentTest` (7) and `LaserIgnoreTest` (4); clean build after
  the merge of `master`, 571 engine and 216 core tests pass; warnings 87 (one fewer: `DarknessV3` deleted); greps
  clean; `contact-rule-selection.txt` unchanged; the owner's play test passes (21 rooms).
- Follow-up: backlog candidate is new item `071` (`getEntity()` casts to `MegaGameEntity`). Two bugs found in the scan
  are fixed by this branch, so no issue file: each map spawn leaked one random id (the pool is gone), and the
  `FireFlower` in `SMB3_1` (object `117`) could ignore the entity that held random id `-1`. Note: the branch folder
  files were empty on `master` after the branch start (a script error at the branch start); the squash restores them.

## 2026-10-28 — master (docs)

- Summary: `docs/mega-refactor/CLAUDE.md` gets two rules. "Item start: merge target": before the first branch of an
  item, Claude offers the choice between merging each branch directly into `master` and merging into a parent branch,
  with a recommendation. "Parent branches": the name `mega-refactor-<id>-master`, its worktree, how a branch merges
  into the parent (squash in the parent's worktree; docs still on `master`), how the parent stays current, and how the
  parent merges into `master` (`--no-ff`, after a combined play test and the owner's approval). Item `016` is the first
  item with a parent branch, `mega-refactor-016-master` (owner request: "Since this is a major change, instead of
  merging each branch directly to master, can we merge to `016-master`? Put in the mega-refactor CLAUDE.md that on
  each item start, Claude should offer the choice and make a recommendation").
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-28 — mega-refactor-016a-typed-level-state (into `mega-refactor-016-master`)

- Summary: first branch merged into the parent `mega-refactor-016-master` (not into `master`). A typed `LevelState` on
  `MegamanMaverickGame.levelState` and typed game members (`weaponsAttained`, private camera flags and collectors)
  replace the property bag; the 58 bag calls use the fields; the game no longer implements `IPropertizable`; 11 unused
  `ConstKeys` removed. Squash commit on the parent `07a4a3297`.
- Contract check: a temporary suite (deleted before the merge; 200 seeds x 200 write moments on a real game, with a
  hand mutation check) showed every read equal to a frozen copy of the old bag code; new permanent `LevelStateTest`
  (6); clean build, 571 engine and 225 core tests pass; warnings 81 (no new one; 6 unchecked casts gone); greps clean;
  the owner's play test passes (10 checks; an earlier run on the old code was logged as a baseline run).
- Follow-up: bug filed `docs/issues/2026-10-28-end-level-beam-flag-left-true.md`; no backlog candidates.

## 2026-10-28 — master (docs)

- Summary: `docs/mega-refactor/CLAUDE.md`, "Test maps": a request to run checks starts with the absolute path of the
  worktree as a `cd` command, also for checks shown from a draft `CONTRACT.md` (owner: "For the sake of convenience,
  please provide the path to the worktree being referred to. Add this instruction to the mega-refactor CLAUDE.md").
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-10-29 — mega-refactor-016b-entity-host-reads (into `mega-refactor-016-master`)

- Summary: the host-only reads in entities get a home. `GameClock` (`game.clock.paused`) replaces `game.paused`;
  `MegaPlayers.selectButtonAction` replaces `game.selectButtonAction`; `DrWily` submits the new `GAME_COMPLETE` event and
  `MegaLevelScreen` switches to `ENDING_SCREEN` (owner decision 12, replacing the planned `LevelState` callback); the
  dead `endGame` read is removed; the drawables map is typed with `InsertionOrderPriorityQueue`. No forwarding members
  (decision 11). The pathfinding debug reads were moved first by `fix/pathfinding-debug-flag` on `master`. Squash
  commit on the parent `dcb5db93e`.
- Contract check: a temporary suite (deleted before the merge; 200 seeded sequences) showed the new `DrWily` equal to a
  frozen copy of the old one, with `GAME_COMPLETE` submits in place of `setCurrentScreen` calls; new permanent tests
  `GameClockTest`, `StarknerPausedTest`, `DrWilyTest`, `MegaLevelScreenEventsTest`, a `MegaPlayersTest` case; clean
  build after the merges of the parent, 575 engine and 249 core tests pass; warnings 81 (no new one); greps clean; the
  owner's play test passes (8 checks; the final boss needed `--heartTanks`/`--healthTanks`, added by
  `fix/tanks-launch-options`; the pause-exit check runs in `GAUNTLET_1` because exit needs a beaten or extra level).
- Follow-up: none (the one bug candidate was already filed).

## 2026-10-29 — mega-refactor-016c-entity-services (into `mega-refactor-016-master`)

- Summary: entities take `MegaServices` (core) in place of `MegamanMaverickGame`: the holder is built in `postCreate`
  before Megaman and the entity pools and passed by both factories; 452 entity files and `MegaUtilMethods` swap the
  constructor type (mechanical, proved by a check script). Real homes replace the game forwards (decision 11):
  `GameCameraState` (state only until `051`), `GameSettings` with a top-level `Performance` enum, new `LevelState`
  members. `IMegaGameEntity` loses `game` and the 3 default methods (decision 15). `players` and `gravity` are built
  before the pools. The run scripts no longer pass `--performance medium`. Squash commit on the parent `4097749fa`;
  `.git-blame-ignore-revs` lists it (`d6bb042cb`, owner: "Keep doing what you're doing", one squash listed, not split).
- Contract check: a temporary suite (deleted before the merge; 200 seeded write sequences) showed each new direct read
  equal to a frozen copy of the old game forwards; check scripts proved the 3 rewrite steps (0 problems); new permanent
  tests `EntityConstructorsTest` (more than 400 classes), `MegaEntityFactoryTest`, `GameCameraStateTest`,
  `GameSettingsTest`, and the `testServices()` helper; clean build, 575 engine and 259 core tests pass; warnings 81 (no
  new one); greps clean (no code line in `entities/` names `MegamanMaverickGame`); the owner's play test passes (16
  checks).
- Follow-up: none.

## 2026-10-29 — mega-refactor-016d-system-services (into `mega-refactor-016-master`)

- Summary: systems code takes narrow services. `players` and `gravity` are `val` fields built with the game object;
  `megaContactRules(players, gravity, debugFilter)` and `MegaPathfinderFactory(levelState)` replace the game;
  `LevelDefinition.screenOnCompletion` is `(LevelState) -> ScreenEnum` (owner: "I see no harm in continuing with the
  original recommendation"). New test map `RefactorTest016.tmx`; `GravityChange` debug outlines are colored by direction
  (owner request). After it, only the host, 32 screen files, 13 backgrounds, `ScreenController` and the launcher name
  `MegamanMaverickGame` in code (the input for `001b`). Squash commit on the parent `ffb8741a0`.
- Contract check: new permanent tests `GamePlayersFieldsTest`, `MegaContactRulesTest`, `MegaPathfinderFactoryTest`,
  `LevelCompletionScreensTest` (first run against the old code through an uncommitted test adapter); clean build, 575
  engine and 272 core tests pass; warnings 81 (no new one); `contact-rule-selection.txt` unchanged; greps clean; the
  owner's play test passes (check 2 in the new room replaced by the `GravitySwitcharoo` checks; the upside-down view
  there is filed as the question `docs/issues/2026-10-29-gravity-change-turns-camera-upside-down.md`).
- Follow-up: backlog candidate is new item `083` (`PathfinderResult.targetReached`).

## 2026-10-29 — mega-refactor-016e-service-names (into `mega-refactor-016-master`)

- Summary: a mechanical rename (owner: "Scope approved, go with your recommendations"). The value `game` of type
  `MegaServices` is `services` in 489 files; on `MegaServices` and on `MegamanMaverickGame`, `eventsMan`, `assMan`,
  `audioMan` and `state` are `events`, `assets`, `audio` and `gameState`, with every read on a services or host
  receiver. A 6-line pre-step renames the local `assets` in `MegamanMaverickGame.queueAssets()` to `queuedAssets`.
  Comments, strings, locals of other classes and the host's own name `game` keep their names. Root `CLAUDE.md` uses
  the new names. Squash commit on the parent `9502724cd`; `.git-blame-ignore-revs` on the parent points at it
  (`2f6f1fc6c`).
- Contract check: the mechanical commit is 532 files and 2,038 lines, each line only token renames (the same as the
  dry run of the scan); normalized `javap -c -p` of all 1,861 `core` main classes equal; clean build, 575 engine and 272
  core tests pass; the same 81 warnings; `contact-rule-selection.txt` unchanged; 3 smoke launches with no exception;
  the owner's short sanity pass passes ("approved").
- Follow-up: none. Next for `016`: the parent merge into `master`.

## 2026-10-30 — mega-refactor-016-master (into `master`)

- Summary: item `016` is done. The parent merges into `master` with `--no-ff` (merge commit `eb2debc6e`), so each
  branch keeps its squash commit: `016a` `07a4a3297`, `016b` `dcb5db93e`, `016c` `4097749fa`, `016d` `ffb8741a0`,
  `016e` `9502724cd`. `.git-blame-ignore-revs` lists `4097749fa` and `9502724cd`. Entities, entity utilities, contact
  rules and the pathfinder factory take `MegaServices` (named `services`) or narrow services in place of
  `MegamanMaverickGame`; only the host, screens, layer builders, backgrounds, `ScreenController` and the launcher name
  the host (the input for `001b`). Before the merge, `master` merged into the parent twice (docs only; the 5 `016e`
  branch-folder files conflicted add/add and kept the parent's versions).
- Contract check: the owner's combined play test (12 checks chosen from the `016a` to `016d` contracts) passes
  (verbatim: "all tests pass"); merge approval (verbatim: "yes"). On `master` after the merge: clean build passes; 575
  engine and 272 core tests, 0 failures; 81 unique warnings.
- Follow-up: item `072` (remove `IMegaGameEntity`) can start. Found during the play test: the old bug
  `docs/issues/2026-10-29-needle-spin-no-bounce-while-jump-held.md` (also on `master` before the merge).

## 2026-10-30 — master (docs)

- Summary: item `031`: branch folders prepared ahead of time (no branches yet). `mega-refactor-031d-devices` is
  `planned`; its scan and a draft `docs/SCOPE.md` come next. `mega-refactor-031f-menu-input` (the "menu-input branch"
  of the `031` decisions of 2026-10-26) is `blocked` by `031d` and `031e`; its process log has the proposed goal,
  including the removal of the `controllerPoller` facade and the engine `IControllerPoller`. Its scan waits for `031e`
  to merge. `ITEMS.md` has both branch rows; `ITEM.md` lists them. Owner (verbatim): "yes", to the proposal to
  register `031f` now and scope `031d` next.
- Contract check: not needed (docs only).
- Follow-up: the `031d` scan.

## 2026-10-30 — mega-refactor-051a-camera-read-interface

- Summary: entities read the game camera through a read-only `engine` interface, `ICameraView` (`direction`,
  `getRotatedBounds(out)`, `toGameRectangle(out)`, `overlaps(bounds, out)`), with live values. `RotatableCamera`
  implements it; `core` adds the pooled `getRotatedBounds()`, `toGameRectangle()` and `overlaps(bounds)`.
  `MegaServices.getGameCamera()` became `val gameCamera: ICameraView` (76 caller lines, mechanical);
  `MegamanMaverickGame.getGameCamera()` became the field `gameCamera`; `getGameCameraCullingLogic` takes
  `ICameraView`. Squash `7370943a0`; `1d50a659e` puts it in `.git-blame-ignore-revs`.
- Contract check: all automated checks pass (a temporary equivalence suite of 50 cases compared the old and new
  reads, then was deleted); after the deletion and a clean build, engine 575 and core 274 tests pass, 103 unique
  warnings, the same set as the baseline. Owner play test (verbatim): "All tests pass".
- Follow-up: no backlog or bug candidates. `master` was merged into the branch (`170b37e05`) before the merge.
  `084a` touches `Megaman.kt`; 051a changed only line 214 there. 051 stays active; next is `051b-camera-behaviors`.

## 2026-10-30 — mega-refactor-031d-devices

- Summary: the device adapters of slot 0 read every device that single player can use. `Enter` and numpad `Enter` give
  `START`, and the arrow keys give the d-pad, as fixed aliases. `GamepadInput` reads every connected pad, from a copy
  of the controller list per tick. All pads read one code set, from the primary pad. The left stick gives the d-pad at
  `0.5` per axis. Triggers can be bound on the gamepad bindings screen and act as buttons at `0.5`. Fixes the issues
  `2026-10-25-gamepad-index-shift-keeps-old-codes.md` and `2026-10-25-preferred-controller-code-zero.md`. Squash commit
  `1006260c9`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` pass (A1 to A11, T1 to T23, R1 to R4). The
  owner played K1 to K7, G1 to G10 and D1 to D4 ("all tests pass"), with a PS3 pad and a NES-style pad. Check `D3` found
  a crash from this branch (a pad left the live list during the read); it was fixed with a test, and G6, G8, D2 and D3
  ran again. P1 (`--diagnostics` captures) was not done: the owner skipped it ("skip the captures"). Merge approval
  (verbatim: "approved").
- Follow-up: item `031` stays active; next is `031e` (bindings), then `031f`. Bug filed:
  `docs/issues/2026-10-30-robot-master-intro-dispose-after-audio.md`. The branch backlog candidate (a readable trigger
  label) went to the `031` `ITEM.md` as a `031e` question, not a new item, because it belongs to that branch.

## 2026-10-30 — master (docs)

- Summary: item `031`: branch folder `mega-refactor-031e-bindings` prepared ahead of time (no branch yet; `planned`).
  The owner decided that a replug keeps today's behavior: saved bindings do not load by default (`ITEM.md`, decision of
  2026-10-30), so that open question is closed.
- Contract check: not needed (docs only).
- Follow-up: the `031e` scan.

## 2026-10-30 — mega-refactor-084a-player-state

- Summary: the first branch of `084`. `facing`, `aButtonTask`, `currentWeapon` and `running` are typed fields on
  `Megaman`, and the teleport hooks are constructor parameters of `TeleportableComponent`, set once in
  `Megaman.init()`. `MegamanProps`, `ConstKeys.RUNNING`, the 3 `ON_TELEPORT_*` keys and
  `setStandardOnTeleportContinueProp` are deleted. Squash commit `2a81e1ba3`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` pass (876 tests in `engine:test` and
  `core:test`, `lwjgl3:jar`, the `grep`, a smoke launch with a clean exit). The owner played the manual checks ("all
  tests pass"), before `master` was merged into the branch; that merge took in `031d` and `051a` with no conflict, and
  the automated checks ran again on the result. Merge approval (verbatim: "merge").
- Follow-up: item `084` stays active. The branch backlog candidate (the other player keys in the property bag) went to
  the `084` `ITEM.md`, not a new item, because it belongs to a later `084` branch. No bugs filed. `051b` takes this
  branch from `master` before its step 8.

## 2026-10-30 — mega-refactor-051b-camera-behaviors

- Summary: the game camera is driven by a `CameraController` with ordered parts (`CameraFollow`, `CameraRoomBounds`,
  `CameraRoomClamp`, `CameraRoomTransition`, `CameraRotationStep`, `CameraShake`), in place of `CameraManagerForRooms`
  and `CameraShaker`. The camera knows only where it points and how it is rotated. The 5 room callbacks moved from
  `MegaLevelScreen` to `LevelRoomHandler`. `focusSnappedAway` became `startSmoothFollow` and `stopSmoothFollow` on
  `services.cameraController`; `coerceIntoBounds` is gone. Test map `REFACTOR_TEST_051`. Squash commit `7fe00b8b8`.
- Contract check: all automated checks in the branch `docs/CONTRACT.md` pass. The temporary equivalence suite matched
  the old manager frame by frame, and it caught 4 of 4 deliberate defects. After its removal: engine 585, core 310, 0
  failures, `lwjgl3:jar`, 96 unique warnings with none new. The owner played the manual checks ("All tests passed").
  Merge approval (verbatim: "approved").
- Follow-up: item `051` stays active; next are `051c`, `051d` and `051e` (`051e` removes `RotatableCamera` and
  `GameCameraState`, with or after `046`). New item `085` from the branch backlog candidate. No bugs filed.

## 2026-10-30 — mega-refactor-084d-damage-reaction

- Summary: stun, knockback, the recovery flash and freeze left `Megaman.kt`, one component per mechanism, in `entities/state/`:
  `StunComponent`, `KnockbackComponent`, `DamageRecoveryComponent`, `FreezeComponent` with `IThawRule` and `MashThawRule`. Hooks and
  values are constructor arguments, set in `define*Component()` functions. The updatables lambda stays as the schedule.
  `IFreezableEntity` left `Megaman`. New test map `RefactorTest084` (`REFACTOR_TEST_084`). Squash commit `4b65b1fdb`.
- Contract check: the automated checks in the branch `docs/CONTRACT.md` pass (922 tests in `engine:test` and `core:test`,
  `lwjgl3:jar`, both `grep` checks, a short launch). The temporary suite matched the old code on 20 seeds of 3000 frames for stun,
  recovery and freeze, and on all 4 directions for knockback. It is deleted. The owner played the manual checks ("All other tests
  pass"). The `--diagnostics` captures were not done: the owner skipped them ("I'll skip this for now"), so the frame-time effect is
  not measured. Merge approval (verbatim: "yes").
- Follow-up: item `084` stays active; the movement and effects branches are next, and the open questions in its `ITEM.md` apply to
  them. Bugs filed: `docs/issues/2026-10-30-spawn-damage-lock-no-effect.md` and
  `docs/issues/2026-10-30-saw-trajectory-velocity-as-position.md`. No backlog candidates.

## 2026-10-30 — master (docs)

- Summary: item `051`: research and owner decisions for camera zones and auto-scroll are in `ITEM.md` and `NOTES.md`.
  Branch folders `mega-refactor-051c-camera-zones` (`planned`) and `mega-refactor-051d-auto-scroll` (`blocked` by
  `051c`) prepared ahead of time with draft `SCOPE.md` files. No branch exists yet.
- Contract check: not needed (docs only).
- Follow-up: owner approval of the 2 draft scopes. Short answers to linked questions in `013` and `041`.

## 2026-10-30 — master (docs)

- Summary: new item `086` (player movement modifiers as a component), from the owner's request and a scan. `084` does
  not entail it. The `051` `ITEM.md` and the `051d` draft scope point to it.
- Contract check: not needed (docs only).
- Follow-up: none. `086` is `idea`.

## 2026-10-30 — mega-refactor-084c-movement

- Summary: post-action momentum and the room-transition carry are player components, with functions in `entities/state/` and hooks
  in `M/components/MovementComponents.kt`. The `AButtonTask` transitions moved to `Megaman.updateAButtonTask()`, with no component.
  `Megaman.kt` went from 879 to 789 lines. `onSpawn` still resets the momentum component, because a behavior that ends in the
  component reset pass can write momentum after its reset (item `087`). One intended gameplay change: the room-carry crash on a
  direction change is fixed, at the request of `051c`, with the owner's approval. The start of a transition clears the carried
  values, and the end restores each one to its own axis. New rooms `carry_left`, `carry_right` and `carry_top` in `RefactorTest084`.
  Squash commit `1d6a1ee52`.
- Contract check: the automated checks in the branch `docs/CONTRACT.md` pass (943 tests in `engine:test` and `core:test`,
  `lwjgl3:jar`, both `grep` checks, short launches). The temporary suite matched the old code on 20 seeds of 3000 frames in 2
  directions; for the carry, on pairs with the same `carries` value. It is deleted. The owner played the manual checks ("all other
  tests pass"). `edge_rotated`, which reproduces the crash, is only on `051c`, so it was not played here. Merge approval (verbatim:
  "yes").
- Follow-up: new item `087` (reset order for components). No bugs to file. `051c` check 18 can now run after `051c` takes `master`.
  `WilyStage3_v4.tmx` (`room7`, `room9`) also sets `megaman_direction`; not tested. Item `084` stays active; `084b` (effects) is next.

## 2026-10-31 — mega-refactor-051c-camera-zones

- Summary: merged into the parent `mega-refactor-051-master`, not `master` (owner decision, so that `051c` and `051d` are played
  together first). `CameraLimitStage` with `ICameraLimitRule`, `CameraRoomScan`, `CameraZones` from the Tiled `camera_sections`
  layer, with limits as references to Tiled objects, the ease and snap rule, players-only `CameraEdgeBlock`, `CrusherComponent`
  (`GravityBlock` moved to it), and `CameraController.reset()`. Levels with no zones play the same. New rooms in
  `RefactorTest051`. Squash commit `03c884d48` on the parent.
- Contract check: the automated checks pass in the parent worktree after `./gradlew clean` (engine 594, core 391, `lwjgl3:jar`,
  96 unique warnings, the baseline count). The temporary suites passed against the frozen old code and are deleted. The owner
  played the manual checks ("tests pass"). Merge direction (verbatim: "let's merge it into a parent 051 branch cut from
  `master`").
- Follow-up: 2 bugs filed in `docs/issues/` (`CameraRoomTransition.cancel()` timers; a spawn in the start area). `031e` also
  changes `Constants.kt` and `MegaLevelScreen.kt`, so check them when it merges. `051d` starts from the parent. The owner plays
  the combined result before the parent merges into `master`.

## 2026-10-31 — mega-refactor-084b-effects

- Summary: the underwater bubbles and the trail sprite timer are player components (`BubbleEmitterComponent`, `TrailSpriteComponent`), with
  hooks in `M/components/EffectComponents.kt`. Their timers keep the old rule and survive a respawn. The out-of-bounds rule is `isOutsideLevel`.
  `ExplosionOrb.spawnRing` replaces the orb tables in `Megaman` and `AbstractBoss`. `EXPLOSION_ORB_SPEED` is now `DEFAULT_EXPLOSION_ORB_SPEED` in
  `ExplosionOrb`, with an optional `ConstKeys.SPEED` spawn prop that no caller passes yet (the owner asked for it). New room `out_of_bounds` in
  `RefactorTest084`. `Megaman.kt` has 672 lines (990 at the start of `084`). Squash commit `997b83db2`.
- Contract check: the automated checks in the branch `docs/CONTRACT.md` pass (961 tests in `engine:test` and `core:test`, `lwjgl3:jar`, the 3 `grep`
  checks, short launches). The branch had no temporary suite. The owner played the manual checks ("tests pass"). Merge approval (verbatim: "yes").
- Follow-up: on the baseline code, Mega moved from the `out_of_bounds` spawn into the pit in 3 of 15 launches with no scripted input. The cause is known:
  the owner played in the open window (owner, 2026-10-31), so it is not a defect. No backlog or bug candidates. Item `084` stays active until the owner
  decides on the remaining bag keys.

## 2026-10-31 — master (docs)

- Summary: the owner agreed to the 8 decisions from the research on the `Megaman` interfaces (`084` `NOTES.md`). Item `085` is dropped; `084f` takes its work. `048` owns `IFaceable`. The `060` `ITEM.md`
  has the new prerequisites. Stale lines in `029`, `030`, `060`, `063` and `084` are corrected. Planned `084` branches: `084e`, `084f`, `084g`, `084h`.
- Contract check: not needed (docs only).
- Follow-up: `084e` starts next.

## 2026-10-31 — mega-refactor-051d-auto-scroll

- Summary: merged into the parent `mega-refactor-051-master`. `CameraScroll` with Tiled `type=scroll` sections, stop modes, one-way `next`
  links, a kept trailing edge and room rules. Edges follow the fall direction. `crush_surface` objects replace the side sense in a
  crush. Edge blocks cover their section or zone. The view size blends over a camera turn. A scroll waits until the player is ready.
  New rooms in `RefactorTest051`, and spawn `23` moved out of a ceiling block. Squash commit `4ed49c0f9` on the parent.
- Contract check: the automated checks pass in the parent worktree after `./gradlew clean` (engine 596, core 429, `lwjgl3:jar`, 96 unique
  warnings, the baseline count). The owner played the manual checks ("The tests pass"). Merge approval (verbatim: "yes").
- Follow-up: new item `089` from the backlog candidate. No bugs to file. The hold list has no play check for the boss health fill.
  `031e` also changes `Constants.kt` and `MegaLevelScreen.kt`. The owner plays the combined result before the parent merges into `master`.

## 2026-11-01 — mega-refactor-051-fix-scroll-room-scope

- Summary: merged into the parent `mega-refactor-051-master`. The combined play test found a death at the end of a pan down from
  `basic_up`. A respawn now marks a section done only if the section overlaps the current room, a kept edge applies only in a room that
  its section overlaps, and no block or kill acts on limits from the last room. Squash commit `285c734a4` on the parent.
- Contract check: the automated checks pass in the parent worktree after `./gradlew clean` (engine 596, core 450, 0 failures, `lwjgl3:jar`,
  96 unique warnings, the baseline count). Owner play test and merge approval (verbatim: "approved").
- Follow-up: no backlog candidates and no bugs to file. A `TODO` in `CameraScroll.deriveDoneSections` marks the accepted case: after a
  respawn in a later room, a walk back into an earlier section starts it again. The combined play test on the parent continues.

## 2026-11-01 — mega-refactor-051-master

- Summary: the parent merged into `master` with `--no-ff`, merge commit `14caa4191`. It carries `051c` (`03c884d48`), `051d` (`4ed49c0f9`) and
  `mega-refactor-051-fix-scroll-room-scope` (`285c734a4`). Camera zones, scroll sections, crush surfaces and the view blend are on `master`.
- Contract check: the owner played the combined result, with the control checks `MOON_MAN room6`, `WILY_STAGE_3 room5`, `INFERNO_MAN room5`
  and `INTRO_STAGE room1` after the last fix ("tests pass"). Merge approval (verbatim: "merge"). The automated checks pass on `master` after
  `./gradlew clean` (engine 596, core 450, 0 failures, `lwjgl3:jar`, 96 unique warnings, the baseline count).
- Follow-up: the parent stays for `051e`. `031e` also changes `Constants.kt` and `MegaLevelScreen.kt`, so it must check both files when it
  reaches `master`.

## 2026-11-01 — mega-refactor-084e-class-line

- Summary: removed 5 of the 15 interfaces on `Megaman`, and the dead player code, so that a second playable entity gets the gate, cart and
  sprite paths through `isPlayer` and component queries. Squash commit `647c24b56`.
- Contract check: the owner's play test passes ("tests pass"), and merge approval (verbatim: "merge"). `051` reached `master` before the
  squash, so `master` went into the branch. One conflict, the imports of `GravityBlock.kt`; `master`'s side kept. The play test was not
  repeated on the merged code. The automated checks pass on the branch and on `master` after `./gradlew clean` (engine 600, core 461,
  0 failures, `lwjgl3:jar`, 96 unique warnings, the baseline count). A launch of `REFACTOR_TEST_084 --room cart` gives code 124 with no exception.
- Follow-up: new item `093` from the backlog candidate. The old `Cart` candidate is covered by `092`. No bugs to file. `031e` shares
  `AirDashBehavior.kt` and `ControllerComponent.kt` with this branch, in the imports only.

## 2026-11-01 — master (docs)

- Summary: prepared the `051e` branch folder and added its row to `ITEMS.md`. New `051` decisions: `051e` goes before
  `046`; `rotating` goes on `ICameraView`, with defaults for a camera with no rotation part; `ICameraPart` and
  `CameraController` lose their type parameter; `CameraRotation` is not `Resettable`, so a respawn keeps its smooth turn.
  Also recorded in `046`, and the stale open question in `058` is corrected.
- Contract check: not needed (docs only).
- Follow-up: filed the question `2026-11-01-camera-room-transition-raw-viewport-size.md`. The next step for `051e` is the
  scan and `docs/SCOPE.md`.

## 2026-11-01 — mega-refactor-084f-focus-progress

- Summary: `Megaman` went from 10 to 8 interfaces. `FocusComponent` holds the camera focus, and `PlayerProgressComponent` is the game state
  listener. The dead `ConstKeys.FOCUS` reads and the constant are gone, which is the `085` work. Squash commit `27c634d9c`.
- Contract check: the owner's play test passes ("tests pass"), and merge approval (verbatim: "go for it"). The automated checks pass on the
  squash commit after `./gradlew clean`, on a clean copy of the tree (engine 600, core 475, 0 failures, `lwjgl3:jar`, 96 unique warnings, the
  baseline count). A launch of `REFACTOR_TEST_029 --room heart` gives code 124 with no exception.
- Follow-up: no new items and no bugs to file. `git add -A` took the owner's local mute of `run-game-alpha.sh` into the branch; it was reverted
  before the squash, so `master` has no change to that file. The next branch of `084` is `084g`, after `031e` merges.

## 2026-11-01 — mega-refactor-031e-bindings

- Summary: one `PlayerSettings` per slot and the slot file `PlayerSettingsStore` replace `ControllerButtons` and the old prefs files, with no
  migration. `--resetBindings`, reserved keys, key and trigger labels, the capture end on a pad loss, `selectButtonAction` at boot, and "RESET TO
  DEFAULTS" saved. `BACK` from the bindings screen in a level goes back to the pause screen. Squash commit `b22b232c9`.
- Contract check: the owner's play test passes ("all tests passed"), and merge approval (verbatim: "yes"). `master` moved after the play test, so
  `master` went into the branch (`8ed39c993`). 2 conflicts, the imports of `AirDashBehavior.kt` and `ControllerComponent.kt`; both sides kept. The play
  test was not repeated on the merged code, because `master`'s changes in the shared files do not touch pause, resume, viewports, music or input. The automated checks pass on the branch and on a clean copy of `master` at
  `d28c60893` (engine 600, core 528, 0 failures, `lwjgl3:jar`, 76 unique warnings with the line numbers removed and no new line; 96 by the count of the `084f` entry, the baseline count).
- Follow-up: 5 issues closed (4 fixed, `2026-10-22-saved-key-bindings-not-applied.md` as on purpose). Item `034` updated. No new items and no bugs to file. `031f` is now `planned`. The accepted 1-frame pause grid has a `TODO` at `showOverPause` (item `013`).

## 2026-11-01 — mega-refactor-051e-camera-rotation

- Summary: removed `RotatableCamera`, `GameCameraState`, `CameraRotationStep` and the dead `CameraRotator`. Cameras are plain
  `OrthographicCamera`s. Rotation is an optional part: `CameraRotation` holds the turn and is not `Resettable`, `CameraView` gives
  the reads and the defaults for a camera with no part, and `CameraOrientation` writes the angle when it changes. `ICameraView`
  gets `rotating` (40 reads). `ICameraPart` and `CameraController` lose their type parameter. The pause exit also works in a
  `TEST_LEVEL`. Squash commit `d8f2a4ec0` on `mega-refactor-051-master`. The code reaches `master` with the parent merge.
- Contract check: the owner's play test passes on all 10 checks ("tests pass"), and merge approval (verbatim: "yes, merge").
  The temporary equivalence suite passed until its last commit, and it found a real difference: a 180 degree turn is a tie, so
  a float difference could spin a flip the other way; `CameraRotation` now uses the old float operation, and the angle is
  exactly equal. An automated check changed after the contract approval: `up` within `0.001` of the old camera could not pass,
  because the old camera drifts up to `0.0018` in 1000 turns. The automated checks pass on the parent after `./gradlew clean`
  (engine 621, core 521, 0 failures, `lwjgl3:jar`, 95 unique warnings, under the baseline count of 96).
- Follow-up: no new items and no bugs to file. The issue `2026-11-01-camera-room-transition-raw-viewport-size.md` stays open.
  Item `051` stays active until the parent reaches `master`. Then apply the root `CLAUDE.md` text in the branch `docs/NOTES.md`.

## 2026-11-01 — mega-refactor-051-master

- Summary: the parent merged into `master` with `--no-ff`, merge commit `e12c880f5`. It carries `051e` (`d8f2a4ec0`). Item `051` is done, and the root
  `CLAUDE.md` has the new camera text.
- Contract check: `master` went into the parent first. The checks pass on the parent after `./gradlew clean` (engine 621, core 521, 0 failures,
  `lwjgl3:jar`, 95 unique warnings). The code on `master` is identical to the parent, so the checks were not run a second time. `051e` is the only code on the
  parent, so the owner's `051e` play test is the combined result. Merge approval (verbatim: "approved").
- Follow-up: the parent worktree and branch are removed. The owner's uncommitted import removal in `CameraEdgeBlocks.kt` was saved as a patch for
  the merge and restored; it stays uncommitted. `084g` is active, and any `services.camera.rotating` on that branch must become
  `services.gameCamera.rotating`. Item `058` (who starts a turn) is next for the camera.

## 2026-11-01 — mega-refactor-084g-movement-state

- Summary: `RunComponent` holds the run state, and `run(delta, side)` replaces `runLeft` and `runRight`. The crouch timers moved into `crouchBehavior()`,
  and `currentAnimKey` and `airDashDirection` are typed fields. 15 casts to `Megaman` remain. `canMove` stays a member, by owner decision, with a
  `TODO` for `060`. Squash commit `87d512886`.
- Contract check: the owner's play test passes ("tests pass"), apart from 2 bugs that exist on `master` too. Merge approval (verbatim: "yes"). The
  automated checks pass on the squash commit after `./gradlew clean`, on a clean copy of the tree (engine 621, core 570, 0 failures, `lwjgl3:jar`, 95
  unique warnings). A launch of `REFACTOR_TEST_029 --room movement` gives code 124 with no exception.
- Follow-up: no new items. Filed `2026-11-01-rodent-claws-run-facing.md` and `2026-11-01-left-right-both-held.md`, both for `fix/left-right-last-pressed`,
  and `2026-11-01-heart-tank-does-not-sink-in-sand.md` for `fix/heart-tank-sinks-in-sand`. The next branch of `084` is `084h`.

## 2026-11-01 — master (docs)

- Summary: added `objectives/` with 9 objectives and 39 rules, of which 23 are always on. Each of the 103 items links to at least
  one objective, through an `Objectives:` line in `ITEM.md`, a column in `ITEMS.md` and an item table in each objective. The merged
  code was audited against each rule (`master` at `1fa0be319` for `O2` and at `30d88f0b7` for the others). `CLAUDE.md`, the root
  `CLAUDE.md`, `docs/issues/README.md` and the item and branch templates now use the objectives. The effort is no longer limited to
  "plays the same": a branch keeps all behavior that its contract does not name as an intended change.
- Contract check: docs only. The link checks pass: each item has a valid objective, each `ITEMS.md` row has one, and the item tables
  match the `ITEM.md` lines.
- Follow-up: new `idea` items `094` to `102`. Audit notes on `009`, `010`, `021`, `025`, `032`, `039` and `047`. Open: the conflict
  between `034` and `O9-R3`, the reference machine for `O5`, and the comparison games for `O4`. `084h` and `031f` keep their scope.

## 2026-11-01 — mega-refactor-084h-charge

- Summary: `WeaponChargeComponent` holds the charge timer, and the 5 charge reads and `stopCharging()` are `internal` extensions on `Megaman` in
  `components/WeaponChargeComponent.kt`. The charge rules stay for `024`. `Megaman.kt` went from 555 to 528 lines and keeps 8 interfaces. The one intended
  change: in `Test1.tmx`, a death during a charge no longer leaves the charge light until the respawn. Squash commit `d796e00ba`.
- Contract check: the owner's play test passes ("tests pass"). Merge approval (verbatim: "yes"). The automated checks pass on the squash commit after
  `./gradlew clean`, on a clean copy of the tree (engine 621, core 625, 0 failures, `lwjgl3:jar`, 95 unique warnings). A launch of
  `REFACTOR_TEST_029 --room movement` gives code 124 with no exception.
- Follow-up: no new items and no bugs. Item `084` is done, and the rest of the interface work moves to `048`. `O8` and `O1` have the new state, and their
  status does not change. The `Objective check` of this branch came after the scope approval, because `db7a263a0` arrived during the branch. It records
  `O5-R2` as `no effect` with no measurement; the owner can ask for a `--diagnostics` run. The 2 "Open questions" in the `084` `ITEM.md` stay open.

## 2026-11-01 — mega-refactor-031f-menu-input

- Summary: the 9 poller reads move to frames through `MegaPlayers.menuInput`. A second, never-locked frame carries `Esc` (`SystemButton.BACK`, `game.systemInput`). `Esc` is back in the menus and pause in
  gameplay, and it acts on press. `AbstractMenuScreen` has the back action. `game.controllerPoller`, `FrameControllerPoller` and the engine `IControllerPoller` go. The diagnostics label is `inputFrames`.
  Intended change: the pause test and the queued pause each test the lock, which fixes the dead pause menu at the start of a lock (issue `2026-11-01-start-release-at-lock-start-pauses.md`, older than `031`).
  Squash commit `df7028ed3`.
- Contract check: the owner's play test is partial. The owner (verbatim): "Admittedly, I did not run each and every test just because it's so many of them. I tested some of the keyboard-only tests and found no
  issues. I want to approve the changes". The owner did not name the checks, so no single manual check has a recorded pass. The 7 `[pad]` checks did not run. L1, L2, L6, M1 to M5, C1 to C5, S1 and X1 have no
  recorded result. Merge approval is the same message. A1 to A18, T1 to T33 and R1 to R11 passed on the branch at step 12. The automated checks pass on the squash commit after `./gradlew clean`, on a clean copy of the
  tree (engine 631, core 645, 0 failures, `lwjgl3:jar`, 75 unique warning lines with the line numbers removed, no new line). A launch of `REFACTOR_TEST_051 --room basic` gives code 124 with no exception.
- Overlap: `master` merged into the branch once, for `084g` (`LevelRoomHandler.kt`). At the merge, `master` had `084h` and 2 `fix/` changes: the two sides share 0 paths, and the rule of plan step 14.2 matches 0 paths,
  so no second merge. `084h` and the 2 fixes are not in a play test with this branch.
- Follow-up: item `031` is done. New `idea` items `103` and `104`. A gamepad back button goes in `070`. 5 new open issues: the listener lock, a focus-loss pause under a lock, a press read that carries a held button, a rebind
  with a held key and level select back after an extra level. `O2-R1` has no `031f` closing item now, and its text still names `controllerPoller`, as a rule text that only the owner changes. `O9-R1` names the pad gap. `O2`,
  `O9` and `O1` keep their status. No performance capture ran, so the system frame has no measurement.

## 2026-11-01 — mega-refactor-004a-entity-smoke-test

- Summary: new `core:smokeTest` suite: every concrete entity class (432) builds and runs `init()` headless with the real atlases and PNGs. A silent `findRegion` miss fails too; a miss through `containsRegion` does not count. `known-entity-failures.txt` holds the 18 classes that fail today, and a listed class that passes fails the test. `check` and CI run the suite. No production code changed. Squash commit `b8afd2632`.
- Contract check: all automated checks pass with no display on the branch (engine 631, core 645, smokeTest 438, 0 failures, `lwjgl3:jar`), and again after `master` merged into the branch and on the squash commit. The 7 fault checks each failed the suite and were reverted. CI run `41368452607` on `40509d023` passed; the owner pushed the branch, and that run is before the merge of `master` into the branch. No play test: the contract has no manual check, because no game code changed. A fresh code review gave 5 Minor findings; one was raised to Important and fixed.
- Overlap: `master` merged into the branch once, because 3 `core/src/main` files had changed (`EffectComponents.kt`, `BodyEntityExtensions.kt`, `IBodyExtensions.kt`).
- Follow-up: item `004` is done. New `idea` items `105`, `106`, `107`. 5 new open issues: `JetMet`, `ColtonJoe`, `TreeStump`, the 10 unused classes, and the 4 silent misses. `JetMet` is in the live map `Gauntlet2.tmx`, so it is the first to fix. `CLAUDE.md` and the branch contract template name `core:smokeTest`. `O6` current state is updated. Open: `O6-R1` names three tasks, not `core:smokeTest`; a change to the rule text needs the owner. The `master` branch on `origin` is 112 commits behind.

## 2026-11-01 — master (docs)

- Summary: a code scan added open questions for multiplayer to 12 `ITEM.md` files, including the owner's question on culling for several players. The owner then changed `O2`: Spec Ops and PvP are the crux of the objective, co-op in the main campaign is optional and comes after them, and the first PvP target is one large room. `O2` "Goal", "Done means", `O2-R6`, "Decisions" and "Open questions" change, with the owner's approval. `O8` names the modes. The mission and PvP questions move from `102` to the new items.
- Contract check: not needed (docs only).
- Follow-up: new `idea` items `108`, `109`, `110`, `111`. The `047` branch list must be reviewed against `108` and `109` before `047b`. Open: does PvP need rollback (`095`), and does PvP need `094` from its start (`109`).

## 2026-11-01 — master (docs)

- Summary: `O6-R1` names `core:smokeTest`, with the owner's approval, because CI runs it since `004a`. `O6` "Decisions" records the change.
- Contract check: not needed (docs only).
- Follow-up: none. This closes the `O6-R1` question in the `004a` entry.

## 2026-11-01 — mega-refactor-047a-second-slot

- Summary: with `--players 2`, a second tinted `Megaman` plays in a level on its own device (squash commit `fda6509f4`). The branch adds the `--players` flag, capacity `MAX_PLAYERS` (2), one `InputFrameBuilder` per slot, `PadShare` in `GamepadInput`, the slot 1 spawn (`slotSpawnProps`, with the `player_2` reference), the death rule for slot 0 and slot 1, all-player spawn and level-end locks, a debug tint and the `REFACTOR_TEST_047` map.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch with `master` merged in. The owner's play test passes (all experimental and control checks).
- Follow-up: bug filed: `docs/issues/2026-11-01-two-players-stuck-in-quicksand.md` (owner decision: each sinkable entity sinks and rises on their own). The branch list of `047` must be reviewed against `108` and `109` before `047b`. The `O2-R9` accepted gap stays until `038`.

## 2026-11-01 — master (docs)

- Summary: owner decisions on the character order. X (`081`) comes before Proto Man (`060`), and `CharacterDef` moves from `060` to `081`, so `060` now depends on `081`. `081` is `planned`, and it has a proposed branch split: X in slot 1 behind a dev flag, with the buster only. `081` no longer depends on `024` to start, and `047` has an exception for this dev flag. An NES-style pad is not enough to play as X: `069` no longer has this goal, and `O9-R1` has an exception for X, with the owner's approval.
- Contract check: not needed (docs only).
- Follow-up: `069` open question 7: does the `SELECT` override stay? See the next entry.

## 2026-11-01 — master (docs and direct change)

- Summary: the owner removed the `069` `SELECT` override and `LevelType.X_LEVEL`, because every pad having a dash input is no longer a goal. For X, `SELECT` is always `NEXT_WEAPON`, whatever the option says, and `R` is the only dash input and exists only for X. `selectButtonAction` and `AIR_DASH` are specific to Megaman and Proto Man. `068`, `070` and `081` no longer rely on them. `GAUNTLET_1` and `GAUNTLET_2` stay Megaman levels, so the `O9-R1` exception can affect only the new X6-based extra levels. Direct change to `master`: 9 `TODO` comments in 8 files now name item `081` in place of `060` or "Proto Man item". It changes comments only, and it did not go through a branch because the owner asked for this small cleanup after the move of `CharacterDef`.
- Contract check: not needed for the docs. The comment change runs the build and the tests before its commit.
- Follow-up: `081` open question 2 stays open: can Megaman also play the new X6-based extra levels? See the next entry.

## 2026-11-01 — master (docs)

- Summary: owner decision on `081` open question 2. For now, the X6-based extra levels are playable only by X, but the code must not block Megaman from playing them later. X-specific rules are done on a player basis, not on a level basis. `068` supports slopes for all entities. `069`, `081` and `O9` record this.
- Contract check: not needed (docs only).
- Follow-up: `081` open question 1 stays open: how does a level pick its character? The X-only limit must be level data (`O7-R3`).

## 2026-11-01 — master (docs)

- Summary: new `idea` item `113` (character select screen). `081` open question 1 has a proposed `characters` field on `LevelDefinition`. The `081` branch split changes: `047b` comes first, because enemies target slot 0 today, and `069` comes after the first X branch, because only X reads its button. So `081` depends on `047b` and `084`, and `069` depends on `031` and `081a`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-11-02 — mega-refactor-047b-player-rules

- Summary: the session has one read-only `SessionRules` from a `GameMode`, chosen with `--mode coop|pvp` (squash commit `cb1f0e6ed`). Friendly fire is off in co-op and on in PvP, and it covers shots, explosions, the needle spin and what a teammate owns. `targetFor` gives the nearest live player. `MegaPlayers.anyLive` and `forEachLive` fix 8 sites, so a dead player does not count while another player is live. `ChargedShot`, `SpiderWeb`, `BigAssMaverickRobot`, and health and weapon energy pickups act on the right player. A code review narrowed the types: `IOwnable` has an `IGameEntity` bound, `asPlayer` no longer takes `Any?`, `isFriendlyFire` is a `SessionRules` member, `players` is a `MegaGameEntity` member, and `BodyGravityChangeRule` takes `WorldGravity`, with no cast.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch with `master` merged in (`engine` 631 tests, `core` 693 tests, 0 failures). The owner's play test: "tests passed". `--diagnostics` in `DESERT_MAN` `room1`: with no `--players`, the median `screen.render` is 2.121 ms and 2.152 ms, against 2.169 ms and 2.205 ms on `master`. With `--players 2` it is 2.266 ms and 2.429 ms, with no `master` capture to compare. The owner accepted the result with the merge approval. `master` took 2 commits during the merge steps, so the branch took `master` again, with 1 `SwimBooster.kt` conflict in the first merge.
- Follow-up: new item `114` (gravity from components). Bugs filed: `docs/issues/2026-11-02-koopa-shell-rolls-toward-target.md`, `2026-11-02-enemy-direction-from-target.md`, `2026-11-02-players-any-counts-dead-slot.md`, `2026-11-02-darkness-v4-inverted-iterator-loop.md`. `047e` depends on the first branch of `112`. `112` and `081` no longer wait for `047b`. `072` has a new scope text, and it can start. The launches through `run-game-alpha.sh` with `timeout 12` can end in the Gradle build, before the game starts: use the built jar in `assets/` for a short launch. The quicksand fix is next in its own `fix/` branch.

## 2026-11-02 — master (docs)

- Summary: new `ROADMAP.md`, the recommended order of the work in stages and parallel tracks. It holds only the order, and it is advice, not a rule. `CLAUDE.md` lists the file, and "Branch merge" step 6 now updates it.
- Contract check: not needed (docs only).
- Follow-up: `010` and `102` are `idea`, and they must be `planned` before stages 3 and 4.

## 2026-11-02 — fix/retire-gauntlet-2

- Summary: owner decision. Gauntlet 2 is out of the game, so `JetMet`, `ColtonJoe`, `GapingFish` and `TreeStump` are in no live stage (squash commit `3747c6dd4`). The level entry and the 4 classes are commented out, each with a `TODO` that names the issue. `Gauntlet2.tmx` stays. `CeilingCrusher` stays active, because `Compactor.tmx` on `compactor-level` uses it.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes (`engine` 631 tests, `core` 693 tests, 0 failures). `TEST_1` and `GAUNTLET_1` launch from the built jar with no exception, and `--level GAUNTLET_2` exits with code 2. The owner approved the merge after the Extras screen check.
- Follow-up: 5 issues closed. `ROADMAP.md` updated. The owner decides on `CeilingCrusher` before `compactor-level` merges.

## 2026-11-02 — master (docs)

- Summary: new `idea` item `115` (partial sink and stuck behavior in sand). The quicksand contract adds `SandReactionComponent`: enemies with feet and a `DYNAMIC` body sink, and they die at a `Death` sensor.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-11-02 — master (docs)

- Summary: new `idea` item `116` (body senses as typed state, then a sense history). `048` records the target for item drops: an `ItemDropComponent` and a system.
- Contract check: not needed (docs only).
- Follow-up: the owner decides if edge features such as coyote time are wanted, which gives `116` its later callers.

## 2026-11-02 — master (docs)

- Summary: new `idea` item `117` (crush with a falling pushable block), from the owner's question in the play test of `sand_entities`.
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-11-02 — fix/quicksand-two-players

- Summary: 2 players on one `QuickSand` area now sink, rise and jump out alone (squash commit `44a4c7e61`). `QuickSand` owns its floors: a private floor for each sinkable entity and a shared floor at the surface. The 20 `FeetRiseSinkBlock` map objects are gone. `SandReactionComponent` sets `SINK`, `SUBMERGE` or `SURFACE`. Enemies with feet and a `DYNAMIC` body sink, and an enemy that dies in sand drops no item. `PushableBlock` no longer counts its own inner block as ground, and it sinks in sand with a rider.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 631 tests, `core` 717 tests, 0 failures). `DESERT_MAN` `room1` launches from the built jar with and without `--players 2`, with no exception. `--diagnostics` in `DESERT_MAN` `room1` with no input, 2 runs each: mean `resolveCollisions` 0.086 to 0.097 ms on the branch and 0.081 to 0.093 ms on `master`, so the ranges overlap; median frame 2.84 to 3.18 ms and 2.48 to 2.61 ms, and each branch run went first. These runs do not touch sand. The owner reports that the play checks pass and approved the merge.
- Follow-up: `2026-11-02-pushable-block-ledge-sliver.md` is next, and it also closes `2026-11-02-pushable-block-velocity-builds-at-rest.md`. `2026-11-02-ledge-sliver-jump-pose.md` waits for an owner decision.

## 2026-11-02 — master (docs)

- Summary: research on the enemy and boss design, in 12 agent reports, kept in full in the item notes. New items: `118` (boss fights as routines, rules and leases), `119` (`PLAYER_DESTROYED` and `LEVEL_FAILED`), `120` (a player that turns hostile and comes back), `121` (enemy clones of a playable character), `122` (human-driven boss). Owner decisions in `048`, `030`, `013`, `018`, `012`, `038`, `047` and `082`: no enemy or boss flag; `BossComponent` holds fight data only; `Fight` is a plain object; `013` has no boss states; no membership recompute on a component change; a scene takes the driver and not the identity; a player is slot-owned, not pooled. 10 new issues. Recounts in `O1`, `O2` and `O8`. The root `CLAUDE.md` says "slot-owned".
- Contract check: not needed (docs only). No game or build was run for this research.
- Follow-up: `118a` and `119` can start. `120`, `121` and `122` stay `idea` until they have a real caller.

## 2026-11-02 — fix/big-ass-robot-hand-block-position

- Summary: on a second `BigAssMaverickRobot` fight after a death, a hand no longer floats in midair while the boss falls (squash commit `8e7ae7af0`). A hand puts its body at the boss center at spawn, and it spawns its platform block with `BOUNDS` under the hand, so a pooled block no longer starts at its old position. Found in the owner's play test of `118a`; the bug was older than `118a`.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch (`engine` 631 tests, `core` 718 tests, 0 failures). The new test fails with each half of the fix taken out. The owner reports that the play checks pass and approved the merge.
- Follow-up: other entities that spawn a pooled `Block` with no position are not checked. `118a` takes this fix through a merge of `master`.

## 2026-11-02 — mega-refactor-118a-fight-routine

- Summary: the `BigAssMaverickRobot` fight runs on a `Fight` (squash commit `8ce479f1d`). `engine` adds `Step` and `Routine`. `core/fights` adds `Fight`, `FightPlan`, `FightRule`, `Lease`, `FightDirector`, `FightPlans`, the stage plan, its steps and a package README. `MegaLevelScreen` routes a boss with a plan to the director, and 4 guards keep the old flow off that boss. The other 24 live fights keep the old flow until `118g`. The music of this fight has one source. The owner renamed the steps by hand during the branch.
- Contract check: a temporary frozen-flow suite gave equal traces for each frame (normal fight, defeated level, slot 0 death at each frame, death then level reset), and it was deleted before the merge. `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch without it (`engine` 636 tests, `core` 741 tests, 0 failures). CI did not run, because the branch was never pushed. The owner reports that the play checks pass, and approved the merge.
- Follow-up: `118b` (`TimberWoman`, `MoonMan`, `ReactorMan`, with `midBoss`). The play test found `2026-11-02-big-ass-robot-hand-floats-on-second-fight.md`, an older bug, fixed in its own branch before this merge.

## 2026-11-03 — mega-refactor-118b-batch-1

- Summary: the 6 fights of `TimberWoman`, `MoonMan` and `ReactorMan` run on a `Fight` (squash commit `cd1af498b`). `FightPlans.find` reads the map `end` value and chooses `standardBossPlan` (renamed from `stageBossPlan`) or the new `midBossPlan`. The mid-level outro ends with `ExitMidBossFight`, `ResumeAfterMidBoss` and `AwaitBossGone`, so the resume moved from the screen into the plan. Intended change: after a mid-level fight, the unlock, the level music and the first free input frame come 1 frame earlier (research in the branch `docs/NOTES.md`). `FightPlan.music` is nullable, `Fight` takes a level music supplier, and the screen's `INTERMEDIATE_BOSS_DEAD` branch has a guard. The `fights` README has a generic mid-level example.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch (`engine` 636 tests, `core` 804 tests, 0 failures). 11 mutation checks, each one failed at least 1 new test. CI did not run, because the branch was never pushed. The owner reports that the 7 experimental and 4 control play checks pass, and approved the merge.
- Follow-up: none from the branch `TODO.md`. Next batch: `118c`.

## 2026-11-03 — mega-refactor-068a-slopes

- Summary: X6-style floor slopes for every body with a `FEET` fixture (squash commit `5da0ab120`). A `GroundSlope` block is a rectangle named `GroundSlope` in a `blocks_slopes(_<n>)` layer, with `side=left|right` and a ratio of 1/4 or 1/2. It has the stepped X6 surface, a triangle `BLOCK` fixture and a filled-triangle drawing. `MegaCollisionHandler` pushes a body up onto the surface (with friction), lets a rising body pass, skips the flat block at a run's high end, and throws `InvalidSlopeLabelException` for a `SLOPE` body that is not a `GroundSlope`. `GroundSlope.hitByFeet` holds a body on the surface on a continued feet contact (`UP`, `vy <= 0`, gravity on) and steps it down at the low end, so a slide stays on the ground. `BlockSideRule` and `HeadBlockRule` skip slopes. Test map `RefactorTest068.tmx` (9 rooms). No engine change; no existing map has a slope. The root `CLAUDE.md` got the fail-fast rule during the branch (`b176f72b5`).
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 848 tests, 0 failures). Each new test fails when the code it covers is broken. One `--diagnostics` sanity run in `shallow` (no baseline, owner decision): `resolveCollisions` mean 0.02 ms, `processContacts` mean 0.05 ms, no outlier. The owner's first play test found a jump pose and land sound on a slide on or off a slope, and the box-shaped slope art; both fixed (plan step 6b) and reproduced headless first. The owner reports that all experimental and control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: new items `123` (X6 flat step-down, after `017`) and `124` (slope pass-through from below; accepted snap, `TODO` at `GroundSlope`). The slope validator rule went into `003`. `O5` current state has the accepted `O5-R2` gap, with a `TODO` at the slope case in `MegaCollisionHandler` (comment added on `master` in the merge docs commit, because the branch had none). No bug candidates.

## 2026-11-03 — mega-refactor-019c-merge-spawn-queues

- Summary: one spawn queue (squash commit `5e4483993`). `SpawnsManager` takes a spawn function and gives each fired spawn to the engine at once, in spawner order; a refused spawn frees its spawner. `MegaLevelScreen` no longer keeps a `spawns` array; its second drain, after `engine.update`, always found the array empty and went with nothing in its place. `Spawn.onRejected` and the unread `MegaMapLayerBuildersParams.spawnsManager` went. The scan found that `playerSpawnsMan` and `SPAWN_NOW` were not spawn queues; the owner renamed `PlayerSpawnsManager` to `PlayerCheckpointManager` (field `playerCheckpointMan`). No gameplay change. Decision before the branch (item `019`): a destroyed entity respawns in the next tick at the earliest.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after a `master` merge (`engine` 636 tests, `core` 851 tests, smoke 437 tests, 0 failures); the same 76 unique compiler warnings; no diff in `engine/` or `contact-rule-selection.txt`. New `SpawnsManagerTest` (3 cases); with the rejection call removed, one case in it and one in `SpawnerReleaseTest` fail. A `timeout 12` launch of each contract room ran with no exception. The owner reports that the 4 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: item `019` is done. New item `125` (spatial or room index for spawners, with a measure first; objective `O5`). Closed issue `2026-10-22-spawner-drops-room-transition-event.md` (fixed by `019b`). The old class name changed in the item docs of `047`, `094`, `109` and the two open checkpoint issues; the `036` and `037` notes have a rename line. No bug candidates.

## 2026-11-04 — mega-refactor-118c-batch-2

- Summary: the 10 fights of `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` run on a `Fight` (squash commit `caf4ef701`). `FightPlans.find` gives the 5 bosses a plan with no music, and the plan comes from the map `end` value, as in `118b`. No new API, step or plan. The `fights` README has the open-fight rule ("a fight starts only when no fight is open", with the reason), a warning that `FightDirector.start` replaces an open fight with no cancel, and the corrected supplier line. Owner decision before the branch: no cancel in `FightDirector.start`, and no `TODO` (item `118` `ITEM.md`). Intended change: the `118b` 1-frame earlier resume, for the 5 mid-level fights. 17 of 25 live fights are now on the new flow.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 852 tests, 0 failures). 7 mutation checks, each one failed at least 1 new test. A `timeout 12` launch of each of the 9 boss-approach rooms ran with no exception; the only errors are known map issues. The owner played 2 checks (`GLACIER_MAN room12`, `WILY_STAGE_1 room9`), and both pass. The owner skipped the other 14 manual checks and asked for a lightweight code review in their place, with merge approval if it found no issue. The review found no issue (branch process log). CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. Next batch: `118d`.

## 2026-11-05 — mega-refactor-118d-batch-3

- Summary: the fights of `ElecDevil` (`WILY_STAGE_1`) and `GutsTank` (`WILY_STAGE_2`) run on a `Fight` (squash commit `f9a7021f7`). `FightPlans.find` gives `ElecDevil` and `GutsTank` their boss music in the plan, and the forced music line in their `onSpawn` is gone. `SigmaRat` gets a plan with no music; no live map spawns it, so it has tests only (owner decision). No new API, step or plan. Owner decision before the branch: the key-based gate unlock stays in `118j` (item `118` `ITEM.md` line corrected). 19 of 25 live fights are now on the new flow.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 854 tests, 0 failures). 6 mutation checks, each one failed at least 1 test. A `timeout 12` launch of each of the 3 contract rooms ran with no exception; the only errors are known map issues. The owner reports that the 2 experimental and 1 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. Overlap: `x6-intro-extra-level` adds `D1000` to the same `when` in `FightPlans.find`, and gets a text conflict when it takes in `master`; keep both sets of entries. Next batch: `118e`.

## 2026-11-05 — mega-refactor-118e-batch-4

- Summary: the 5 live mini-boss fights of `MoonHead`, `MechaDragon` and `ReactorMonkey` run on a `Fight` with the new `miniBossPlan` (squash commit `d06cc6ecf`). `FightPlans.find` reads the map `mini` before `end`; `mini=true` gives an intro of `AwaitBossReady`, `EndBossSpawn` (new step) and `ControllerOn`, and an outro that notifies the listeners, waits for the defeat end, opens the gate of the boss key (`ExitMidBossFight`), turns the controller on and waits until the boss is gone. No bar, no freeze, no music, and no lease. `PenguinMiniBoss` moves with tests only. Owner decisions: the map `mini` is the one source (the 3 forced `mini` lines went), the 1-frame earlier controller is accepted, and `Bospider` (no live map) is commented out. 24 of 25 live fights are now on the new flow; only `WilyFinalBoss` is left.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 858 tests, smoke 436 tests, 0 failures). 9 mutation checks, each one failed at least 1 test. A `timeout 12` launch of each of the 5 rooms ran with no exception; the only error is a known map issue. The owner reports that the 3 experimental and 1 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. `x6-intro-extra-level` gets a larger text conflict in the `when` of `FightPlans.find`; keep both sets of entries. Next batch: `118f`.

## 2026-11-05 — mega-refactor-118f-final-boss

- Summary: the last live fight, `WilyFinalBoss` in `WILY_STAGE_3`, runs on a `Fight` with the new `finalBossPlan` (squash commit `f04aa859e`): the stage plan with `OUR_BLOOD_BOILS_8_BIT` and no exit step, chosen by name. New `FightPlan.endsLevel` (the outro has `LevelVictory`); at `ENTER_BOSS_ROOM`, `find` runs first, and the "level already defeated" check runs only for a plan that ends the level, so `wily_room` no longer has it (owner, 2026-11-02). The forced `MUSIC` and `END` lines left `WilyFinalBoss`; the end scene stays in its `onDestroy` (owner, 2026-11-05). Intended change: with slot 0 dead at the end of the defeat animation, the bar goes when the fight closes (owner, 2026-11-05). The `fights` README open-fight rule names a block that spawns at the room entry, and a plan with no exit event. All 25 live fights are now on the new flow.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 872 tests, 0 failures). 5 mutation checks, each one failed at least 1 test. A `timeout 12` launch of each of the 2 rooms ran with no exception; the only error is the known `Sealion` map issue. The owner reports that the experimental and the control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. `x6-intro-extra-level` now changes only the `D1000` lines in `FightPlans.kt`, away from the new entry. Next: `118g` removes the old screen branches, the route and the guards.

## 2026-11-05 — mega-refactor-118g-remove-old-flow

- Summary: the old boss flow is gone (squash commit `b3c8a9574`). `MegaLevelScreen.onEvent` lost the `BOSS_READY`, `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `BOSS_DEAD` and `INTERMEDIATE_BOSS_DEAD` branches, their guards and their mask entries, and `ENTER_BOSS_ROOM` lost the `mini`/`end` fallback and the route check (`MegaLevelScreen.kt` 131 lines changed, mostly removed). `FightPlans.find` returns a `FightPlan` and throws the new `MissingFightPlanException` for a boss with no plan; the scan found a plan for the boss of each of the 25 boss rooms in the 26 maps that `LevelDefinition` names. The events `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN` went. By owner decision (2026-11-05), the parts with no caller left went too: `getEventOnDefeated` and its `WilyFinalBoss` override, `AbstractBoss.end`, `getHealthFillType`, `ConstKeys.HEALTH_FILL_TYPE` (and `FILL`, its only user), and `FightDirector.owns`. The `fights` README says that every boss room needs a plan. No change that a player can see.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 864 tests, 0 failures; 10 old-branch tests went). 3 mutation checks, each one failed its test. The unique compiler warnings of the changed files did not change. A `timeout 12` launch of each of the 4 rooms ran with no exception; the only errors are the known `Sealion` and `DuoBallCanon` map issues. The owner reports that the 3 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. The plan said the compiler warns on an unused import; it does not, so the branch counted imported names by hand (branch process log). Next in item `118`: `118h` (the bar), `118i` (`BossComponent`), `118j` (the key-based gate unlock).

## 2026-11-05 — mega-refactor-118h-bar-fill

- Summary: the fight runs the boss bar fill (squash commit `7c4fc1d87`). `Fight.update(delta)` runs the steps, then the bar fill while the bar is not finished; `FightDirector` and `MegaLevelScreen` pass `delta` on. The bar branch left the screen's `when` chain, so the screen only builds, draws and unsets the bar. `HealthFillType`, the `type` parameter of `BossHealthHandler.set` and the dead `ALL_AT_ONCE` branch went. By owner decision (2026-11-05), `118h` is lean: `HudBars`, `BarSpec` and `BarSource` wait in the item "Possible later" table until a second bar has a caller (`O1-R5`). Intended change: if slot 0 dies in the boss intro, the death sequence runs during the fill. The `fights` README says that a tick runs the steps, then the fill.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 867 tests, 0 failures). 3 mutation checks, each one failed at least 1 test. A `timeout 12` launch of each of the 2 rooms ran with no exception; the only errors are the known `Sealion` and `DuoBallCanon` map issues. The owner reports that the 2 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. Next in item `118`: `118i` (`BossComponent`), `118j` (the key-based gate unlock).

## 2026-11-05 — mega-refactor-118i-boss-phase

- Summary: a `BossComponent` holds the boss phase (squash commit `afe4dc6ac`). `BossPhase` is `ENTRANCE`, `INTRO` or `FIGHT`. The boss sets `INTRO` when `isReady` is true, and the new `StartBossFight` step sets `FIGHT`: after the bar fill in the stage plan, and after `EndBossSpawn` in the mini plan. So a fight step, not the `END_BOSS_SPAWN` event, starts the boss AI. `ready` and `betweenReadyAndEndBossSpawnEvent` went, and `AbstractBoss` makes the component as a field, because the fight reads it before `init()`. By owner decision (2026-11-05), the branch is phase only: `isReady`, `preReady`, `onReady` and `onEndBossSpawnEvent` stay on `AbstractBoss`, and a `TODO` in `BossComponent` points to `048` step 6. Scope count correction: 17 boss files read the phase, not 12 (16 plain `ready` reads that the scan grep missed, each mapped 1 to 1). The `fights` README has "The boss phase". No change that a player can see.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 636 tests, `core` 875 tests, 0 failures). 4 mutation checks, each one failed at least 1 test. A `timeout 12` launch of each of the 4 rooms ran with no exception. The owner reports that the 4 control play checks pass, and approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. Overlap: `x6-intro-extra-level` must change its 2 `D1000` reads when it takes in `master`: `D1000.kt:556` `!ready || betweenReadyAndEndBossSpawnEvent` becomes `bossComponent.phase != BossPhase.FIGHT`, and `D1000.kt:583` `ready` becomes `bossComponent.phase != BossPhase.ENTRANCE`. Next in item `118`: `118j` (the key-based gate unlock).

## 2026-11-06 — master (docs)

- Summary: planning has 1 owner approval in place of 3. The owner was tired from reading `SCOPE.md`, `CONTRACT.md` and `PLAN.md` (269 lines with no diagrams for `118j`). Now Claude writes the 3 files as its record, and the owner approves a one-screen `docs/BRIEF.html`, published as a private Artifact page. It has a before and after diagram, a code sketch at the first caller, the decisions as questions, risks and play checks. `PLAN.md` needs no approval. Changed "Planning" in `CLAUDE.md` and the 3 branch templates.
- Contract check: not needed (docs only).
- Follow-up: first brief on the next branch that starts planning. After the first brief, decide if a `BRIEF.html` template is necessary.

## 2026-11-06 — master (docs)

- Summary: no more game launches during a branch, also not short smoke launches, because the
  windows distract the owner. Compile checks use Gradle. Added "Review before the play test" to
  `CLAUDE.md`: one or more code-review subagents make sure that every manual check in the
  contract compiles, launches and runs as written, because the owner often runs only some of
  the checks. Added the review to the automated checks of the branch `CONTRACT.md` template.
- Contract check: not needed (docs only).
- Follow-up: active branches whose `PLAN.md` has smoke-launch steps skip them and use the review.

## 2026-11-06 — mega-refactor-118j-gate-unlock

- Summary: 1 keyed `UNLOCK_GATE` event unlocks a `Gate` (squash commit `bf5dc333c`). `Gate` reads 1 key from `boss_key` or `gauntlet_key`; by owner decision (2026-11-05) the 2 keys share 1 namespace, and a gate with both throws at spawn. `GauntletMaster` sends `UNLOCK_GATE` after its last wave. `FightPlans.find` reads `boss_key` from the boss map props into `FightPlan.gateKey`, so `AbstractBoss.bossKey` went. The step `ExitMidBossFight` is now `UnlockGate`: it sends the plan key, and nothing with no key or with slot 0 dead. `INTERMEDIATE_BOSS_DEAD` and `GAUNTLET_CLEARED` went. New `GateTest`. No change that a player can see. This is the last proposed branch of `118`, so the item is done; the "Possible later" table in `ITEM.md` waits for real callers.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch and on `master` (`engine` 638 tests, `core` 885 tests, 0 failures). 6 mutation checks, each one failed at least 1 test. The branch planned and ran 4 short launches before the 2026-11-06 rule against game launches; they ran with no exception. The branch did not do the new "Review before the play test", because the rule came after its plan; the owner played all 4 control checks, and they pass. The owner approved the merge. CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. Overlap: `038a` also changes `EventType`, so its merge can have a text conflict there. `038b` moves the `PLAYER_SPAWN` listener of `Gate` and must change the `PLAYER_SPAWN` case of `GateTest`.

## 2026-11-06 — master (docs)

- Summary: a passed play test with no other request from the owner is now the approval to merge.
  Claude does not ask for merge approval. Claude resolves simple merge conflicts itself, runs the
  automated checks again and merges. Claude stops and tells the owner when a conflict is
  substantial, when the checks fail after a resolution, or when another active branch or item
  can conflict with the merge. Changed "Branch merge", step 2, and the parent merge in
  "Parent branches".
- Contract check: not needed (docs only).
- Follow-up: none.

## 2026-11-06 — mega-refactor-081a-player-lifecycle

- Summary: the shared player lifecycle and rules left `Megaman` (squash commit `7a577b4cc`). New `players/PlayerLifecycle.kt` (`playerSpawnStart`, `playerSpawnEnd`, `playerOnDestroy`, `playerOnRoomTransition`, `killPlayerIfOutsideLevel`) and `players/PlayerRules.kt` (`playerInvincible`, `playerCanMove`, `playerAllowsDamageFrom`, `playerAfterDamage`, `playerEditDamage`). By owner decision (2026-11-05) they are free functions, not a base class or locks; stun, freeze and damage as lock reasons went to `126`. `MegamanDamageNegotiator` is now `players/PlayerDamageNegotiator.kt`. The rules read their components with `require`, so a player class that lacks one throws. The spawn is 2 shared calls, so the gravity set stays before the lock clears. `Megaman.kt` went from 535 to 465 lines. New `players/README.md`. No change that a player can see; the out-of-bounds error text now names "the player".
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after `master` merged in (`engine` 638, `core` 923, `smokeTest` 436, 0 failures). 38 new tests on a test player that is not `Megaman`; 3 older test files got a helper for the components that the fail-fast rules need. 13 mutation checks, each one failed at least 1 test. The branch ran 9 short launches before the 2026-11-06 rule against game launches, with no exception. It did not do the "Review before the play test", because the rule came after its plan. The owner's play checks pass, and the owner approved the merge. 1 simple conflict in `Megaman.kt` (imports, from `6c29604ef`). CI did not run, because the branch was never pushed.
- Follow-up: none from the branch `TODO.md`. `081b` (X) is the second caller. Overlap: `038b` must move the `PLAYER_SPAWN` submit, now in `playerSpawnEnd`. `x6-intro-extra-level` adds 7 entries to `MegamanDamageNegotiator`; they must move to `players/PlayerDamageNegotiator.kt` when it takes in `master`.

## 2026-11-06 — mega-refactor-038a-respawn-routine

- Summary: 1 `RespawnRoutine` in `screens/levels/respawn/` runs the death and the respawn as an ordered list of steps (`Step<RespawnContext>`), in place of the chain of 5 events between `MegaLevelScreen` and 2 handlers (squash commit `a97e75e4d`). `PlayerDeathEventHandler` is now `DeathFadeOut` and `PlayerSpawnEventHandler` is now `BeamInView`: timers, fades and draws only. `PLAYER_SPAWN_INIT` went; its body is `resetLevelForRespawn()`, without the room-music block. The engine got `EventsManager.runCount`, so a step can wait for the next `events.run()`, as the old hand-offs did. The public events (`PLAYER_JUST_DIED`, `PLAYER_DONE_DYIN`, `PLAYER_SPAWN`, `PLAYER_READY`) stay. New test map `RefactorTest038.tmx` (`REFACTOR_TEST_038`) and package README. No change that a player can see. This was the first branch under the one-brief planning rules (brief: an Artifact page).
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after `master` merged in with no conflict (`engine` 639, `core` 957, `smokeTest` 448, 0 failures). `RespawnRoutineTest` checks the frame of each event and of the level reset; 14 mutation checks, each one failed at least 1 test. The review before the play test (2 code-review subagents, no launch) found 1 confirmed and 2 plausible frame changes, and the re-review found 1 more; all are fixed and tested. 2 baseline launches ran before the 2026-11-06 rule against game launches. The owner's 6 play checks pass. CI did not run, because the branch was never pushed.
- Follow-up: closed `docs/issues/2026-10-22-respawn-stale-room-music.md`, with a correction (every `SMB3_1` room has `music`). The bug candidate `Bat` 616 in `DesertMan_16x14_v4.tmx` is already in 2 issue files, so no new file. `038b` (the `LEVEL_RESET` split) is still proposed; it must move the `PLAYER_SPAWN` submit in `players/PlayerLifecycle.kt` (`playerSpawnEnd`) and the `PLAYER_SPAWN` case of `GateTest`.

## 2026-11-06 — master (docs)

- Summary: added `_templates/branch/docs/BRIEF.html`, a brief template made from the `038a` brief: the same styles (light and dark), and the 7 sections of "Planning", step 3, in order, with placeholders and short instructions. Step 3 now says to start the brief from the template. A new branch folder gets the file with the rest of `_templates/branch/`.
- Contract check: not needed (docs only).
- Follow-up: `038b` is the first brief made from the template.

## 2026-11-06 — mega-refactor-038b-level-reset

- Summary: the respawn routine sends `LEVEL_RESET` from its `ResetLevel` step, and the 24 entity classes that used `PLAYER_SPAWN` as a level reset listen to it (squash commit `b4d18eaba`). By owner decision D1, `PLAYER_SPAWN` went, with its slot-0 submit and `TODO` in `playerSpawnEnd()`; `047` adds a spawn event for each slot when a listener needs one. The new event reaches the same `events.run()` as the old one, because `ResetLevel` runs in the same `render` as the player spawn and the run delivers by listener and key mask, not by submit time. No change that a player can see. Item `038` is done (its 2 planned branches merged). This was the first brief made from the new template.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch (`engine` 639, `core` 957, `smokeTest` 448, 0 failures). 3 mutation checks of the submit (drop, twice, moved to `BeamIn`), each one failed 5 tests. The review before the play test (2 subagents, no launch): no bug in the diff; the check review reworded 3 checks, removed 1 that could not fail, and relabeled 1 as a control check; 1 claim (no reset at level start) was wrong and rejected. No game launch during the branch. The owner's 4 play checks pass. `master` moved only in docs, so it was not merged into the branch. CI did not run, because the branch was never pushed.
- Follow-up: closed the `O2-R9` accepted gap. The roadmap listed "coins on respawn" and "`SPAWN` damage lock" as closed by `038`; neither branch touched them, and their issue files link `037` and `084`, so the line went and they stay open there. `081b` shares `players/README.md` with this branch (a docs conflict when it takes in `master`); its `MegamanX` calls `playerSpawnEnd()`, so it gets the change.

## 2026-11-06 — mega-refactor-081b-x-run-jump-shoot

- Summary: phase 1 of playable X (squash commit `30393d44d`). `MegamanX` (`entities/megamanx/`) plays slot 0 on
  `X6_INTRO` and `REFACTOR_TEST_081`, with the X6 values: walk start and walk, jump, fall, and the buster with the X6
  input (a press shoots, a hold charges, a release fires the charged shot), shot limits and a charge tint flash. It
  calls the `081a` `players/` functions. `PlayerCharacter`, `LevelDefinition.characters` and `PlayerRoster`: slot 0
  gets the level's character at `startLevel`. Megaman's jump moved to `players/playerJumpBehavior(JumpParams)`, and a
  test written on the old code shows that Megaman jumps the same. The camera focus is set at each level start. 5 `is
  Megaman` workarounds with `TODO`s for `024` (listed in `024` `ITEM.md`). X passes a one-way block while `DOWN` is
  held (until climb). `--players 2` on a level with no `MEGAMAN` is a launch error. The `X6_INTRO` boss room got a
  floor and the `D1000` arena moved onto it. Atlas: hand-made strips `stand_simplified`, `jump_v2` and
  `jump_shoot_v2` (not made by the slicer data file; a comment in `MegamanX.yaml`). New test map `REFACTOR_TEST_081`.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after the last
  merge of `master` (`engine` 639, `core` 1013, `smokeTest` 449). 16 mutation checks, each one failed at least 1 test.
  The review before the play test (2 agents) found 2 bugs (a slow sink through a ladder top; the charge sound after
  death), 2 risks and 2 map problems; all were fixed or decided by the owner. The owner played the checks, and they
  pass. CI did not run, because the branch was never pushed.
- Follow-up:
  - `047g` (active): when it takes in `master`, its group focus must move from `MegaLevelScreen.init()` into `start()`,
    or the `start()` call replaces it with slot 0 (owner decision: merge `081b` first and warn `047g`; `081`
    `NOTES.md`, "Camera focus and `047g`").
  - Bug filed: `docs/issues/2026-11-06-x6-intro-room2-floor-below-art.md`.
  - `X6_INTRO` is not finishable as X until the wall kick (phase 2): a 2.5-tile step in `room1`.
  - Workarounds documented in `024` and `029` `ITEM.md`.

## 2026-11-06 — mega-refactor-081c-x-wall-slide-air-dash

- Summary: phase 2 of playable X (squash commit `61460cd31`). The wall slide and the air dash moved into shared
  `players/PlayerWallSlide.kt` and `players/PlayerAirDash.kt` with params objects; `JumpParams` got `wallJumpAllowed`
  and `ignoreRelease`; `isRunSidePressed` and the air dash input reads moved to `players/`. Megaman passes his own
  rules and plays the same (37 baseline tests written on the old code pass unchanged). X got the X6 wall grab, slide,
  push-off, wall kick (kick lock and wall delay) and air dash (hover, fixed dash, 1 per air). The air dash starts on
  `A`, on `SELECT` when set to air dash, and on a new `MegaControllerButton.R` (keyboard `U`, pad `R1`; renamed from
  the branch's `DASH` at the merge, as `069` and `031` decided). An old save with no `R` entry leaves `R` unbound
  when its default clashes with another button. Test rooms `walls` and `dash` in `REFACTOR_TEST_081`. X can now pass
  `X6_INTRO` `room1` to the gate.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master` before the docs
  commit, after the rename (`engine` 639, `core` 1078, `smokeTest` 449). 15 mutation checks, each one failed at
  least 1 test. The review before the play test (2 agents) found no change to Megaman, 2 bugs in X (no wall delay;
  no dash on `R` or `SELECT` during a rise) and 3 risks; all fixed with tests. The owner played the checks, and they
  pass. CI did not run, because the branch was never pushed.
- Follow-up: open question 12 in `081` (a shot during the kick lock goes into the wall). Phase 3: the ground dash, the
  dash jump, the dash wall kick and the faster start on `R`.

## 2026-11-06 — mega-refactor-047g-shared-camera

- Summary: phase 1 of the shared screen (squash commit `3e8655f02`). With `--players 2`, the camera follows
  `PlayersFocus` (the mean of the players in the world; a 0.5 s ease when the group changes or the target jumps more
  than 1 tile; on an axis with a `WARP` or `KILL` edge, one keeper player stays fully in view). `CameraViewEdges` gives
  each view edge inside the room a rule (`NONE`, `WALL`, `BOUNCE`, `WARP`, `KILL`) with `delay` and `damage`, from
  `SessionRules.viewEdges`; the default is side walls. `WARP` holds the out player (`MegaLockReason.VIEW_EDGE`) and
  puts it beside a partner in view; `BOUNCE` damages through `ViewEdgeHazard`. The `--viewEdges` dev flag sets the
  rules. The rules are off in turned rooms and in rooms with camera sections (`TODO`, phase 5). With 1 player, nothing
  changes. Test rooms `group_wide`, `group_clamp`, `group_pit`, `group_tall` in `REFACTOR_TEST_047`.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after 2 merges of
  `master` (`engine` 639, `core` 1108, `smokeTest` 450). The first merge resolved 2 simple conflicts (the group focus
  moved into `MegaLevelScreen.start()`, as the `081b` entry asked; imports and a line in `LaunchOptionsResolver.kt`).
  The second merge was needed because `master` had replaced the `081c` commit of the first merge; the 6 `081c` files
  take `master`'s version. The review before the play test (2 agents) found 4 bugs and a per-frame allocation; all
  fixed. The owner played 4 rounds; 3 reports led to fixes (the ease, the warp hold, the keeper), and the fourth
  passed. CI did not run, because the branch was never pushed.
- Follow-up:
  - New items `127` (warp spot solids), `128` (cull near players, the owner's F3), `129` (warp visuals, phase 2).
  - Phase 5 note in `047` `ITEM.md`: zones and scrolls read the group midpoint with 2 players.
  - Open in `047`: the final default for each view edge, and whether side `WARP` stays.
  - Next: phase 2 of the shared screen (side exits, "carry").

## 2026-11-06 — master (direct change)

- Summary: new run script `run-game-test-coop.sh` (owner's request): the `run-game-test.sh` settings with
  `--players 2`, and `--level REFACTOR_TEST_047` (changed from `TEST_1` at the owner's request) and `--mode coop` unless the arguments give them (JCommander rejects an option
  given twice). A direct change to `master`, because it is a small run script with no gameplay change and no active
  branch owns it. Root `CLAUDE.md` lists it.
- Contract check: `bash -n` passes; the default logic was checked with no arguments, with `--level`, and with
  `--mode`. The game was not launched.
- Follow-up: none.

## 2026-11-06 — master (direct change)

- Summary: a co-op route in `RefactorTest047.tmx` (owner's request), so `./run-game-test-coop.sh` with no `--room`
  plays through room transitions: `coop_start` (16x14) → `coop_wide` (48x14) → `coop_shaft` (16x42, platforms on the
  way down, a ladder back up) → `coop_rest_1` (16x14) → `coop_hscroll` (64x14, a right scroll at 3 tiles/s) →
  `coop_vscroll` (16x42, an up scroll at 2 tiles/s, steps 3 tiles apart) → `coop_rest_2` (16x14). The map grew from
  356 to 550 tiles wide at the right, so the old rooms keep their coordinates. The level start spawn `0` moved to
  `coop_start`; the old one in `default_spawn` is now `14`, so `--room default_spawn` still works. New spawns `15`
  (`coop_rest_1`) and `16` (`coop_rest_2`). Built with the `level-builder` CLI and `temp/coop-route/` scripts. A direct
  change to `master`, because it is a test map with no code change.
- Contract check: 180 object ids, all unique; every new block and ladder cell painted; the layout drawn as text and
  checked. Gradle did not run (owner's uncommitted code in the main checkout), and the game was not launched.
- Follow-up: room transitions with 2 players are phase 2 of the shared screen ("carry"): slot 0 triggers them, and
  slot 1 moves to the transition point. In the scroll rooms, the view edge rules are off (phase 5).

## 2026-11-06 — mega-refactor-047h-side-exits

- Summary: phase 2 of the shared screen (squash commit `1ff8de509`). With 2 players, `CameraRoomExits` puts a
  players-only wall 1 tile outside each side room exit (a left or right room edge that touches another room) and,
  when every live player touches the exit, starts the transition at once (`transitionToRoom`); the carry moves every
  player. One live player: no walls. An exit that a `Gate` covers gets no wall (phase 4). On in rooms with camera
  sections; off in a transition and in a turned room. Owner's decisions D1 to D4 as recommended. With 1 player,
  nothing changes. Spawns `17` to `20` in the `coop_*` route.
- Contract check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on the branch after the merge of
  `master` (docs only), which is the tree of the squash (`engine` 639, `core` 1119, `smokeTest` 450). The checks did
  not run in the main checkout, because it holds the owner's uncommitted X code. 3 mutation checks each failed at
  least 1 test. The review before the play test (2 agents) found no serious bug; 3 check texts were corrected and 2
  real-stage checks added (`DESERT_MAN` `room14` with gates, `GLACIER_MAN` `room11` without). The owner played the
  checks, and they pass. CI did not run, because the branch was never pushed.
- Follow-up:
  - New items `131` (a player past the wall outside the span), `132` (spread after a transition), `133` (a wait cue).
  - Phase 6 note in `047` `ITEM.md`: with slot 0 held or dead, slot 1 can still leave alone.
  - Owner's decisions at the play test: `047i` (per-player transition path), `047j` (co-op default `bottom=WARP`);
    item `127` got the owner's report of a warp into a block.
  - `081d` changes other lines of `MegaLevelScreen.kt`; its merge may meet a simple import conflict.


## 2026-11-07 — master (docs)

- Summary: added the diff review in Diffity to the branch process (`CLAUDE.md`, "Diff review").
  The owner reviews the branch diff with anchored comments at the same time as the play test, in
  place of a GitHub pull request. A branch does not merge while the review has open threads or
  uncommitted fixes ("Branch merge", step 2). The commands are in the global skills
  `diffity-open`, `diffity-resolve` and `diffity-review`.
- Contract check: not needed (docs only).
- Follow-up: none.
