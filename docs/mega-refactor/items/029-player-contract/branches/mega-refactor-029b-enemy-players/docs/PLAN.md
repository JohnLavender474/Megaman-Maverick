# Plan: mega-refactor-029b-enemy-players

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-23

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`.

Each step is its own commit. After each code step, the build and the tests pass, and a short launch
of `REFACTOR_TEST_029` shows no exception. The enemy steps (6 to 8) go from the sites that need a
choice to the mechanical rest, so that the last step can be a scripted replace that a review checks.

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029b` row in `ITEMS.md` to `active` with the
  start date; mark `029b` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-029b-enemy-players
  .worktrees/mega-refactor-029b-enemy-players master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 472 engine, 102 core) and the warning count
  (expected 114) in the process log.
- Done: no

### 3. Test rooms (subagent)

- Changes: a subagent adds rooms to `assets/tiled_maps/tmx/RefactorTest029.tmx` with the
  `level-builder` skill, each with its own player spawn and painted tiles on every block cell:
  - `enemy_aim`: a flat floor with a `Met` and a `SniperJoe`, with room to walk past both.
  - `enemy_drop`: a `Darspider` on the ceiling and a `SuicideBummer`, with room to walk under and
    toward them.
  - `enemy_chase`: a `Bat` on the ceiling, and room to move.
  - `enemy_magfly`: a `MagFly` over a ladder and open floor.
  - `enemy_contact`: a `Matasaburo` on the floor, and a `GapingFish` in a water area.
  - `enemy_owner`: a `Cokeyro`.
  The subagent copies each enemy's placement and properties from a real map (`Test1.tmx`,
  `TimberWoman_16x14_v3.tmx`, `DesertMan_v4.tmx`, `MagnetMan.tmx`, `GlacierMan_v3.tmx`), and reports
  what it could not place.
- Check: a launch of each new room shows no exception. Then the owner checks each new room on this
  baseline code; the results go in the process log.
- Done: no

### 4. Engine identity helper

- Changes: `IPlayers.viewOf(entity): V?` (the view whose entity is this entity, in slot order, or
  null); `slotOf` uses it.
- Check: engine tests for `viewOf` (registered entity, other entity, empty slots). Build and tests
  pass.
- Done: no

### 5. Player state, view accessors and the target helper

- Changes:
  - Core `PlayerStateComponent(damageTimer)`: `var ready`, `val damaged`; `reset()` does nothing.
  - `Megaman`: adds the component in `init()`; `ready` gets and sets the component's `ready`.
  - `MegaPlayer`: requires `PlayerStateComponent` and checks at registration that the entity is an
    `IDirectional`, a `MegaGameEntity` and an `IBehaviorsEntity` (a missing part fails with
    `MissingComponentException` for components, or an `IllegalArgumentException` for an interface).
    Accessors: `direction`, `dead`, `ready`, `damaged`, `isBehaviorActive(type)`, `feetFixture`,
    `leftSideFixture`, `rightSideFixture`, `damageableFixture` (from the same body properties that
    Megaman's fixture accessors read).
  - `MegaPlayers.targetFor(seeker): MegaPlayer` (slot 0), with a `TODO` comment that it always gives
    slot 0 and that the targeting design (item `029` `NOTES.md`, "Targeting design") comes with its
    first real caller; `val IMegaGameEntity.targetPlayer: MegaPlayer`.
- Check: core tests: registration fails without the component or the interfaces; each accessor reads
  through its part; `targetFor` gives slot 0 also when the player is not present; `Megaman.ready`
  reads and writes the component. Build and tests pass; a launch shows no exception.
- Done: no

### 6. Enemy sites that need a choice: identity, ownership, fixtures, stomp

- Changes, by hand:
  - Identity (18 lines): `== megaman` and `is Megaman` on a contact or damage entity become
    `players.viewOf(entity)`; a following read uses the view.
  - Ownership (5 lines): `owner == megaman` becomes `players.viewOf(owner) != null`.
  - Fixtures (9 lines): the Koopa shells and `JetpackBlaster` read the target's fixture accessors.
  - Stomp bounce (6 lines): the write goes through the view's body, unchanged, with a `TODO` comment
    at each site that every player bounces with Mega's jump velocity and that a bounce per character
    is for `029g`.
- Check: a review of each site against the old line. Build and tests pass. Launch `enemy_contact`
  and `enemy_owner`.
- Done: no

### 7. Enemy sites: proximity

- Changes, by hand: the 42 overlap and range lines. Overlap of a sensor or area with the player's
  body becomes `players.any { ... }`. Range or position checks that act on the player afterwards
  (for example aim after the check) stay with the target and move in step 8. Each choice is listed in
  the process log.
- Check: a review of each site. Build and tests pass. Launch `enemy_drop` and `enemy_chase`.
- Done: no

### 8. Enemy sites: the rest, and `FacingUtils`

- Changes: a script replaces each remaining read of `megaman` (and `game.megaman`) in
  `entities/enemies/` with `targetPlayer`, and removes the then unused `megaman` imports.
  `FacingUtils` reads `entity.targetPlayer`. The script output is checked by the contract grep and a
  review of the diff.
- Check: the contract grep finds only comments, log text, names and the two damage-class sets. Build
  and tests pass. Launch `enemy_aim`, `enemy_magfly` and `death`.
- Done: no

### 9. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test. Write the
  results in the process log.
- Done: no

### 10. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no
