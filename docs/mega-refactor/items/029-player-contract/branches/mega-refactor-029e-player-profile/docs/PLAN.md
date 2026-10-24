# Plan: mega-refactor-029e-player-profile

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-23

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`. Smoke launches are short (about 20 seconds); a clean exit with no exception is
a pass. The owner plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| `GameState.getLives(slot)`, `addLife(slot)`, `removeLife(slot)`, `livesExhausted()`, `resetLives()`, `setLives(value)` | `LevelPauseScreen`, `Life`, `AbstractEnemy`, `MegaLevelScreen`, `MainMenuScreen`, `MegamanMaverickGame` |
| `GameState.containsWeapon`, `addWeapon` | `MegamanWeaponsHandler.hasWeapon`, `LevelPauseScreen`, `PlayerStatsHandler`, the level override |
| The reward table (levels package, out of the `Megaman` companion) | `GameState.addLevelDefeated`/`removeLevelDefeated`, `Megaman.onAddLevelDefeated` (`WEAPONS_ATTAINED`) |
| `GameState.addToHealthTanks`, `hasAnyHealthTanks` | `PlayerStatsHandler`, `LevelPauseScreen` |
| `MegaPlayer.healthRatio` | `AbstractEnemy` (`HealthBulb` drop odds) |

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029e` row in `ITEMS.md` to `active` with the
  start date; mark `029e` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-029e-player-profile
  .worktrees/mega-refactor-029e-player-profile master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 113 core) and the warning count
  (expected 114) in the process log.
- Done: no

### 3. Test room (subagent)

- Changes: a subagent adds the room `lives` to `RefactorTest029.tmx` with the `level-builder` skill: a
  player spawn, painted tiles, a floor, and one `Life` item (properties copied from a real map, for
  example `IntroStage_v3.tmx`). No other room changes.
- Check: a short launch of `--room lives` shows no exception.
- Done: no

### 4. Save-string test and `GameState` profile calls

- Changes:
  - First, a test that pins today's `GameState.toString()` for a fixed set of data, and that
    `fromString` of it gives back the same data. It passes on the code before the changes.
  - Lives in `GameState`: a shared `Points` (0 to 9, start 3) with `getLives(slot)`, `addLife(slot)`,
    `removeLife(slot)`, `livesExhausted()`, `resetLives()`, `setLives(value)`. `TODO`s: per-player lives
    at the count (`047`, decision H); the game-over rule with more players at `livesExhausted` (`047`).
    Not in `toString`; `reset()` does not touch it.
  - `addToHealthTanks` and `hasAnyHealthTanks` move from `Megaman` to `GameState` unchanged; the
    `MegamanHealthTankTest` cases move with them.
  - The `Megaman` wrappers delegate to `GameState` for now, so no caller changes in this step.
- Check: tests for the lives calls and the moved health-tank cases; the save-string test still passes.
  Build and tests pass.
- Done: no

### 5. Owned weapons in `GameState`; the handler reads them

- Changes:
  - The reward table moves out of the `Megaman` companion to the levels package.
  - `GameState` gets the owned-weapon set: `containsWeapon` (true for `MEGA_BUSTER` at all times),
    `addWeapon`; `addLevelDefeated` adds the reward weapons, `removeLevelDefeated` removes them, `reset`
    clears the set. Not in `toString`.
  - `MegamanWeaponsHandler` makes one runtime entry for each weapon at creation; `hasWeapon` reads
    `game.state`; `putWeapon` and `removeWeapon` go. `Megaman.clearWeapons` and `putWeapon` go.
    `Megaman.onAddLevelDefeated` only sets `WEAPONS_ATTAINED`; `onRemoveLevelDefeated` goes.
  - The level override calls `state.addWeapon`. `resetPlayerProgress` sets the current weapon to
    `MEGA_BUSTER`, as `clearWeapons` did.
- Check: tests for the set (reward table on level add and remove, override, reset, the buster) and for
  `fromString` giving the reward weapons; the save-string test still passes. A review that each reset path
  keeps its current-weapon reset. Build and tests pass. Launch `--room pickups` (the override weapons are
  in the pause screen).
- Done: no

### 6. Heart tanks: max health from `GameState`

- Changes: Megaman's heart-tank listener sets the max health to `START_HEALTH + count × HEALTH_BUMP` from
  `game.state`. `TODO`s: the read-through max health at the set (backlog candidate in `docs/TODO.md`); late
  join (heart tanks, weapons) at the player registration in `MegamanMaverickGame` (`047`).
- Check: a test that the max health follows heart tanks added and removed. Build and tests pass.
- Done: no

### 7. Profile reads move to `game.state`

- Changes:
  - Every caller of the `Megaman` wrappers reads `game.state`: `MegaLevelScreen` (`removeLife` with the
    slot of `PLAYER_JUST_DIED`; `livesExhausted`; `resetLives`), `MainMenuScreen`, `MegamanMaverickGame`
    (reset, override `setLives`, load), `LevelPauseScreen` (`getLives(PlayerSlot(0))`, tanks, weapons),
    `PlayerStatsHandler` (tanks, weapons), `MegamanJetpackHandler`, `HealthTank`, `HeartTank`,
    `ReactorMonkey`, `AbstractBoss`, `AbstractEnemy`, and the player components that read enhancements.
  - `AbstractEnemy`: the one-up calls `addLife(targetPlayer.slot)` with a `047` `TODO`; the `Life` drop
    odds read `getLives(targetPlayer.slot)`; the `HealthBulb` odds read `targetPlayer.healthRatio` with a
    `047` `TODO`. `MegaPlayer.healthRatio` is added.
  - The wrappers on `Megaman` go. The `029e` `TODO`s go.
- Check: the contract grep (no profile read through `megaman`, no `029e` `TODO`). Build and tests pass.
  Launch `--room lives` and `--room player_death`.
- Done: no

### 8. `ItemEntity` takes the player view

- Changes: `ItemEntity.contactWithPlayer(player: MegaPlayer)`; the 7 items use `player.slot` (`Life`:
  `state.addLife(player.slot)`); `PlayerItemRule` uses `asPlayer` and loses its cast and `TODO`.
- Check: a test that `PlayerItemRule` passes the view, if practical. The contract grep for `Megaman` in
  `ItemEntity` and `PlayerItemRule`. Build and tests pass. Launch `--room pickups`.
- Done: no

### 9. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test. Write the results in
  the process log.
- Done: no

### 10. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no
