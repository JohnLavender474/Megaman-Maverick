# Plan: mega-refactor-029f-level-players

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-24

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`. Smoke launches are short (about 20 seconds); a clean exit with no exception is
a pass. The owner plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass. Each new API comes in the
same step as its first caller.

New APIs and their first callers in this branch:

| API | First callers (step) |
|---|---|
| `MegaPlayer.currentHealth`, `maxHealth`, `translateHealth` | `PlayerStatsHandler`, `LevelPauseScreen` (3) |
| `MegaPlayer.megaman` (typed accessor, `players/`) | `PlayerStatsHandler`, `LevelPauseScreen`, `GetWeaponScreen`, `MegamanMaverickGame` (3); `MegaLevelScreen`, the handlers (4, 5) |
| `MegaPlayer.spawn(props)`, `MegaPlayer.focus` | `MegaLevelScreen` (4) |

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029f` row in `ITEMS.md` to `active` with the
  start date; mark `029f` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-029f-level-players
  .worktrees/mega-refactor-029f-level-players master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 138 core) and the warning count
  (expected 114) in the process log.
- Done: yes

### 3. Health reads, the `megaman` accessor; HUD, pause, get-weapon screen, game reset

- Changes:
  - `MegaPlayer`: `currentHealth`, `maxHealth`, `translateHealth(value)` (through the `IHealthEntity` it
    already requires).
  - `players/`: `val MegaPlayer.megaman: Megaman` (the entity, cast), with the `024`/`029g` `TODO`.
  - `PlayerStatsHandler` takes the game; a private lead-player property (`PlayerSlot(0)`, `047` `TODO`);
    health through the view; weapons and ammo through `.megaman`. `MegaLevelScreen` passes the game.
  - `LevelPauseScreen`: the lead-player property replaces `megaman`; health through the view
    (`hasMaxHealth` becomes `currentHealth >= maxHealth`, as `IHealthEntity.hasMaxHealth`); weapons through
    `.megaman`.
  - `GetWeaponScreen`: the current weapon of the lead player through `.megaman`.
  - `MegamanMaverickGame.resetPlayerProgress`: the buster for each player (`players.forEach`, through
    `.megaman`).
- Check: tests for the health reads and the accessor in `MegaPlayersTest`. Build and tests pass. Launch
  `--room pickups`.
- Done: yes

### 4. Level screen

- Changes in `MegaLevelScreen`:
  - a private lead-player property replaces `megaman`, with the `047` `TODO`;
  - `MegaPlayer.spawn(props)` and `MegaPlayer.focus` (the entity must be `IFocusable`), added here with
    their callers: the spawn on `PLAYER_SPAWN_INIT`, the camera focus, the snap-back check;
  - `players.forEach` for `canBeDamaged` on `BOSS_DEFEATED` and `INTERMEDIATE_BOSS_DEAD`, and for
    `running = false` (through `.megaman`) after a room transition;
  - the lead player for the checkpoint, the gravity aura, the boss-defeat dead checks, pause and forced
    resume, the death handler; `047` `TODO`s;
  - the projectile cleanup keeps a projectile when `owner.asPlayer != null`.
- Check: tests for `spawn` and `focus` in `MegaPlayersTest`. Build and tests pass. Launch `--room lives`.
- Done: yes

### 5. Spawn, death and end-level handlers

- Changes: each handler gets a private lead-player property (`047` `TODO`) in place of its
  `game.megaman` field; generic state through the view (`ready`, `canBeDamaged`, `setAllBehaviorsAllowed`,
  `body`, `direction`, `facing`); the beam sprite priority, flip and the current weapon through `.megaman`;
  the event slot from the view.
- Check: a review that each read happens after the registration (the handlers are made in
  `MegaLevelScreen.init`, which runs after `players.register` in `postCreate`). Build and tests pass. Launch
  `--room player_death`.
- Done: yes

### 6. Removals

- Changes: `MegamanMaverickGame.megaman` becomes a local value in `postCreate` (make, `init`, listener,
  register); the extension `IMegaGameEntity.megaman` goes; `AbstractItem` reads `game.gravity.direction`;
  `MegamanTrailSprite` (V1) is commented out in full with its `DecorationsFactory` pool entry and a `TODO`.
- Check: the contract grep (no `game.megaman`, no field, no extension). Build and tests pass. Launch
  `--room lives`.
- Done: yes

### 7. Contract checks

- Changes: none, unless a check fails.
- Check: a clean build; the automated checks in `CONTRACT.md`; the owner's play test. Write the results in
  the process log.
- Done: yes

### 8. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: no
