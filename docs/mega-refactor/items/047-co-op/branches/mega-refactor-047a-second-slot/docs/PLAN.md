# Plan: mega-refactor-047a-second-slot

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes")

Each step has one commit. Stage files by path, not with `git add -A`. Run all commands in the worktree.

## New APIs and their first callers

| New API | First caller | Step |
|---------|--------------|------|
| `LaunchOptions.players`, `DesktopAppArgs.players` | `resolveLaunchOptions`, the launcher | 2 |
| `ResolvedLaunch.players` | `postCreate()` | 4 |
| `PadShare` (`ALL`, `ALL_BUT_LAST`, `LAST_ONLY`), a `GamepadInput` constructor value | `create()` | 3 |
| `slotSpawnProps(slot, spawnProps)` in `screens/levels/spawns/` | `PLAYER_SPAWN_INIT` in `MegaLevelScreen` | 5 |

## Steps

### 1. Test map

- Changes:
  - A subagent builds `assets/tiled_maps/tmx/RefactorTest047.tmx` with the `level-builder` skill. The rooms are
    `default_spawn`, `linked_spawn`, `slot_0_death`, `slot_1_death` and `transition`. Each room has a player spawn, a
    painted `tiles` layer and floor at the slot 1 point.
  - `linked_spawn` has a `player_2` reference to a rectangle in a `player_slot_spawns` layer.
  - The two death rooms have a hazard that kills a player.
  - Add `REFACTOR_TEST_047` to `LevelDefinition.kt`, with type `TEST_LEVEL`, and to `LevelCompletionScreensTest.kt`.
- Check: `./gradlew core:test` passes. One `timeout 12` launch per new room on the baseline code gives no exception.
- Done: yes

### 2. Launch flag

- Changes:
  - Add `players` to `LaunchOptions`, and `--players` to `DesktopAppArgs` and the launch print.
  - `resolveLaunchOptions` accepts `1` and `2`, gives the value in `ResolvedLaunch`, and adds an error for other values.
- Check: new cases in `LaunchOptionsResolverTest`. `./gradlew core:test` passes.
- Done: yes

### 3. Slots, frames and the pad split

- Changes:
  - `MegamanMaverickGame.kt:236` builds `MegaPlayers(2)`. `GamePlayersFieldsTest.kt:30` changes to 2. The constructor
    default stays 1.
  - An array of `InputFrameBuilder` per slot replaces `slot0FrameBuilder`. The frame step builds a frame for each
    registered slot. `TURN_CONTROLLER_*` set `locked` on all builders. `inputLocked` reads slot 0.
  - `GamepadInput` takes a `PadShare`. `ALL` is today's behavior. `ALL_BUT_LAST` skips the last pad when 2 or more pads
    exist, and has no pad when 1 exists. `LAST_ONLY` reads only the last pad. The array loop does not allocate.
  - `LAST_ONLY` logs one error when it has no pad, and logs again only after a pad has connected.
  - Add a `TODO` at the split for the swap during play, which points to the device claim in `047f`.
- Check: new cases in `GamepadInputTest` for 0, 1 and 2 pads with each `PadShare`. `./gradlew core:test` passes. One
  `timeout 12` launch of `default_spawn` with no flag gives no exception.
- Done: yes

### 4. Slot 1 entity and tint

- Changes:
  - With `players == 2`, `postCreate()` builds, initializes and registers a second `Megaman` in slot 1, and adds its
    `PlayerProgressComponent` as a game state listener. `create()` builds the slot 1 `GamepadInput` with its own
    `PlayerSettingsStore(PlayerSlot(1))`, and gives slot 0 `ALL_BUT_LAST`.
  - The main sprite pre-process in `SpritesComponent.kt` sets a debug tint for a slot that is not 0, with a `TODO` that
    the tint is temporary (item `047`).
- Check: `./gradlew core:test` passes. One `timeout 12` launch with `--players 2` gives no exception. Slot 1 does not
  spawn yet.
- Done: yes

### 5. Slot 1 spawn

- Changes:
  - `slotSpawnProps` copies the spawn props and replaces the bounds. With a `player_<slot.index + 1>` reference, it uses
    that rectangle. With none, it moves the bounds `slot.index` tiles along `direction.getRotatedClockwise()`.
  - `PLAYER_SPAWN_INIT` spawns each registered slot after slot 0.
  - The phase, lock and gravity steps in `PlayerSpawnEventHandler` (`init` and `beamTrans`) act on all players.
  - `Megaman.onSpawn` submits `PLAYER_SPAWN` only for slot 0, with a `TODO` that points to `038`.
- Check: a new test for each `direction` with no reference, and for a reference. `./gradlew core:test` passes. One
  `timeout 12` launch of `default_spawn` and `linked_spawn` with `--players 2` gives no exception.
- Done: yes

### 6. Death

- Changes:
  - The `PLAYER_JUST_DIED` case in `MegaLevelScreen` returns early for a slot that is not 0.
  - For slot 0, it destroys each other live player.
  - Add the `TODO` for the other two death options from `ITEM.md`.
  - Confirm that the slot 1 locks are clear at the next spawn.
- Check: a test of the slot filter if `testServices()` can reach it. If it cannot, write this in the process log, and
  the manual checks cover it. `./gradlew core:test` passes.
- Done: yes

### 7. Level end

- Changes:
  - The locks at `EndLevelEventHandler.kt:114-115` and the phase at `:166` act on all players.
  - Find where the `LEVEL_END` locks clear, and confirm that this also covers slot 1, also when slot 1 is dead at level
    end.
- Check: `./gradlew core:test` passes. A short launch of `DESERT_MAN` with `--room room16` and with no flag gives no
  exception.
- Done: yes

### 8. Docs and final checks

- Changes:
  - Update the root `CLAUDE.md`: the `--players` flag, and capacity 2.
  - Search for `TODO`s that this branch makes wrong, for example `MegaPlayers` and `MegamanMaverickGame.kt:446`.
  - Write the backlog and bug candidates in `docs/TODO.md`.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes.
- Done: yes

### 9. Owner play test

- Changes: fixes from the play test. After a fix, the control checks run again.
- Check: the manual checks in `CONTRACT.md`, with the experimental checks first. The result goes in the process log.
- Done: yes
