# Plan: mega-refactor-029c-boss-players

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-23

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from
`build/test-results`. Smoke launches are short (about 20 seconds); a clean exit with no exception is
a pass.

Each step is its own commit. After each code step, the build and the tests pass.

New APIs and their first callers in this branch (the "production caller" rule):

| API | First callers |
|---|---|
| `PlayerStateComponent.shooting` | `AbstractEnemy.isMegamanShootingAtMe` (through the view) |
| `PlayerStateComponent.canBeDamaged` | `Megaman.canBeDamaged`; `WilyFinalBoss` (through the view) |
| `PlayerStateComponent` damage listeners | `Megaman.takeDamageFrom`; `AbstractBoss` (through the view) |
| `MegaPlayer.facing` (get and set) | boss facing reads; `WilyFinalBoss` write |
| `MegaPlayer.direction` setter | `MoonMan` gravity change |
| `MegaPlayer.invincible` | `TimberWoman.shouldJumpSpin` |
| `MegaPlayer.shooting` | `AbstractEnemy.isMegamanShootingAtMe` |
| `MegaPlayer.canBeDamaged` (get and set) | `WilyFinalBoss` |
| `MegaPlayer.addDamageListener`, `removeDamageListener` | `AbstractBoss` |
| `IPlayerDamageListener` | `AbstractBoss`, `PreciousWoman` |

The landing checks use the existing body extension (`player.body.isSensing(...)`), so the view needs
no `isSensing` accessor.

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `029c` row in `ITEMS.md` to `active` with the
  start date; mark `029c` active under "Branches" in `ITEM.md`; set the branch `process-log.md` state
  to `active`. Then `git worktree add -b mega-refactor-029c-boss-players
  .worktrees/mega-refactor-029c-boss-players master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 470 engine, 107 core) and the warning count
  (expected 114) in the process log.
- Done: no

### 3. Owner's baseline check

- Changes: none. The owner plays the contract's manual checks on this baseline code (the test rooms
  and boss stages exist already; no new test rooms).
- Check: the results go in the process log.
- Done: no

### 4. Shared player state and view accessors

- Changes:
  - `PlayerStateComponent(damageTimer, shootAnimTimer)`: `val shooting` (from the shoot animation
    timer), `var canBeDamaged = true`, and the damage listener set (`add`, `remove`, `forEach`).
  - `Megaman`: `canBeDamaged` reads and writes the component; `shooting` reads the component's value
    (the setter still resets or ends `shootAnimTimer`); the component is made with both timers.
  - `MegaPlayer`: requires `IFaceable` and `IDamageable` too; `var facing`, `var direction`,
    `invincible`, `shooting`, `var canBeDamaged`, `addDamageListener`, `removeDamageListener`.
- Check: core tests for each new part and accessor, and for `Megaman.canBeDamaged` and
  `Megaman.shooting` through the component. Build and tests pass; a short launch.
- Done: no

### 5. Damage listener rename

- Changes: `IPlayerDamageListener.onPlayerDamaged(damager, player: MegaPlayer)` replaces
  `IMegamanDamageListener`. `Megaman.takeDamageFrom` calls the component's listeners at the same point,
  with its view (`players.viewOf(this)`); Megaman's own listener set goes away. `AbstractBoss` adds
  itself to every registered player in `onSpawn` and removes itself in `onDestroy` (`players.forEach`),
  with a `TODO` for players who join during a fight (`047`). `PreciousWoman` overrides the new method.
- Check: a core test that a listener in the component gets `onPlayerDamaged` with the view when
  `Megaman` takes damage, if practical with the relaxed game mock. Build and tests pass.
- Done: no

### 6. `AbstractEnemy` and `AbstractBoss`

- Changes: owner identity uses `players.viewOf(owner)`; `isMegamanShootingAtMe` uses
  `players.any { it.shooting && ... }` with the facing check on the same player; the floating points
  direction uses `targetPlayer.direction`; the drop odds, the one-up and `hasEnhancement` keep the
  global with a `TODO` that names `029e`.
- Check: a review of each line. Build and tests pass. Launch `enemy_aim`.
- Done: no

### 7. Bosses

- Changes:
  - By hand: identity (`GutsTankFist`, `GutsTank` x3) and ownership (`GutsTankFist`, `ReactorMan`) use
    `players.viewOf`; the writes (`WilyFinalBoss` defeat: `canBeDamaged` and `facing`; `MoonMan`:
    `direction`, skipping a dead player) use `players.forEach`; the four landing checks use
    `players.all` with the `047` `TODO`; `ReactorMonkey.hasHeartTank` keeps the global with the `029e`
    `TODO`.
  - By script: the remaining `megaman.` and `getMegaman().` reads become `targetPlayer.`; a check of
    the diff that each changed line differs only in the reference.
- Check: the contract grep for bosses. Build and tests pass. A short launch of a boss stage.
- Done: no

### 8. Old bosses

- Changes: comment out `ReactorMan_OLD`, `MechaDragon_OLD` and `SphinxMiniBoss` in full, each with a
  short `TODO` at the top that the code may be out of date; comment out their three entries (and
  imports) in `BossesFactory`.
- Check: a grep that nothing else names them. Build and tests pass.
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
