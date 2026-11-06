# Plan: mega-refactor-081a-player-lifecycle

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-06

## Design notes

- New files in `players/` (core): `PlayerRules.kt` (the 2 rules and the damage functions), `PlayerLifecycle.kt` (spawn,
  destroy, room transition, out-of-bounds kill), and `PlayerDamageNegotiator.kt` (moved and renamed from
  `entities/megaman/MegamanDamageNegotiator.kt`). The names of the functions below are the plan's choice; a step may
  change a name if a clash shows, and writes it in the process log.
- Receivers: the functions need health, a body, facing and the event listener. They use a type parameter with bounds,
  not a new interface or a base class (`O1-R2`):
  ```kotlin
  fun <T> T.playerSpawnStart(spawnProps: Properties)
      where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IEventListener
  ```
  `AbstractHealthEntity` in a bound gets a `TODO` that points to `048`.
- The 2 rules, computed at each read, with `require(...)` in place of the silent queries:
  - `AbstractHealthEntity.playerInvincible`: `damaged || require(DamageRecoveryComponent).recovering ||
    require(LocksComponent).getLockGroup(DAMAGE).isLocked() || !existenceState.liveAfterUpdate ||
    require(PlayPhaseComponent)` not in play. Same terms, same order as `Megaman.kt:89`.
  - `AbstractHealthEntity.playerCanMove`: `!require(LocksComponent).getLockGroup(MOVEMENT).isLocked() &&
    !require(StunComponent).stunned && !damaged && !require(FreezeComponent).frozen`. A `TODO` points to item `126`.
  - `Megaman.invincible` and `Megaman.canMove` call them. The 2 old `TODO`s go.
- The spawn, in 2 calls, so that the order of the shared lines stays:
  - `playerSpawnStart(spawnProps)`: add the listener; place the body on the bottom center of the bounds; velocity zero,
    collision on, gravity on, fixtures active; `facing` from the spawn data; `services.gravity.set` from the spawn
    data; health to max; clear the `MOVEMENT` locks; clear the teleporter; clear the `DAMAGE` locks.
  - `playerSpawnEnd()`: stop the smooth follow; submit `PLAYER_SPAWN` for slot 0, with the `038` `TODO`.
  - `Megaman.onSpawn`: `super.onSpawn`, `playerSpawnStart`, then its own lines (`aButtonTask`, weapons and ammo,
    `canMakeLandSound`, scalars, `damageTimer`, timers, momentum reset, air dash), then `playerSpawnEnd`.
  - Order change: `aButtonTask = JUMP` and the weapon lines move after the health set and the lock clears. Step 4
    checks that they read nothing that those lines write. The gravity set stays before the lock clear.
- The destroy: `playerOnDestroy()` removes the listener and submits `PLAYER_JUST_DIED`, with a `TODO` for `119`.
  `Megaman.onDestroy` keeps the charge sound stop after it, as today.
- The room transition: `playerOnRoomTransition(event)` handles `BEGIN_ROOM_TRANS`, `CONTINUE_ROOM_TRANS` and
  `END_ROOM_TRANS` as `Megaman.kt:391-415` does, without the charge sound. `Megaman.onEvent` calls it for these 3
  keys, and keeps the charge sound stop for the first 2 keys after it.
- The damage:
  - `playerAllowsDamageFrom(damager)`: not live refuses; then the `rootPlayer` and `rules.friendlyFire` rule, with the
    `030` `TODO`. `Megaman.canBeDamagedBy` keeps its `super` call:
    `existenceState.liveAfterUpdate && super.canBeDamagedBy(damager) && playerAllowsDamageFrom(damager)` in the same
    order as today.
  - `playerAfterDamage(damager)`: knockback, freeze, `players.onPlayerDamaged`. `Megaman.takeDamageFrom` calls it
    after `super.takeDamageFrom`, then plays its sounds. Order change: the damage signal now comes before the 2 sound
    lines. The only listener is `AbstractBoss` (`AbstractBoss.kt:126`), and the only override is
    `PreciousWoman.onPlayerDamaged`, which resets a laugh timer and reads no sound state.
  - `playerEditDamage(baseDamage)`: the enhancement and the hard-mode +1, with the `023` `TODO`.
- The out-of-bounds kill: `killPlayerIfOutsideLevel()`, with the 1.5-tile offsets as constants in `PlayerLifecycle.kt`
  and the same error log text. The `Megaman` updatable calls it first, as today.
- `PlayerDamageNegotiator(private val player: MegaGameEntity)`: the same table and rules; `owner == player`. A `TODO`
  names the overrides parameter for `081b`. The `x6-intro-extra-level` branch must move its 7 entries into this file
  when it next merges `master`.
- `players/README.md`: the purpose (one lifecycle for every player class, no base class), the functions and the hook
  that calls each one, the order inside `onSpawn`, the components that each function requires, and a link to the
  `081` item folder. Model: `fights/README.md`.

### New APIs and their first callers

| API | First caller |
|---|---|
| `playerInvincible`, `playerCanMove` | `Megaman.invincible`, `Megaman.canMove` |
| `playerSpawnStart`, `playerSpawnEnd` | `Megaman.onSpawn` |
| `playerOnDestroy` | `Megaman.onDestroy` |
| `playerOnRoomTransition` | `Megaman.onEvent` |
| `playerAllowsDamageFrom` | `Megaman.canBeDamagedBy` |
| `playerAfterDamage` | `Megaman.takeDamageFrom` |
| `playerEditDamage` | `Megaman.editDamageFrom` |
| `killPlayerIfOutsideLevel` | the `Megaman` updatable |
| `PlayerDamageNegotiator` | `Megaman.damageNegotiator` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, item `081` `active`
  with the start date; `ITEM.md` "Branches". Then
  `git worktree add -b mega-refactor-081a-player-lifecycle .worktrees/mega-refactor-081a-player-lifecycle master`.
  Recheck the overlap with the other worktrees, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: yes

### 2. Tests first

- Changes: add the new functions as empty or `TODO()` stubs and the renamed negotiator, so that the tests compile. New
  tests in `core/src/test/.../players/`, on `SecondPlayerEntity` (extended as needed) or `Megaman` with
  `testServices()`: each check in the contract "Automated checks". Move `MegamanInvincibleTest` to the new rule and add
  the components that `require` needs to its setup.
- Check: `core:test` fails only on the new tests that need the moved code.
- Done: yes

### 3. The rules and the damage

- Changes: `playerInvincible`, `playerCanMove`, `playerAllowsDamageFrom`, `playerAfterDamage`, `playerEditDamage`;
  `Megaman` calls them; `PlayerDamageNegotiator` replaces `MegamanDamageNegotiator`.
- Check: `core:test` passes for these tests. A mutation check for each function (break 1 term, at least 1 test
  fails); write the results in the process log.
- Done: yes

### 4. The spawn, the destroy, the room transition and the kill

- Changes: `playerSpawnStart`, `playerSpawnEnd`, `playerOnDestroy`, `playerOnRoomTransition`,
  `killPlayerIfOutsideLevel`; `Megaman` calls them. Read the setter of `currentWeapon` (`MegamanWeaponsHandler.onChangeWeapon`),
  `setAllToMaxAmmo` and the `aButtonTask` writes, and confirm that they read no health and no lock; write the result
  in the process log.
- Check: `core:test` passes. A mutation check for each function; write the results in the process log.
- Done: yes

### 5. README, imports and the full checks

- Changes: `players/README.md`. Remove the imports that this branch made unused in the changed files (count each
  imported name, because the compiler does not warn). Search for `MegamanDamageNegotiator` in code and in the docs of
  active branches.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. `Megaman.kt` has no lifecycle line left
  that the scope lists as shared.
- Done: yes

### 6. Launch checks

- Changes: none.
- Check: a `timeout 12` launch of each room in the contract "Manual playthrough", one launch per room, from the
  worktree. A clean exit passes. Write the results in the process log. Then the owner plays the contract checks.
- Done: yes
