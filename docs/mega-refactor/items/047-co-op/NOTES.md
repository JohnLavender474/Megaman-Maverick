# Notes: 047 Co-op with multiple controllers

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

All controller and behavior logic assumes one player. Add support for more than one controller
and a co-op mode.

## `047b` scan (2026-11-01)

Line numbers are from `master` at `3e05f8cff`. These are recommendations from the scan. The owner's decisions are in `ITEM.md`, "Decisions". The `AbstractBoss` component and the hold time are not decided yet. The branch list still fits `108` and `109`, because all 4 topics apply to both.

Owner's input (2026-11-01, verbatim):

> Since friendly fire is one of the open questions, I think it needs to be part of a suite of toggles that each change value depending on whether the gameplay is single player, co-op, or PvP.

> I imagine many entities will have different rules about which player to target, and so being able to have a component/decorator/inheritance setup might be good.

### Findings

- Friendly fire: `Megaman.canBeDamagedBy` refuses only the player's own shots (`Megaman.kt:426-432`). `Bullet.kt:142` and `ChargedShot.kt:145-148` explode when a target refuses damage. So a change to `canBeDamagedBy` alone makes shots explode on a teammate, and they do not pass through. A teammate's shield also deflects a shot (`Bullet.kt:156`). One filter in `ProjectileRule.begin` covers all projectiles, for `BODY` and `SHIELD`.
- The owner check goes 2 levels deep. `047b` can use the same check with a `TODO` for `030`.
- Targeting: 613 `targetPlayer` lines in 145 entity files go through `MegaPlayers.targetFor`, which ignores the seeker. `hasBody` does not test if a player is alive.
- A new default also reaches readers that are not enemy aim. 23 files in `blocks`, `sensors`, `special`, `hazards` and `projectiles` read `targetPlayer`, for example `Gate`, `Lift` and `RisingLavaRiver`. Each one needs a review.
- `ChargedShot.kt:118-119` reads `targetPlayer`, so a nearest default makes its known fault worse. It must read the owner first.
- Pickups: each item sends the slot. `MegaLevelScreen.kt:621-644` drops it, and `PlayerStatsHandler.kt:58` acts on slot 0. Lives and currency are one shared pool.

### Recommended: session rules

A typed, read-only rules object, chosen at session start and read through `MegaServices`. This agrees with `O2-R8`. `047b` builds only the friendly fire toggle, because it is the only one with a reader in that branch. The other toggles come with the branches that read them. The current behavior is the PvP value of friendly fire.

| Toggle | Single | Co-op | PvP | Branch |
|---|---|---|---|---|
| Friendly fire | off (no effect) | off, shots pass through | on | `047b` |
| Lives | one pool | shared or per player | none | `047c` |
| Result of a death | level reset | the 3 options in `ITEM.md` | respawn | `047c` |
| Respawn place and delay | checkpoint | near the lead | spawn point choice | `047c` |
| Gravity scope | world | world or per player | to decide | later |
| Damage between players | not used | not used | size score or per weapon | `023`, `024` |

A team field has no reader with 2 players. It waits for `109` and `030`.

### Recommended: targeting

- Use the component and policy design in `029` `NOTES.md`, "Targeting design": `TargetPolicy`, and `TargetingComponent` with a default when the component is absent. A decorator such as `Sticky(Nearest)` is a policy that wraps a policy. Inheritance is rejected (`048`).
- `targetFor` stays as the temporary dispatch point, so the 613 lines need no edit. The removal of `targetPlayer` from entities with no target is a separate change.
- The default is `Nearest`, with no stored state. A target can flip when the two players are at almost equal distance, so the play test must check this.
- The first user of the component is `AbstractBoss`, with `Sticky(Nearest)`, and the hold lasts until the target dies. The scope step must confirm this against each boss.

### Other recommendations

- `047b` builds the small rules object now, and `102` extends it.
- `--mode coop|pvp` chooses the rules. `--players 2` defaults to co-op. `--mode pvp` with 1 player is a launch error.
- Health and weapon energy go to the player who collects them. Lives, currency, heart tanks and health tanks stay shared. Slot 1 has no HUD until `047d`.

## Agent report: Can `Megaman` be pooled? (2026-11-02)

### Report: can `Megaman` be pooled?

Paths: `C/` = `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/`. `E/` = `/home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/`. Line numbers are from `master` at `b3e792bc4`. I changed no file and ran nothing.

**Short verdict:** the stated reason ("one singleton") is outdated. The design is not. A player is slot-owned, and a pool adds almost nothing. Do not pool. The real driver for a change is "a slot's entity can change" (`047f`, `081`). That work needs no pool.

#### 1. What a pool would add

A pool in this code does 2 things: it reuses an object, and it treats `destroy` as release (`GameEntityPoolCreator.kt:15`). A slot needs the first thing only in a few cases. The second thing is wrong for a player, because the slot keeps the player through death.

| Case | New `Megaman` objects | When |
|---|---|---|
| Today, 1 or 2 players | 1 or 2 | Once, at boot (`MegamanMaverickGame.kt:448`) |
| N deaths and respawns (`MegaLevelScreen.kt:549-553`) | 0 | |
| `047c` P2 respawn | 0 | |
| `047f` join and leave, capacity 2 | 1 if the object is kept for a rejoin; otherwise 1 per join | First join |
| Capacity 4 | Up to 3 more | First joins |
| `081a` X in slot 1 behind a flag | 1 X | Launch, 0 swaps |
| `081` step 5 or `113`: the level picks the character | At most slots × characters objects | Level load |
| `095` net session | Capacity only | Session start. `O2-multiplayer.md:34` fixes players at session start |

- A pool holds no more objects than a slot-owned spare would.
- It saves one thing: `init()` on the second and later uses of a class. A kept object saves the same `init()`.
- Compare `Bullet`, where each shot is a fetch.
- The pool starts empty (`DEFAULT_START_AMOUNT = 0`). So with no preload, the first join still runs a full `init()` inside a frame.

**`init()` facts, verified:**
- 137 defs × 10 weapon atlases = 1370 `containsRegion` calls. Each charge def adds inner calls.
- `containsRegion` is `findRegion` (`TextureAtlasExtensions.kt:5`).
- I counted the region names in the 10 `Megaman_v3*` atlases: 431 animations.
- `MegamanAnimations` is per instance, with no shared cache (`MegamanAnimations.kt:46`).

#### 2. Remaining singleton assumptions

**Only 2 sites hold one instance for the session:**

| Site | What it does | Effect if the slot's object changes |
|---|---|---|
| `MegamanMaverickGame.kt:454` | `gameState.addListener`, once. `removeListener` (`GameState.kt:48`) has 0 production callers | The old object still gets events. The new one gets none |
| `MegaLevelScreen.kt:337` | `setFocus` in `init()`, once. `CameraFollow` has no `reset` | The camera follows the old body, with no error |

**Assumes slot 0 is never empty (the getters read on each use, so they survive a new instance):** 8 `get(PlayerSlot(0))!!` sites.
- `MegaLevelScreen.kt:181`
- `PlayerStatsHandler.kt:58`
- `PlayerSpawnEventHandler.kt:66`
- `PlayerDeathEventHandler.kt:47`
- `EndLevelEventHandler.kt:71`
- `LevelPauseScreen.kt:111`
- `GetWeaponScreen.kt:322`
- `MegaPlayers.kt:77`

**Assumes the slot outlives the level:** `GetWeaponScreen.kt:153,195` reads `currentWeapon` after `reset()` ran `engine.reset()`.

**Assumes the entity is initialized while unspawned:**
- `PlayerSpawnEventHandler.kt:126-131` writes `body` before the spawn.
- `WorldGravity.kt:14` writes all registered players.
- `MegamanMaverickGame.kt:822` writes `currentWeapon`.
- `:337` reads `focus`.

**Assumes the class, not the instance:** 9 `as Megaman` lines in screens and the game.
- `MegamanMaverickGame.kt:822`
- `EndLevelEventHandler.kt:173`
- `GetWeaponScreen.kt:153,195`
- `MegaLevelScreen.kt:624`
- `LevelPauseScreen.kt:286,569`
- `PlayerStatsHandler.kt:106,115`

**Launch-only:** `MegamanMaverickGame.kt:319-323` makes the slot 1 device only with `--players 2`.

**Held for a short life only:**
- `owner` on shots
- `SpiderWeb.kt:85`
- the `collector` closure at `PlayerStatsHandler.kt:301-305`
- `AbstractHealthEntity.lastDamager`

**No assumption:**
- `MegaPlayers.kt:31` closes over the slot, not the entity.
- `MegaPlayer` no longer exists (0 hits).
- HUD lambdas read through a getter.
- `AbstractBoss.kt:128` connects to a service signal.
- `LevelRoomHandler` takes a supplier.

**Registered means joined.** 120 `players.forEach/any/all/none` lines read all registered entities, and `MegaLevelScreen.kt:550-552` spawns each one. So a spare must stay unregistered until the join. See also `docs/issues/2026-11-02-players-any-counts-dead-slot.md`.

#### 3. Steps for a fetch through `MegaEntityFactory`

**First blocker.** `pool.onSupplyNew` puts `runnablesOnDestroy[POOL]` on the entity. Each death then frees the player while the slot still holds it. The next respawn puts the same object in play while it is also in the free queue. A second `fetch` then gives a live player. `Players.kt:16` throws at that `register`. `onSpawnRejected` (`MegamanMaverickGame.kt:1005`) can also free it. So a player cannot use the pool as other entities do.

**Fetch, in order:**
1. `fetch(Megaman::class)`.
2. `runnablesOnDestroy.remove(ConstKeys.POOL)`. The object now returns only through a manual `free`.
3. `if (!initialized) { init(); initialized = true }`.
   - It must be before `register`. `MegaPlayers.kt:31` uses `?.`, so with no `init()` the bind does nothing and no error shows.
   - `init()` adds a new `ControllerComponent` with `input = null`. The player then reads `RELEASED` for ever (`EntityQueries.kt:37`).
   - `init()` has no slot, and this is safe: all slot reads are in lambdas (`SpritesComponent.kt:135`, `ControllerComponent.kt:115`, `AirDashBehavior.kt:38,43`).
   - Do not let `MegaEntityFactory.kt:42` do this `init()` without the flag. See "Double `init()`" in section 4.
4. `register(slot, entity)`. It adds `PlayerComponent`. The class is in no system mask, so the add is safe on a reused entity. The slot must be empty (`Players.kt:15`), so a swap does `unregister` first, inside one runnable.
5. `gameState.addListener(progress)`, then set the max health from the heart tanks. `setMaxHealthFromHeartTanks` is private (`PlayerProgressComponent.kt:22`). See the `TODO` at `MegamanMaverickGame.kt:446`.
6. Slot 0 only: run `setFocus` again.
7. Set the phase to `PRE_PLAY`. Clear the tint.
8. Spawn props.
   - Level start: the existing chain.
   - Join: a new path. It passes `DIRECTION = gravity.direction`, because `Megaman.kt:329-331` writes world gravity for all players (default `UP`).
   - Add a path to `IN_PLAY`. Only `PlayerSpawnEventHandler.kt:213` sets it.

**Release, in order:**
1. `destroy()` is only queued. Wait for `destroyNow`.
2. Unregister after that, for example from `runQueue`.
   - `Megaman.onDestroy` runs `super` first, then reads `playerSlot!!` at `:374`.
   - A destroy runnable or an early `unregister` therefore gives a null failure there.
   - The same read runs in `GameEngine.reset` and `dispose` (`GameEngine.kt:251,264`).
   - On a live entity, `Megaman.kt:364`, `AirDashBehavior.kt:38,43` and `ControllerComponent.kt:115` also throw.
3. `gameState.removeListener(progress)`.
4. Set `ControllerComponent.input = null` (the `TODO` at `MegaPlayers.kt:29`). `input` then reads `RELEASED`, which is safe.
5. `unregister(slot)`. This removes the `PlayerComponent` key. Without it, the next `register` throws at `Players.kt:16`.
6. `MegaEntityFactory.free(entity)`. The leave flow frees, never the engine.

**Other limits:**
- **`PLAYER_JUST_DIED`:** a leave fires it, so `GauntletMaster.kt:168` resets its waves. An owner-based destroy also runs in `PreciousGemCluster.kt:109`. A leave needs its own event or a flag.
- **Shots of a player who left:** `rootPlayer` gives `null`, and `Megaman.kt:435` then allows damage. Inference: these shots can hurt teammates.
- **Slot 0:** it cannot be released at all today. Level-scoped fetch and free fail at `:337` and `GetWeaponScreen.kt:153`.

#### 4. The "not pool-safe" list, checked

"Respawn" below means the level chain. It gives at least 2.1 s of updates before `IN_PLAY` (0.25 + 1 + 0.5 + 0.35), and the longest weapon timer is 1 s.

| Item | Claim true? | Bug at respawn today? | Bug for reuse or join? |
|---|---|---|---|
| `PlayPhaseComponent.reset()` is empty | Yes (`:7`) | No. `PlayerSpawnEventHandler.kt:126-127` sets it | Yes |
| `weaponsHandler.reset()` has 0 callers: `spawned` sets | Yes | **Yes.** See below | Yes |
| Same: cooldowns and inner timers | Yes | No, they end in the beam-in | Slight: up to 1 s after a join |
| `jetpackHandler.reset()` runs at 2 behavior ends | Yes (`JetpackBehavior.kt:68`, `AirDashBehavior.kt:138`, second only with `RUSH_JET`). A destroy ends active behaviors, so it runs then too | No. Ammo fills at spawn | No |
| `slash_index` | Survives | No. `resetTimer` ends first | No |
| `shoot_down` | Survives for up to 0.3 s. `delayRun` clears it, and `updatables` still run | No | No |
| `shoot_down_id` | Survives | No. A stale string only | No |
| `airDashDirection` | Survives | No. Written at `:95`, read only while active | No |
| `currentAnimKey` | Survives | No. `roomTransition` is false and `!inPlay` gives `"spawn"` | Only a join during a transition |
| `delayRun` work | Yes. Nothing cancels it, and `updatables` is never cleared | **Yes, small:** `throwAxe` has no live check (`:660`, `:1006`), with a 0.1 s window | Same |
| Trail and bubble timers | Yes | No. Owner decision | No gameplay bug |

**The `spawned` bug.** `cullAllDead` tests only `liveAfterUpdate` (`:99`). A dead player does not cull. A shot that returned to its pool and was reused by another owner stays in the set.
- Same-class fetchers exist: `MoonMan.kt:671`, `PreciousWoman.kt:926`, `LumberJoe.kt:455`, and the other player.
- With 2 players, slot 1 dead while slot 0 fires `MOON_SCYTHES` is the plain case. The dead slot's count grows, and its `postShoot` can fade the other player's scythes. This is inference from the code, not run.

**Missed by the list:**
- **POOL runnable against a slot:** the first blocker in section 3.
- **Second `init()`:** it would replace `ControllerComponent` (the bind is lost), `PlayerProgressComponent` (the listener is orphaned) and `FocusComponent`.
- **Max health:** a new object has the base value until a heart tank changes. Join only.
- **World gravity in `onSpawn`:** see fetch step 8. Join and virus.
- **Tint:** `SpritesComponent.kt:135` never clears it. Only when an object moves from slot 1 to slot 0.
- **`RoomTransitionCarryComponent.reset()`:** it leaves `carriedX/Y`. `beginRoomTransitionCarry` clears them, so only an `END` with no `BEGIN` can reach them: a join during a transition.
- **Unspawned listener:** `onCurrencyAdded` queues a sound on an unspawned entity. Inference: it plays at the next spawn.
- **`spawnedUpdateFunc`:** it casts and moves a stale `PreciousGemCluster` (`:373-390`). This is part of the `spawned` bug.

**Double `init()`: confirmed.** `MegaEntityFactory.kt:42` leaves the flag false, and `GameEngine.kt:122` runs `init()` again. This hits 25 `Bullet` and 25 `Block`. `Block.kt:57` adds a second debug shape supplier each time. It is a real bug, separate from this question, and it has no issue file yet.

**Owner decision conflicts:**
- `084` `ITEM.md:89` (trail and bubble timers survive) conflicts with `CLAUDE.md:155` ("Reset all per-spawn state") only if the player is reused. The effect is bounded by the timer delay. Keep the decision.
- Inference: reuse across sessions in one JVM can start 2 net peers with different timers. Trail and bubble timers spawn entities, so this is a desync risk for `(d)` only.
- `029` `NOTES.md:758` already says "never from `MegaEntityFactory`". That is a recommendation, not a recorded decision.

#### 5. Options

| | (a) Slot-owned for the session | (b) Pool-safe state, same instance | (c) Created on join, released on leave | (d) Full pool |
|---|---|---|---|---|
| Load | 1 `init()` per launch slot, as today | Same | Same for launch slots. Slot 1 pays at first join, in a frame, unless built at a load point | Same as (c). A preload pays per instance at boot, for all sessions |
| Enables | No new feature | Correct co-op respawn | `047f`, the level picks the character, `113` | (c), plus no second `init()` on a rejoin. A kept object in (c) gives this too |
| Code | `CLAUDE.md:57,157`, the comment at `MegamanMaverickGame.kt:450` | `Megaman.onSpawn`, carry `reset()`, a guard in `throwAxe` | Claim and release flows; `setFocus` moves when slot 0 can change | (c), plus POOL removal, the factory flag, and all of (b) |
| Risks | Does not cover join | Order: `reset()` must run after `Megaman.kt:338`, or `onChangeWeapon` no longer destroys the clusters | A stall in a frame; empty-slot `!!`; shots of a player who left | Double ownership; reuse across slots; pool key problem; a new static dependency |

**New or first-use APIs:**

| API | First real caller | Production caller today |
|---|---|---|
| `MegamanWeaponsHandler.reset()` (exists) | `Megaman.onSpawn`, after `:338` (`047c`) | None |
| `Players.unregister` (exists; closes the `O1-R5` row) | `047f` leave flow | None |
| `MegaPlayers.unregister` override (unbind) | `047f` leave flow | None |
| `GameState.removeListener` (exists) | `047f` leave flow | None |
| Public max-health sync | `047f` join flow | None |
| Join claim function | `047f` join flow | None |
| Spawn-time `setFocus` (a moved line, no new API) | `081` step 5 | Not needed before that |

(d) adds 0 new APIs that (c) lacks. It adds 2 costs:
- `MegaEntityFactory.kt:27` is static session state (`O1-R1`, `096`), and `Players` is already session-owned.
- A pool key is a class. If `081` picks one `PlayerEntity(services, def)`, a pool cannot tell Mega from X, and it needs a key per def.

**Recommendation:**
1. **Now:** (a), as a docs change. New reason: "slot-owned: one entity per slot, created and released by the slot's owner, not by `destroy`". This agrees with `O2-R5` ("Identity is the slot").
2. **`047c`:** the (b) subset with real effect: the `spawned` clear and the `throwAxe` live check. File an issue for each. Skip the fields that are harmless.
3. **`047f`:** (c), with the object kept for a rejoin. `081` step 5 adds the swap and `setFocus`.
4. **Reject (d).** `045` does not own this: it covers `GameObjectPools` scratch objects, not entity pools.

**The real driver:** `O2` "Done means" requires join and leave during play, and `081` open question 1 lets a level pick the character. Performance is not the driver, and neither is pooling.

#### 6. Virus recovery, and `MegamanClone`

No text in the repo names the virus flow or `MegamanClone`. Grep gave 0 hits in `docs` and `core/src/main/kotlin` for "virus", and 0 hits for clones in the item and roadmap files. I used your description.

| Option | "Same instance" and "respawn" are one object? |
|---|---|
| (a) | Yes |
| (b) | Yes. `onSpawn` resets run at recovery too |
| (c) | Yes while the player stays. A leave during recovery releases the object; that case needs a rule |
| (d) with POOL kept | **No.** `destroyNow` frees the object, and `spawnNow` runs it while it is in the free queue (`GameEngine.kt:226,235`) |
| (d) with POOL removed | Yes |

The engine supports this path (`GameEngine.kt:90-96`). Slot, input and listener all survive. These points hold in all options:
- `destroy` fires `PLAYER_JUST_DIED` for slot 1. The level screen returns at `:584`, but `GauntletMaster` resets its waves, and any player-owned `PreciousGem` or `PreciousGemCluster` is destroyed.
- A direct `spawn` skips `PlayerSpawnEventHandler`, so the phase and the locks do not change.
- The spawn must pass the current gravity `DIRECTION`.
- `onSpawn` fills health and ammo.

**`MegamanClone`:** no change in any option. A clone has no slot, so it fails at `Megaman.kt:364,374,456` and `ControllerComponent.kt:115`. It would also write world gravity at `:329`. A clone churns, so the pool fits it. Reject (d) or accept it, a separate class stays correct.

#### What I did not read in full

- `029` `NOTES.md` (grep and parts only).
- `MegaLevelScreen.kt` (lines 150-709, 1055-1110, and grep).
- `MegamanMaverickGame.kt` (lines 250-640, 800-830, 985-1020).
- `GameState.kt`, and the `O1` and `O2` objective files (parts).
- The behaviors other than `AirDashBehavior`, and the first part of `GroundSlideBehavior`. For the rest I only searched for state.
- `BodyComponent.kt`.
- `GroundSnow.feetTimers` (it holds an entity key; not traced).
- The `047a` and `047b` branch folders.
- `ROADMAP.md`. It has uncommitted edits in the main checkout, and I read it by grep only.

Inferences are marked. Nothing was run.

#### Direct answer

The reason is outdated, and the decision stands. Only 2 sites still bind one instance for the session. Player entities can be pooled only if they leave the pool's core rule, and then the pool is a spare list with new risks. More work is needed, but that work is slot replacement, not pooling.

#### Recommendation

(a) now, the `spawned` and `throwAxe` fixes in `047c`, (c) in `047f`, and the swap in `081` step 5. No (d).

#### Risks

- Slot replacement can cause the camera to follow a dead object with no error (`:337`).
- A mid-play join stalls a frame for 1370 lookups and 431 animations.
- An empty slot 0 throws at 8 sites.
- `Megaman.kt:374` throws if `unregister` runs too early.
- `reset()` placement can change the guard cluster cleanup.
- The shot behavior of a player who left is not checked.

#### Open questions for the owner

1. Change the rule text in `CLAUDE.md:57,157` to "slot-owned"? Recommend yes.
2. Does a released object stay for a rejoin, or does each join build a new one? Recommend keep it, unregistered. Then `(b)` state fixes must hold for that reuse.
3. When is slot 1 built: at boot, at a lobby, or at the join? Recommend at a load point or lobby, not in play. This depends on `102`.
4. Does `081` pick thin classes or one `PlayerEntity(def)`? Recommend deciding this before any swap work. One class needs a way to rebuild on a def change, because `init()` is not safe to repeat.
5. Does a leave fire `PLAYER_JUST_DIED`? Recommend no: a separate event or flag, so a leave does not reset a gauntlet.
6. Can slot 0 leave? Recommend no until `047` closes "What happens when the lead player leaves?".
7. Where does the double `init()` fix go? Recommend one `docs/issues/` file now, fixed in `MegaEntityFactory.kt:42`, independent of this work.

##### Critical Files for Implementation

- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/handlers/MegamanWeaponsHandler.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`
