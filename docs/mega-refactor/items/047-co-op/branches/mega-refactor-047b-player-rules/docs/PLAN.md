# Plan: mega-refactor-047b-player-rules

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes")

Each step has one commit. Stage files by path, not with `git add -A`. Run all commands in the worktree. Gradle runs with `JAVA_HOME=$HOME/.jdks/corretto-17.0.13`.

The site fixes come before the new default. So at no commit does a site read the wrong player.

## New APIs and their first callers

| New API | First caller | Step |
|---------|--------------|------|
| `LaunchOptions.mode`, `DesktopAppArgs.mode`, `ResolvedLaunch.mode` | `resolveLaunchOptions`, `create()` | 3 |
| `GameMode`, `SessionRules`, `MegaServices.rules` | `Megaman.canBeDamagedBy` | 3, 4 |
| A `rules` value on `megaContactRules`; `ProjectileRule` becomes a class | `createGameEngine()` | 4 |
| A function that gives the player at the root of an owner chain | `Megaman.canBeDamagedBy`, `SessionRules.isFriendlyFire` | 4, 10a |
| `SessionRules.isFriendlyFire` | `ProjectileRule`, `PreciousGem.canBeDamagedBy` | 10a |
| `MegaPlayers.isLive`, `firstLive`, `anyLive`, `forEachLive` | `targetFor` and the site fixes | 10a |
| A player value on `addHealth` and `addWeaponEnergy` | `MegaLevelScreen.onEvent` | 9, 10a |

## Steps

### 1. Test rooms

- Changes:
  - A subagent adds 2 rooms to `RefactorTest047.tmx` with the `level-builder` skill. Each room has a player spawn, a painted `tiles` layer and floor at the slot 1 point.
  - `target_nearest`: aiming enemies in the middle, with space for a player on each side.
  - `pickups`: a `HealthBulb`, a `WeaponEnergyBulb`, a `Life`, and a hazard that hurts but does not kill.
- Check: one `timeout 12` launch of each room on the baseline code gives no exception. `./gradlew core:test` passes.
- Done: yes

### 2. Baseline measure

- Changes: none in code. A `--diagnostics` run with no `--players` in `DESERT_MAN` `room1`. The result goes in the process log. If a scripted run cannot move the player, the owner runs it.
- Check: the diagnostics file exists, and the `diagnostics-analyzer` skill reads it.
- Done: yes

### 3. Launch flag and session rules

- Changes:
  - Add `mode` to `LaunchOptions`, `--mode` to `DesktopAppArgs` and the launch print. `resolveLaunchOptions` gives a `GameMode`: `SINGLE` with 1 player, `COOP` with 2 and no flag. A bad value, and either mode with 1 player, add an error.
  - Add `GameMode` and `SessionRules` (one field, `friendlyFire`). `create()` builds the rules from the launch. `MegaServices` takes them, and `testServices()` gives the rules of `SINGLE` by default.
- Check: new cases in `LaunchOptionsResolverTest`, and a test of the rules of each `GameMode`. `./gradlew core:test` passes.
- Done: yes

### 4. Friendly fire

- Changes:
  - Add the root-owner function, 2 levels deep, as in `Megaman.canBeDamagedBy`, with a `TODO` for `030`.
  - `Megaman.canBeDamagedBy` refuses damage from another player with friendly fire off. This covers the needle spin.
  - `ProjectileRule` becomes a class that takes the rules. With friendly fire off, a projectile from a player ignores the fixtures of another player. Pass the rules through `megaContactRules` and `createGameEngine()`.
  - Update `ContactRuleSelectionTest` if the change needs it.
- Check: the 2 new tests in the contract. `./gradlew core:test` passes. One `timeout 12` launch of `default_spawn` with `--players 2`, and one with `--mode pvp`, give no exception.
- Done: yes

### 5. `ChargedShot`

- Changes: `ChargedShot.kt:118-119` reads the owner when it is a player, and the target when it is not. The `TODO` there changes to `030` only.
- Check: `./gradlew core:test` passes.
- Done: yes

### 6. Site fixes

- Changes:
  - `players.any` at `GroundSlideBlocker.kt:46,47`, `Lift.kt:119`, `Gate.kt:348`, `FallingIcicle.kt:168-170`. Keep the test of a dead player where `Lift` needs it. The comment at `Gate.kt:345-347` changes.
  - `players.forEach` at `SwimBooster.kt:60,61`.
  - `SpiderWeb` stores the stuck player at `stickToMegaman()` and clears it in `onSpawn`. All stuck reads use it (`:125,153,156,191,192`). The stored player must stay readable in `onDestroy`.
  - `BigAssMaverickRobotHand.isBeingStoodUpon` tests any player. `BigAssMaverickRobot.kt:489-491` skips the launch when no hand is free, and tries again after `launchHandDelay`.
- Check: the `SpiderWeb` and `Lift` tests in the contract. `SpiderWebDestroyTest` changes to match. `./gradlew core:test core:smokeTest` passes.
- Done: yes

### 7. Nearest default

- Changes:
  - `MegaPlayers.targetFor` gives the nearest player with `existenceState.liveNow && inPlay`, with a tie to the lower slot. It reads a position only when it has a second candidate. A seeker with no body gets the first live player. With no live player, it runs today's loop.
  - The `TODO` comments at `targetFor` and `targetPlayer` name `112`.
- Check: the `targetFor` test in the contract. `MegaPlayersTest.kt:87-101` changes to match. `./gradlew core:test core:smokeTest` passes. One `timeout 12` launch of `target_nearest` with `--players 2`, and one with no flag, give no exception.
- Done: yes

### 8. Enemy review

- Changes:
  - A read-only subagent reviews the 320 `targetPlayer` lines in 97 enemy files, and `FacingUtils.kt:23`. It looks for a site that stores state from the target, or that starts an action on one read and ends it on another.
  - A fault that the nearest default causes gets a fix here. Any other fault goes in `docs/TODO.md`.
  - If a fix changes the size of this branch much, stop and tell the owner.
- Check: the review result goes in `docs/NOTES.md`. `./gradlew core:test` passes.
- Done: yes

### 9. Pickups

- Changes:
  - `MegaLevelScreen.onEvent` reads the slot in `ADD_PLAYER_HEALTH` and `ADD_WEAPON_ENERGY`. `PlayerStatsHandler.addHealth` and `addWeaponEnergy` act on that player. The other functions stay on slot 0.
  - Confirm how slot 1 reads its maximum health after a heart tank. If it does not get the shared value, tell the owner before a change.
- Check: the pickup test in the contract if `testServices()` can reach the handler. If it cannot, write this in the process log. `./gradlew core:test` passes. One `timeout 12` launch of `pickups` with `--players 2` gives no exception.
- Done: yes

### 10. Docs and final checks

- Changes:
  - Update the root `CLAUDE.md`: `--mode`, `SessionRules`, `MegaServices.rules`, and the `megaContactRules` values.
  - A second `--diagnostics` run, as in step 2. The result goes in the process log.
  - Search for `TODO`s that this branch makes wrong. Confirm that each `--room` in the contract has a player spawn.
  - Write the backlog and bug candidates in `docs/TODO.md`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Done: yes

### 10a. Fixes from the code review

- Changes:
  - Friendly fire tests the root player of the target, and `PreciousGem.canBeDamagedBy` uses it. `isFriendlyFire` is a member of `SessionRules`. `IOwnable` has an `IGameEntity` bound, and `asPlayer` takes `IGameEntity?`.
  - `anyLive` and `forEachLive` at the 8 sites of this branch. `targetFor` takes a `MegaGameEntity`.
  - `BodyGravityChangeRule` takes `WorldGravity`, so it needs no cast.
  - `players` is a member of `MegaGameEntity`, and `targetPlayer` moves to `MegaGameEntity.kt`.
  - `addHealth` and `addWeaponEnergy` take the player. `--mode` uses `parseEnumOption`. Accepted edge cases get a `TODO`.
- Check: new tests for `canBeDamagedBy`, the gem and the live functions. `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. One launch of each changed room from the built jar gives no exception.
- Done: yes

### 11. Owner play test

- Changes: fixes from the play test. After a fix, the control checks run again.
- Check: the manual checks in `CONTRACT.md`, with the experimental checks first. The result goes in the process log.
- Done: yes
