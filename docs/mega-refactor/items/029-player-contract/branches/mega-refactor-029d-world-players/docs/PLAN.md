# Plan: mega-refactor-029d-world-players

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: draft
- Approved: no

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`. Smoke launches are short (about 20 seconds); a clean exit with no exception is
a pass. The owner plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| `WorldGravity` (`game.gravity`: `direction`, `set`) | the 6 gravity writers; the world-direction readers |
| `PlayerStateComponent.teleporting` | `Megaman.teleporting`; `PreciousGem` (through the view) |
| `MegaPlayer.depleteHealth()` | `MegamanDeath`/`PlayerDeath`, `GravityBlock` |
| `MegaPlayer.headFixture`, `feetGravityFixture` | `FloorButton`, `GravityBlock`, `PipePortal`, `InfernoChainPlatform` |
| `MegaPlayer.isAnyBehaviorActive` | `GroundSlideBlocker` |
| `MegaPlayer.teleporting` | `PreciousGem` |
| gravity-device receiver (a typed key on the player entity, registered by Megaman) | `GravitySwitcharoo` |
| light-source receiver (a typed key on the player entity, registered by Megaman) | `DarknessV2`, `DarknessV3`, `DarknessV4` |

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029d` row in `ITEMS.md` to `active` with the
  start date; mark `029d` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-029d-world-players
  .worktrees/mega-refactor-029d-world-players master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 110 core) and the warning count
  (expected 114) in the process log.
- Done: no

### 3. Test rooms (subagent)

- Changes: a subagent adds rooms to `RefactorTest029.tmx` with the `level-builder` skill, each with a
  player spawn and painted tiles:
  - `player_death`: a pit with a `MegamanDeath` sensor (instant), and a `GravityBlock` that can fall on
    Megaman's head;
  - `push_flip`: a `PushableBlock` and a `FlipperPlatform`;
  - `hit_by_player`: a `SmallGrass`, a `Togglee` and a `SmallIceCube`;
  - `gravity_switch`: a `GravitySwitcharoo` and a `ConveyorBelt`, with room to stand and to jump in
    both gravities.
  The subagent copies placements and properties from real maps.
- Check: a short launch of each new room shows no exception.
- Done: no

### 4. Player state, view parts, `WorldGravity` and the receivers

- Changes:
  - `PlayerStateComponent.teleporting` (a field); `Megaman.teleporting` reads and writes it.
  - `MegaPlayer`: requires `IHealthEntity`; `depleteHealth()`, `headFixture`, `feetGravityFixture`,
    `isAnyBehaviorActive(vararg)`, `teleporting`, and the two receiver calls (gravity device, light
    source), which read typed keys on the player entity.
  - Megaman registers the gravity-device receiver (sets its direction through `WorldGravity`, then its
    `aButtonTask` exactly as the devices do today) and the light-source receiver (the radius logic that
    the Darkness entities use today, moved unchanged).
  - `WorldGravity(players)` in core; `MegamanMaverickGame.gravity` made after `players`.
- Check: tests for `WorldGravity.set`, the new component field, the accessors and the receivers. Build
  and tests pass.
- Done: no

### 5. Gravity writers and readers

- Changes:
  - Writers call `game.gravity.set(...)`: `Megaman.onSpawn` (spawn `direction`), `MoonMan` (only when
    `targetPlayer` is not dead, with the `047` `TODO`), `PortalHopper` (when the entity is a player),
    `CapsuleTeleporter`, the level screen's room property; `GravitySwitcharoo` calls the receiver for
    each player in its trigger area.
  - A `TODO` in `Megaman.direction`'s setter: the camera rotation belongs to `WorldGravity`.
  - The world-direction readers read `game.gravity.direction`.
- Check: a grep shows no write of a player's direction outside `WorldGravity.set` and the player's own
  setter. Build and tests pass. Launch `gravity_switch`.
- Done: no

### 6. World code and `HIT_BY_PLAYER`

- Changes: `Receivers.HIT_BY_PLAYER: (player: MegaPlayer) -> Unit`; `BodyPlayerRule` passes
  `players.viewOf(entity)!!` (its cast and `TODO` go); `SmallGrass`, `Togglee`, `SmallIceCube` take the
  view. `IBodyExtensions.defaultDoUpdate` uses `players.viewOf(entity) == null`. `FeetSandRule` gets
  `{ game.players.any { it.ready } }` with the `047` `TODO`.
- Check: a test that `BodyPlayerRule` calls the receiver with the view, if practical. Build and tests
  pass. Launch `hit_by_player`.
- Done: no

### 7. World entities that need a choice

- Changes, by hand: identity and ownership use `players.viewOf`; kill zones use `players.forEach`
  with today's checks; `PushableBlock` and `FlipperPlatform` act for any player in contact; the
  Darkness light uses `players.forEach` and the light-source receiver; `CapsuleTeleporter` sets the
  gravity for the received player; `SwimBooster` (`029g` `TODO`); `ChargedShot` (`030` `TODO`);
  `SpiderWeb` sticks to the target; `PreciousGem` reads the owner view's `teleporting`.
- Check: a review of each site, listed in the process log. Build and tests pass. Launch
  `player_death` and `push_flip`.
- Done: no

### 8. World entities: the rest

- Changes: a script replaces the remaining `megaman.` reads in the world packages with
  `targetPlayer.`; a check of the diff that each changed line differs only in the reference.
- Check: the contract grep. Build and tests pass.
- Done: no

### 9. `PlayerDeath`

- Changes: rename the class `MegamanDeath` to `PlayerDeath` (file, class, `TAG`), the comment in
  `Death.kt`, and every TMX object named `MegamanDeath` (29 objects in 4 files, plus the test room).
- Check: a grep finds no `MegamanDeath`. Build and tests pass. Launch `player_death`.
- Done: no

### 10. Unused paths

- Changes: comment out `GravityCapsule` in full with its `TODO`; comment out `PortalHopper`'s
  `next_body_direction` (prop read, field, event property, body write) with its `TODO`.
- Check: a grep that nothing else uses them. Build and tests pass.
- Done: no

### 11. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test. Write the
  results in the process log.
- Done: no

### 12. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no
