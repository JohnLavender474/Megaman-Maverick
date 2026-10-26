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
