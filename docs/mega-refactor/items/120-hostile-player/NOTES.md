# Notes: 120 A player that turns hostile and comes back

Research for this item. Each report is kept in full, with its own date and `master` commit. A note above each report says what later research or an owner decision replaced. If a report and `ITEM.md` disagree, `ITEM.md` wins.

## Agent report: Hostile AI P2 as a boss (2026-11-02)

Note: Replaced later: the "fight entity" is the plain `Fight` of `118`. The health route, the death-fixture write and the full-health write on P2 are removed (see `ITEM.md`). The `BulletBill` section stands.

### Report: a hostile AI 2nd player as a boss

Code basis: `master` at `49fdd425f`. The work tree has one change, to `docs/mega-refactor/ROADMAP.md`, which does not touch this research. I changed no files.

Paths have these roots:
- `E/` is `engine/src/main/kotlin/com/maverick/game/engine/`.
- Other code paths are under `core/src/main/kotlin/com/megaman/maverick/game/`.

#### 1. Run-time component changes in this engine

**What is possible**

| Fact | Evidence |
|---|---|
| `addComponent` and `removeComponent` are plain map writes. They do not notify anything. | `E/entities/GameEntity.kt:37-45` |
| The map key is the exact class. A second add of the same class replaces the first. | `GameEntity.kt:38` |
| Membership is computed at spawn, after `onSpawn`. | `E/GameEngine.kt:119-131` |
| `updateSystemMembershipsFor` is public and has 1 caller, `spawnNow`. | `GameEngine.kt:130,211` |
| A live entity leaves a system at that system's next `update`, through the `qualifies` sweep. The sweep runs also when `on` is false. | `E/systems/GameSystem.kt:73-99`; test `GameSystemHooksTest.kt:102-112` |
| `add()` in a system that is updating goes to a queue. | `GameSystem.kt:57-65` |
| All 14 systems have a mask of 1 component class. `BossComponent` is in no mask unless we add a system. | `GameSystem(...)` lines |

**Destroy.** `destroyNow` does these steps in order: it removes the entity from the systems (`onEntityLeft`), calls `reset()` on each component, then calls `onDestroy()` (`GameEngine.kt:186-192`). It does not remove components.

**Pool.** A pooled entity keeps its components. `init()` runs once (`GameEngine.kt:122,133-136`). The pool only frees the entity through `runnablesOnDestroy[POOL]` (`entities/factories/GameEntityPoolCreator.kt:15`).

**`Megaman`.**
- `init()` is called by hand, and `initialized` is set to true. `Players.register` then adds `PlayerComponent` after `init()` and before spawn (`MegamanMaverickGame.kt:449-456`, `E/players/Players.kt:17`).
- So `Megaman.init()` does not know its slot. It cannot add a component only for slot 1.
- `Megaman` is not pooled. The same object respawns at `MegaLevelScreen.kt:549-553`.

**Production use.** `removeComponent` has 7 lines: `MegaGameEntity.kt:49`, `ForceDecoration.kt:56`, `Force.kt:96`, `GravityChange.kt:59`, `Water.kt:126-127`, and `Players.kt:24`.
- All but `Players.kt:24` run in `onSpawn`, so `spawnNow` refreshes membership. I checked `Water.kt` and `MegaGameEntity.kt` by reading the code, and the other 3 through their `onSpawn` line numbers.
- `Players.kt:24` has only test callers.
- I found no live add or remove of a masked component. Caution: I did not review each of the 1,239 `addComponent` lines. `018` `NOTES.md` has an older scan with the same result.

**Not possible today**
1. A live entity does not join a system after `addComponent`. Code must call `updateSystemMembershipsFor`, or the entity must respawn.
2. There is no hook or event for a component change.
3. There is no automatic removal at destroy. A component added at run time stays through destroy, pool and respawn.
4. `init()` does not run again.
5. Two components of one class, or a lookup by base class, are not possible.
6. Inference: a `reset()` must not remove a component, because `destroyNow` iterates that map. `GauntletMaster.kt:73-74` states the same rule for `runnablesOnDestroy`.
7. `SessionRules` cannot change. It is a `val` data class, passed by value into `ProjectileRule` (`MegamanMaverickGame.kt:438,950`).

#### 2. The four forms

##### (a) Attached at `init()`, dormant
- **Membership:** no change if `BossComponent` has no mask. With a mask, every `Megaman` is in that system in every session.
- **Readers:** no change if they use the event payload. `hasComponent(BossComponent)` no longer means "in a fight".
- **Spawn:** `reset()` returns the component to dormant at destroy. This is correct.
- **What breaks:**
  - Every `Megaman` carries it, including slot 0 and single player, because `init()` has no slot.
  - The config (`boss_key`, `mini`, `end`, music) cannot come from spawn props, because a player's spawn props come from the player spawn. So this form needs a separate begin call.
  - Boss hooks must live in `Megaman` code. This is against the goal of `084`.
- **Engine change:** none.

##### (b) Added at the transition, removed after
- **Membership:** with no mask, it is 1 map write and membership does not matter. With a mask, code must call `updateSystemMembershipsFor` by hand. Removal then depends on the sweep that `018` plans to delete.
- **Readers:** same as (a).
- **Spawn:** the component stays after destroy, so each exit must remove it. The safe place is a `runnablesOnDestroy` entry, not `reset()`. With a mask, a stale component makes the entity join the system again at respawn (`GameEngine.kt:130`).
- **What breaks:** the fight and the entity have different lifetimes. `P2` exists before and after the fight, and it can leave the fight while it is alive.
- **Engine change:** none if there is no mask.

##### (c) Fight state in a separate owner
Build the owner as an **encounter entity**, not as a field in `MegaLevelScreen`. It is a `MegaGameEntity` with a `BossComponent` that it adds in its own `init()`. `P2` is only a reference.

Precedent: `GauntletMaster` has no body, holds a state enum, gets its update from `UpdatablesComponent`, listens to events and owns level flow (`entities/special/GauntletMaster.kt:60-125`). It also watches other entities through `runnablesOnDestroy.put` (`:227`), and it destroys children that leave the room so that the gauntlet cannot soft-lock (`:54`).

- **Membership:** no run-time change. The component is present at `init()`.
- **Readers:** they read the encounter entity from the payload, the same as for an ordinary boss. `BossHealthHandler` needs only an `IHealthEntity` (`screens/levels/stats/BossHealthHandler.kt:25,39`), and `Megaman` is one. So `BOSS_READY` needs 1 new payload key for the health owner.
- **Spawn:** the encounter entity gets `reset()` and `onDestroy` from the normal path, and a pool gives it a clean reuse. `P2` keeps its normal spawn path.
- **What breaks:** nothing in the engine. The player rules in section 4 must change.
- **Engine change:** none. `MegaLevelScreen` also gets no new field and no new entry in its fragile `when` chain (`:920-925`).

##### (d) Destroy `P2`, spawn a separate boss
- **Membership:** normal spawn.
- **Readers:** none change. An `AbstractBoss` subclass works through today's path.
- **Spawn:** `engine.destroy(P2)` sends `PLAYER_JUST_DIED` for slot 1, and the screen ignores that slot (`MegaLevelScreen.kt:583-584`). The boss destroys itself on `PLAYER_SPAWN` (`AbstractBoss.kt:167`). `P2` returns at `:550-553`.
- **Rule cost:** none. It needs no `FactionComponent`, no `SessionRules` change and no target change. `P1` is the only live player, and the boss is not a player.
- **What breaks:**
  - The boss is a second implementation. Only `Megaman` has a `ControllerComponent`, and its behaviors do not transfer. `031` `NOTES.md` advises against conversion of bosses to input.
  - Health, weapon state and AI state do not carry over unless spawn props pass them.
  - Shots in flight keep owner `P2`, and `isPlayer` stays true. So they stay player shots.
- **Engine change:** none.

##### Recommendation: (c), as an encounter entity
If the boss fight must use Mega Man's moves through the same AI, use (c). If the boss has its own state machine, (d) is the correct form, and it is simpler. (c) and (d) use the same `BossComponent`, so `048` does not need to choose.

(b) with no mask also works. Its one gain is `reset()` at `P2`'s destroy. But in (b) nothing would read the component from `P2`'s map, so the attachment gives almost nothing, and each exit must remove it.

#### 3. Entity state and encounter state

**The rule:** for an ordinary boss, the entity lifetime equals the fight lifetime, so the state can be on the entity. For `P2` the lifetimes differ. So `BossComponent` must be on the entity whose lifetime equals the fight.

| Part | Today | Belongs to | Reason |
|---|---|---|---|
| `bossKey`, `mini`, `end`, music | `AbstractBoss.kt:123-125,137` | Encounter | Map config. Read by `Gate.kt:200` and `MegaLevelScreen.kt:720,766,803`. |
| `ready` | `:98`, 18 `isReady` overrides | Encounter phase | The hook is per boss. Also gates `canDamage` (`:171`). |
| `betweenReadyAndEndBossSpawnEvent` | `:107` | Encounter phase | The boss class reads a level-flow phase. |
| `defeated`, `defeatTimer` | `:101`, `:196-210`; 94 lines in 23 boss files | Encounter phase | Ordinary bosses also read it as their own gate. |
| Exit event | `getEventOnDefeated` `:228`; 1 override | Encounter | Chooses `VICTORY_EVENT` or `INTERMEDIATE_BOSS_DEAD`. |
| Health bar fill | `BossHealthHandler` | Encounter | Already outside the entity. |
| `BOSS_READY`, `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `BOSS_DEAD` | `:209,:243,:271`; `MegaLevelScreen.kt:716-796` | Encounter | Level flow. |
| `BEGIN_BOSS_SPAWN` | `BossSpawnEventHandler.kt:26` | Nobody | It has no listener. |
| `IBossListener.onBossDefeated(boss)` | 3 overrides | Encounter | In the 3 overrides, `boss` is read only for a log. |
| Damage table, `editDamageFrom` | `:60-92,:175-181` | Entity capability | Not fight state. |
| Death immunity | `:118,:141`; read in `DeathRule.kt:19-23` | Entity capability | Not fight state. |
| No drops, no cull | `:127-128` | Entity capability | Spawn settings. |
| Depletion route | `:217-226` | Entity capability, a settable route | `048` risk "Boss death". |
| Orbs, defeat explosions | `:154,:278-302` | Entity presentation | Uses the body. |

**Does this change `BossComponent` for ordinary bosses?** It changes the shape once and in a limited way:
- `BossComponent` holds only the "Encounter" rows: key, mini, end, music, phase, defeat timer, hooks and exit event.
- Damage table, death immunity, drop, cull and depletion route go to the general health, damage and cull parts. A boss sets them in `init()` or `onSpawn`.
- `BossComponent` stays a single class, added at `init()`. A later `BossSystem` is safe, because the component never changes at run time in this design.

#### 4. The transition, step by step (recommended form)

**Step 1: Trigger.** Reuse the boss room path: `LevelRoomHandler.kt:128`, then `MegaLevelScreen.kt:691-713`, then `BossSpawnEventHandler.init`. The map object names the encounter class.
- `BossSpawnEventHandler.kt:21` must stop casting to `AbstractBoss`. `048` does this anyway.
- The controller is already off, because `LevelRoomHandler.kt:54` turned it off and `:135` does not turn it on for an event room.
- A trigger in the middle of a room can spawn the same entity instead.
- Both players arrive stacked at 1 point (`Megaman.kt:390-395`).

**Step 2: Encounter `onSpawn`.** It reads the same props as `AbstractBoss.kt:123-137`. It takes `P2` and `P1` from `players.get(slot)`. Then it writes on `P2`:
- `lock(MegaLockGroup.DAMAGE, <new reason>)`, for no hits in the intro. `Megaman.kt:89` already honors this lock.
- `setHealthToMax()`.
- `ENTITY_KILLED_BY_DEATH_FIXTURE = false`. Fixture properties are not needed, because `DeathRule.kt:19-20` tests this one first.
- The depletion route, so that 0 health calls the encounter and not `destroy()`. Today this can use `PointsComponent.putListener`, which returns the old listener (`E/points/PointsComponent.kt:35`). `PointsSystem` polls the listener each frame.
- A `runnablesOnDestroy` watcher (see step 9).

**Step 3: Intro.** The encounter runs `preReady` and `isReady` in its `UpdatablesComponent`. `P2` is scripted.

**Step 4: Ready.** `onReady` submits `BOSS_READY`, with the encounter as `BOSS` and `P2` as the health owner. `MegaLevelScreen.kt:716-747` fills the bar and turns systems off. The encounter sets `P2`'s side to hostile here, and it gives the AI `P1` as its target.

**Step 5: `END_BOSS_SPAWN`.** `MegaLevelScreen.kt:750-754` turns the controller and systems on. The encounter plays music and removes the intro lock.

**Step 6: Fight.**

| Part of `P2` | Change | Today |
|---|---|---|
| Side | Hostile | No side exists. See the rule problems below. |
| Input | None if the AI writes slot 1 | `ControllerComponent.input` is a `var`. |
| Weapons | None | 12 `OWNER pairTo megaman` sites; shots already name `P2` (`MegamanWeaponsHandler.kt:766-1275`). |
| Health | Route changed | `MegamanDamageNegotiator.kt:40-44` scores by size (2, 3 or 4), not by weapon. |
| Contact | None | Filter and fixture types stay. `getType()` must stay `MEGAMAN`, because `MegaGameEntities.kt:21,26` keys on it in `add` and `remove`. |
| Camera | None | The focus is set once to slot 0 (`MegaLevelScreen.kt:337`). |
| `targetPlayer` | AI uses the reference | `targetFor` includes the seeker, so `P2` as seeker gets `P2` (`MegaPlayers.kt:39-54`). |

**Step 7: Defeat.** The route calls `triggerDefeat`, which submits `BOSS_DEFEATED`. `MegaLevelScreen.kt:756-783` then does this:
- It turns the input off, with the system left on.
- It locks all players (`:774`), so `P2` is invincible.
- It destroys `IBossListener` entities (`:778-780`).
- It removes the bar.

The encounter runs the defeat timer, destroys `P2` and submits `BOSS_DEAD`. If `P1` is dead at either event, the screen does nothing (`:759,:788`), the same as today.

**Step 8: Exit.** `:793-795` submits `VICTORY_EVENT` or `INTERMEDIATE_BOSS_DEAD`. `Gate.kt:196-209` unlocks by key. The encounter destroys itself.

**Step 9: `P2` leaves with no defeat.** `Megaman.kt:486-489` destroys it outside the level, with no health change. Ordinary bosses have no equal case. The watcher must call the defeat flow, or a boss gate stays locked. Inference: this is a soft-lock risk. Remove the watcher outside its own run, as `GauntletMaster.kt:193` does.

**Step 10: `P1` dies.**
- `:586` destroys `P2`. `:591-593` removes a life and starts the death flow.
- `:604-618` goes to game over or to respawn.
- `:528-553` removes the bar and spawns both players.
- `Megaman.kt:363-364` sends `PLAYER_SPAWN` for slot 0.
- The encounter destroys itself on `PLAYER_SPAWN`, as `AbstractBoss.kt:167` does.
- `P2` is an ally again, because its side and route reset in `reset()` at destroy. The encounter's cleanup removes the death property.
- A re-entry to the room runs the fight again.

**Where `MegaPlayers` and `SessionRules` get in the way.**

Player-against-player damage is not possible outside `PVP`, with one exception (see the last bullet):
- `Megaman.canBeDamagedBy` needs `friendlyFire` (`Megaman.kt:434-435`).
- `ProjectileRule` drops the contact (`world/contacts/rules/ProjectileRules.kt:29`), so shots pass through. `SessionRules.kt:9-14` makes this decision, and `PreciousGem.kt:208` also reads it.
- Do not swap the rules at run time. Keep `SessionRules` read-only and add a side test: a hostile pair is never friendly fire. That is 2 sites, `SessionRules.isFriendlyFire` and `Megaman.canBeDamagedBy`.
- `GameMode` needs no new row. `--mode pvp` removes the block, but it also allows hits before the transition.
- `PVP` itself has no match code. It is 1 enum row and a launch flag (`SessionRules.kt:17-21`, `LaunchOptionsResolver.kt:93`).
- Exception: `rootPlayer` walks 2 levels only, so a longer chain counts as an enemy's and can hit a player (`TODO` at `players/PlayerExtensions.kt:11`).

Other effects of `P2` staying `isPlayer`:
- 155 `.asPlayer` lines treat `P2` as a player. The side readers move with `030`.
- The body readers keep working, and these produce hostile-player side effects:
  - `P2` can collect items (`PlayerItemRule.kt:17`). A health pickup can stop the world (`047` `ITEM.md`, on `PlayerStatsHandler.kt:317`).
  - `P2` can trigger gates (`GateRule.kt:13-14`).
  - `P2` sets world gravity (`BodyGravityChangeRule.kt:24`), which can turn the camera (`Megaman.kt:143-162`).
- Enemies still target `P2` if it is nearest. `112` owns the fix.

`P2` does not need `unregister`. That API has only test callers and does not unbind input (`MegaPlayers.kt:29`).

#### 5. What an AI 2nd player needs

**Exists now**
- `ControllerComponent.input: InputSource?` as a `var` (`E/controller/ControllerComponent.kt:11`). `ControllerSystem` skips a null source.
- `InputSource.read()`, `InputFrame`, `InputFrameBuilder` with a lock, and `PlayerInputs[slot]`.
- `MegaPlayers.register` binds a slot source (`MegaPlayers.kt:31`).
- `Megaman` reads through `IGameEntity.input` (`E/entities/EntityQueries.kt:37-41`), so it does not know the source.
- A slot 1 `Megaman` exists with `--players 2`. `players.settings(slot)` holds bindings per slot, but only device code uses them.

**Missing**
- Code that computes held bits.
- A step before `ControllerSystem` that computes the bits once per tick. `031` `NOTES.md` says that Mega reads about 44 times per frame and that `read()` must return a cached frame.
- A launch or session way to say "slot 1 is AI". `launch.players > 1` builds a gamepad source for slot 1 (`MegamanMaverickGame.kt:321-323`), and `:583` writes the frame only from that device.
- Seeded random numbers for the AI (`031`, design point 3).

**A constraint for the AI design:** AI bits must go through `frameBuilders[1].build(held)` at `MegamanMaverickGame.kt:583`.
- `BOSS_DEFEATED` keeps `ControllerSystem` on (`MegaLevelScreen.kt:768-772`). Only the builder lock stops input (`:690-697`).
- A private source that skips the builder keeps pressing buttons after defeat.
- `InputSource` alone is therefore not enough.

#### 6. Effect on earlier conclusions

| Earlier conclusion | Verdict | Change |
|---|---|---|
| No `EnemyComponent` | Stands | None. |
| No "is this a boss?" reader | Stands, and it gets stronger | No code must use `hasComponent(BossComponent)` as a role test. |
| `BossComponent` holds fight state | Stands, refined | It excludes capability data, and it goes on the entity with the fight's lifetime. |
| Readers get the entity from a payload | Stands, 1 addition later | `BOSS_READY` needs a separate health owner for the future scenario only. |
| `FactionComponent` on actors only | Stands, 3 additions | `Megaman` gets one. The side must become writable and must reset at destroy (future). New readers: `Megaman.canBeDamagedBy`, `SessionRules.isFriendlyFire`. |
| Capability data replaces identity checks | Stands | `P2` can use the same fields. |
| `MegaContactFilter.kt:106` plain field | Stands | `P2` keeps `false`. Hostility is decided in the damage gates. |

**`BulletBill`: the earlier claim does not hold.**

What it does today (`entities/enemies/BulletBill.kt:40`):
- It extends `AbstractEnemy` and implements `IProjectileEntity`.
- It uses these enemy abilities: health, damage from player shots, cull, drops, floating points, the enemy contact filter and level cleanup by `ENEMY` type.
- It uses these projectile abilities: `owner`, `canDamage` not owner, `hitBlock` through the `PROJECTILE` fixture, and `explodeAndDie`.
- Kotlin forces 3 manual merges:
  - `canDamage` calls both supers (`:104-107`).
  - `getType()` is chosen by hand (`:178`).
  - Only the enemy cull path is used (`:64`). `defineProjectileComponents` is not called.

With components, it adds health, damage, cull, item-drop and projectile parts. The gates combine by a generic AND, and `!knockedToDeath` is a normal entity gate. `DeathRule.kt:27-29` becomes 2 component tests in the same order. No code names `BulletBill`.

The side check:
- Its side as an actor is enemy.
- Its source is the launcher (`BulletBillLauncher.kt:153-155`), which is not a player.
- Today's rule is "no player at the root means hostile to players" (`Megaman.kt:434`), so the two answers agree.
- Its owner does not change in its own class. `Matasaburo.kt:78` and `MagFly.kt:129` reset `owner` only on `AbstractProjectile`, and `BulletBill` is not one.
- If a design treats a source with no faction as neutral, every shot owned by a hazard fails. `entities/hazards/` has 11 `OWNER pairTo this` lines. Inference: this may be the source of the claim. It is a default rule, not a `BulletBill` case.

So the split of **values** is not needed. No entity needs two different sides.

The split of **storage** is still useful. It does not depend on `BulletBill`:
1. **Lifetime.** A shot needs a side fixed at fire time, so that an old shot does not flip at the transition. `030` also wants a copy at spawn, because owners are pooled. An actor's side is live and can change.
2. **Actor test.** Readers such as `CartV2.kt:151`, `ToxicWaterfall.kt:137` and `CeilingCrusher.kt:160` ask "is this an actor?". Presence of `FactionComponent` answers this only if shots do not have it.

Recommended form: the actor side is in `FactionComponent`. The source side is a snapshot beside `owner`, in the owner part that `048` step 9 moves. Slot identity for friendly fire is a third, separate fact (`SessionRules.kt:13`, `Megaman.kt:435`).

#### 7. APIs and first callers

**Needed for ordinary bosses now**

| API | First real caller |
|---|---|
| `BossComponent` (encounter fields, hooks) | `AbstractBoss`, then the boss classes |
| Payload reads of `BossComponent` | `MegaLevelScreen.kt:717,764,793,802`, `Gate.kt:198` |
| Settable health depletion route | `AbstractBoss.definePointsComponent:217-226` |
| Capability data for damage, cull, drops | `AbstractBoss.kt:118,127-128,141,175` |
| `ignoresPeerContacts` | `MegaContactFilter.kt:106` |
| `FactionComponent`, read-only side (`030`) | The existing hostility readers |

**Only for the future scenario. Keep out of `048`.**

| API | First real caller |
|---|---|
| Encounter entity class | The scenario level |
| Health owner key in `BOSS_READY` | Encounter `onReady`, `MegaLevelScreen.kt:741` |
| Writable side and its `reset()` | Encounter step 4 |
| Hostile test in `isFriendlyFire`, `canBeDamagedBy` | Hostile `P2` |
| New `MegaLockReason` value | Encounter step 2 |
| Run-time route write on `P2` | Encounter step 2 |
| AI source and the launch option | AI `P2` |
| Hostility filter in targeting (`112`) | Enemies near hostile `P2` |
| `P2` boss damage values (`023`, `024`) | Hostile `P2` |

#### What I did not read in full

- `MegaLevelScreen.kt`: lines 170-229, 296-340, 500-979, 1090-1112 only.
- `GauntletMaster.kt`: lines 50-249 only.
- `MegamanWeaponsHandler`, `PlayerStatsHandler`, `PlayerSpawnEventHandler`, `EndLevelEventHandler`: grep and short ranges only.
- `084` `NOTES.md` and most of `029` `NOTES.md`: only "Targeting design" and greps. `019` `NOTES.md`: not read.
- `031` `NOTES.md`: only the section "Who drives an entity".
- Individual boss classes: only override counts and grep hits.
- Most `.asPlayer` lines: I did not classify each one.
- I did not run the game or a build. All statements come from reading the code.

#### Direct answer

**Neither.** `P2` has no `BossComponent` at `init()`, and it gets none at the transition. The `BossComponent` is on a separate encounter entity, added at that entity's own `init()`. The encounter is created at the transition and holds `P2` as a reference.

`P2` changes only role-neutral data: side, health route, locks and the death property. Nothing on `P2` is boss-only.

#### Recommendation

1. Use form (c) as an encounter entity, modeled on `GauntletMaster`, if the fight uses Mega Man's moves. Use (d) if the boss has its own state machine.
2. For `048`, build `BossComponent` for ordinary bosses at `init()`, with only encounter fields. Put capability data in the general parts. Add no API for this scenario.
3. In `030`, use one side per entity, with actor and source storage kept apart for the 2 reasons in section 6.
4. Keep `SessionRules` read-only. Hostility is a side test.

#### Risks

- **Soft lock:** `P2` leaves with no defeat (step 9).
- **Input leak:** an AI source that skips `frameBuilders` keeps pressing buttons after defeat (section 5).
- **Damage balance:** Mega's negotiator scores by size, so the fight numbers are not boss numbers. `damageNegotiator` is a `val`.
- **Hostile side effects:** items, gates and gravity (section 4).
- **Wrong targets:** `targetFor` can give `P2` itself, and enemies target hostile `P2`.
- **Boss intro checks:** `players.all { landed }` never passes for an absent player (`047` open question).
- **Slot-specific events:** `PLAYER_JUST_DIED` for slot 1 acts on any-slot listeners such as `GauntletMaster.kt:168`.
- **Live owner read:** until `030` copies the side at spawn, `rootPlayer` reads the owner live. Shots in flight flip at the transition.
- **Dead slot:** 74 `players.any` lines still count a dead slot 1 (`O2`, current state).

#### Open questions for the owner

1. **Does the boss fight with Mega Man's moves or its own moves?** Recommendation: Mega Man's moves, with form (c). This question decides between (c) and (d).
2. **What happens to `P2` at defeat?** Options: it is destroyed, it survives as an ally, or it is gone for the level. Recommendation: destroyed, as for an ordinary boss. A survival path adds restore code.
3. **Does a fall out of the level count as defeat?** Recommendation: yes, to prevent a soft lock.
4. **Is the trigger a boss room or a moment in a room?** Recommendation: the boss room path first.
5. **Should enemies treat hostile `P2` as a target, and can it collect items?** Recommendation: no to both. The first pass avoids enemies in the fight room.
6. **Does `P2` keep its health or refill?** Recommendation: refill, because the bar fill shows the maximum.
7. **Should a shot in flight at the transition keep its old side?** Recommendation: yes, through the snapshot.
8. **What does an AI session look like at launch?** Recommendation: leave it to the AI item. Do not add a `GameMode` row now.

##### Critical Files for Implementation
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/special/GauntletMaster.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/state/SessionRules.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt

## Agent report: Player and boss roles (2026-11-02)

Note: Replaced later: `heldBy` joins 2 facts and is wrong for a scene. See the next report in `082` `NOTES.md` and `ITEM.md` here. The `onHostGone` skip of the death event is replaced by `119`. The `DamagePolicy` design, the group measurements, the 8 faulty sites and the edge case table stand.

### Hostile `Megaman` P2: player to boss and back

All read-only. I changed no files. Base: `master` at `44a4c7e61`. A short `File.kt:line` is under `core/src/main/kotlin/com/megaman/maverick/game/`, or under `engine/src/main/kotlin/com/maverick/game/engine/` for engine files. "Inference" marks a claim that comes from reading code, not from a run.

#### 0. Corrections to the "already known" list

- `asPlayer`: 91 code lines in 66 files, plus 65 import lines (156 in total). The "about 155" count includes imports. 87 lines are live call sites. The other 4 are the definition, 2 lines in `rootPlayer` and a comment at `GapingFish.kt:110`.
- `players.any`: 37 live lines. The "about 74" count includes imports and comments.
- `isPlayer`: 6 call sites outside `asPlayer`.
- `targetPlayer`: 591 lines in 142 files. `O2-R5` says 616 and 147, so that number is old.
- `Megaman.canBeDamagedBy` is at `Megaman.kt:433-437`. The rule test is at `:435-436`.
- `SessionRules.isFriendlyFire` is confirmed at `SessionRules.kt:9-14`. `ProjectileRule` calls it at `ProjectileRules.kt:29`.

#### 1. What the sites really ask

##### Group measurements

| Group | Question | Sites | Does one flag on `isPlayer` give the right result? |
|---|---|---|---|
| A. Slot | Which slot is this? | 23 `playerSlot` lines, 7 `get(PlayerSlot(0))` getters, `get(slot)` at `MegaLevelScreen.kt:624,630`, 10 `settings` lines | The flag must not touch these. Slot identity stays. |
| B. Slot loop | Do this to each slot entity | 17 of 20 `players.forEach` lines: level flow 9, gravity 2, kill 3, light 3 | A filter gives the wrong result. See the list below. |
| C. Friend | Is this from the player's side? | 37 `owner.asPlayer` lines, 3 `rootPlayer` lines | Right for new shots. Wrong for shots in flight. Wrong at 1 site (`ChargedShot.kt:118`). |
| D. Target | Is this what enemies hit or seek? | 15 `asPlayer` lines (for example `GutsTank.kt:425`, `TellySaucer.kt:191`, `UFOBomb.kt:66`), 591 `targetPlayer` lines through `targetFor`, 25 scanner `any` lines (24 enemy files plus `AbstractEnemy.kt:304`), `players.all` 4 lines (boss landing checks) | Right |
| E. Collect or trigger | Can this collect or trigger? | `PlayerItemRule.kt:14`, 6 `isPlayer` sites (gate 2, cart 3, `UnderwaterFan.kt:221`), 11 `any` lines on devices, 5 `anyLive` lines | Right, if "hostile P2 does not press world devices" is what the owner wants |
| F. Player body | Does the world treat this as a player body? | 27 `asPlayer` lines, including 6 stomp lines | Right at most. Wrong at the sites in the next list. |
| G. World write | Does this change gravity or the camera? | 6 `asPlayer` lines (`BodyGravityChangeRule.kt:24`, `PortalHopper.kt:205`, `CapsuleTeleporter.kt:210`, `PipePortal.kt:125,148,155`) | Right at 5. Wrong at `BodyGravityChangeRule.kt:24`. |
| H. Self | "I am a player" | `Megaman.kt:456` | Crash |

The groups overlap only through the `any` and `anyLive` lines, which I split by folder. That split is my reading, not a tool result.

##### Result

One flag gives the right result for most of the 93 `asPlayer` and `isPlayer` sites. It does not give the right result for slots or for slot loops. So one flag is enough only if slot identity stays apart and `forEach` has two forms.

##### Sites where the flag alone is wrong

| Site | Fault |
|---|---|
| `Megaman.kt:456` | `asPlayer!!` crashes when P1 damages hostile P2. |
| `ReceiverRules.kt:109` | `asPlayer!!` crashes on a `BODY` to `PLAYER` contact. P2 keeps its `PLAYER` fixture (`BodyComponent.kt:77`). |
| `BodyGravityChangeRule.kt:24` | The `else` branch runs `entity.direction = direction`. The `Megaman` setter starts the camera rotation (`Megaman.kt:145-162`). So hostile P2 still rotates the camera for all players, and its own direction leaves the world gravity. |
| `FeetCartRule.kt:34` | `BEGIN` ran as a player, `END` runs as a non-player. `TOUCHING_CART` stays set. Inference: `MegaContactFilter.kt:116` drops the pair, then `WorldSystem.processContacts` fires `END`. |
| `ChargedShot.kt:118` | `owner.asPlayer ?: targetPlayer` gives a hostile P2 shot the direction and facing of P1. |
| `Megaman.kt:435-436` | P1's shots do no damage to hostile P2 in co-op. `damager.rootPlayer` is P1 and `friendlyFire` is false, so the method returns false, and `Bullet.kt:142` explodes the shot. |
| `CameraEdgeBlock.kt:59` | Inference: the edge block stops holding P2, so P2 can leave the view and fall. |
| `AbstractBlock.kt:44` | The feet gravity filter (`BodyComponent.kt:147-149`) stops counting 2 block classes for P2. |

##### If P2 stops being a "player", what breaks

| Area | With removal or `unregister` | With a read-only role and the slot kept |
|---|---|---|
| Input | `InputSource { inputs[slot] }` stays bound, because `MegaPlayers.kt:31` has no unbind. A human keeps control. `AirDashBehavior.kt:38,43` and `ControllerComponent.kt:115` crash on `playerSlot!!`. | Works. Input changes only if an AI drives P2. |
| Slot | `register` must run again. It adds a new component and rebinds input. | Stays |
| Respawn | `MegaLevelScreen.kt:552` skips the slot, so P2 does not respawn. | Works |
| `PLAYER_JUST_DIED` | `Megaman.kt:374` crashes on `playerSlot!!`. | Needs a branch. 5 listeners react (`MegaLevelScreen.kt:574`, `GauntletMaster.kt:168`, `PreciousGem.kt:215`, `PreciousGemCluster.kt:109`, `SpiderWeb.kt:138`). A hostile death must not reach them. |
| `targetFor` | P2 goes away from the loop. | Same result through the filter. `firstWithBody` must skip P2 too (`MegaPlayers.kt:75-78`). |
| HUD | No change. It reads slot 0 only (`PlayerStatsHandler.kt:58`). | No change. `BossHealthHandler.set` takes `IHealthEntity`, so it can read P2 directly. |
| Weapons | Stay on the entity. `Megaman.kt:364-365` crashes at the next spawn. | Work. Shots keep `owner = megaman`. |
| Shots in flight | They change side in the air, because `owner.asPlayer` is a live read. | Same fault, unless the routine destroys them. |
| `rootPlayer` | Gives null for hostile-owned shots. So `Megaman.kt:435` returns true for them. | Same. The layer must walk to the slot owner, not to the role. |

#### 2. Options

| | (a) Remove `PlayerComponent` | (b) `active` field with filtered views | (c) Role value, slot apart | (d) Capability parts |
|---|---|---|---|---|
| Slot, input, respawn | Break, see the table above | Work | Work | Work |
| `playerSlot!!` crash lines | 6 (`Megaman.kt:364,365,374`, `AirDashBehavior.kt:38,43`, `ControllerComponent.kt:115`) | 0 | 0 | 0 |
| Flips the `asPlayer` sites | Yes, at one write | Yes, at one write | Yes, at one write | No. Each group has its own write. |
| Can show "both"? | Yes. Removal and the boss link are 2 writes. | Yes, if "boss" is a second value | No, if one field holds both meanings | Yes. One missed field gives both. |
| Back direction | New component, `register` guards, rebind | One write | One write | 1 write per capability |
| Allocation | One component per turn | None | None | None |

(b) and (c) are the same thing if the field is the link to the fight. So I recommend one field, in the `(b)+(c)` form below.

(d) has a place later. `O8-R2` prefers capabilities over character checks. But each new field needs a production caller (`O1-R5`), and a single value already answers 5 groups. Named queries that read the one value are safe. Separate fields are not.

##### Per-frame recompute, one write, or read-through

The owner's idea has 4 parts:

- Registration stays manual, with no scan. Keep this.
- Use "inactive" in place of removal. Keep this. It saves slot, input and respawn.
- Views filter the entity. Keep this.
- A per-frame recompute. Do not use this. `Players` has no update step, and it does not need one. `forEach` and `any` already read `get(slot)` on each call (`IPlayers.kt:13-25`), and the filter can read the same field. There is nothing to rebuild.

A stored list that recomputes each frame gives a window of up to 1 frame. In the frame order, a write in `UpdatablesSystem` or in `events.run()` (`MegamanMaverickGame.kt:626`) comes after contacts. The recompute comes in the next `engine.update`. In that window:

- `asPlayer` says "not a player" and `players.any` says "player". This breaks "never both".
- `GateRule` can trigger a gate. That starts a room transition, and it cannot be undone.
- `BodyGravityChangeRule` can start a camera rotation.
- `targetFor` can give hostile P2, and an enemy fires at it.
- P1's shot passes through P2, and the hit is lost.
- A P2 death in the window sends `PLAYER_JUST_DIED`. That resets `GauntletMaster` and destroys webs.

The `048` owner decision of 2026-10-30 already rejects stored or per-frame values for the same reason: "a contact later in the same contact pass must see a hit at once".

A snapshot of both `asPlayer` and the views would remove the disagreement. It would still leave the late effects above. Read-through has none of these faults. The read cost is 1 array read per slot if `Players` caches the `PlayerComponent` reference.

#### 3. `BossComponent`

It does not need the same setup. The earlier finding stands.

- `is AbstractBoss`: 0 lines. `BossComponent`: 0 lines. `FactionComponent`: 0 lines.
- `getType()` against `BOSS`: 1 read (`MegaLevelScreen.kt:779`).
- The typed reads take the boss from an event, not from a search: `MegaLevelScreen.kt:717,764,793,802`. `BossSpawnEventHandler.kt:21` fetches by name.
- `MegaGameEntities.getOfType(EntityType.BOSS)`: my boss grep shows no read.

So "is this a boss?" has no reader, and a boss registry has no caller (`O1-R5`). P2 does not get `BossComponent`. The fight entity holds it, as in the earlier design. So there is no pair of components to keep in step.

One fact to keep in view: 16 lines ask `is AbstractEnemy` or use `AbstractEnemy::class`, and P2 is not an `AbstractEnemy`. Those lines are a different question. Only `Bullet.kt:141` matters for damage, and the layer in section 6 covers it. `MegaContactFilter.kt:106` drops enemy-to-enemy contacts by `getType()`, so enemy-to-P2 contacts stay on. The layer must decide those too.

Dependency: `AbstractBoss` is the event payload type at the 4 `MegaLevelScreen` reads. A fight entity that is not an `AbstractBoss` needs `048` step 6 or a small change at those reads.

#### 4. "One or the other, never both"

##### Enforcement

1. **One value.** `PlayerComponent` gets one field: `var heldBy: IGameEntity? = null`, `internal set`. Null means player. Non-null means the fight holds this slot. "Both" cannot be stored. An enum plus a fight reference is 2 values and can disagree. A sealed type works too, but has no gain here.
2. **One writer.** Only `Players.hold` and `Players.release` write it. `internal set` makes the compiler stop `core` from writing it.
3. **Meaning.** `isPlayer` reads `heldBy == null`. `playerSlot` stays raw. `Players.register` guard (`Players.kt:16`) must change to `hasComponent`, or a held entity can register a second time.
4. **Asserts.** `check` in `hold` (not held, entity is in the slot) and in `Megaman.onSpawn` (not held).
5. **No reset.** `PlayerComponent.reset()` must not clear `heldBy`. `destroyNow` runs component `reset()` before `onDestroy` (`GameEngine.kt:191-192`), and `onDestroy` must read the value.

##### Owner and routine

One owner: `MegaPlayers`. One routine file for both directions. Only the fight entity calls it.

**Player to boss: `turnHostile(slot, fight): Boolean`**

1. Refuse, with no write, if any test fails:
   - slot index is 0;
   - already held;
   - `existenceState != LIVE` (`isLive` is not enough, because `DESTROY_QUEUED` has `liveNow = true`);
   - not `inPlay`;
   - `levelState.roomTransition` is true;
   - the clock is paused;
   - the entity is teleporting.
2. Clear player-only state: stop charging, leave the cart, end the web lock.
3. Destroy the shots whose root owner is P2. This must come before step 4, because the walk then changes.
4. `hold(slot, fight)`. This is the one write. All sites see it at once.
5. If an AI drives P2, set `ControllerComponent.input`.
6. Return true. The fight entity then starts its boss flow.

**Boss to player: `restore(slot)`** (idempotent)

1. Return if not held.
2. Destroy the shots whose root owner is P2, before the write, for the same reason.
3. `release(slot)`. This is the one write.
4. If an AI drove P2, restore `InputSource { inputs[slot] }`.
5. Leave a dead P2 in its slot. It respawns at the next `PLAYER_SPAWN_INIT`.

The fight entity calls `restore` at each exit: won, aborted, its own `onDestroy`.

##### Death route

`Megaman.onDestroy` reads `heldBy` first. If null, it submits `PLAYER_JUST_DIED` as today. If not null, it calls the fight directly (`onHostGone`), then the fight calls `restore`. Use a direct call. Events run per listener and then per key, not in send order (`EventsManager.run`), so order across 2 keys is not safe.

##### Edge cases

| Case | What happens | Rule |
|---|---|---|
| Destroy during the turn | `destroy()` only queues (`GameEngine.kt:152-157`). The routine has no yield point. | The `LIVE` test refuses a queued P2. If P2 dies after the write, `onDestroy` routes to the fight. |
| Turn and kill in the same update | The spawn loop runs before the kill loop (`GameEngine.kt:219-227`). So `fight.onSpawn` can see a `DESTROY_QUEUED` P2. | Refused. The fight waits or ends. Call `turnHostile` from the fight's update, not from `onSpawn`, so it can try again. |
| P2 spawns in the same frame | P2 is `PRE_PLAY` until the beam ends (`PlayerSpawnEventHandler.kt:126,210`). | Refused by the `inPlay` test. |
| Destroy and respawn in one frame | `destroyNow` runs before the respawn loop. | The route and `restore` run first, so the spawn assert holds. |
| Fight entity destroyed first | Its `onDestroy` calls `restore`. Pooled fights make a stale `heldBy` a real risk. | Always `restore` in `onDestroy`. |
| P1 dies | `MegaLevelScreen.kt:586` destroys P2. The destroy is queued, so listener order does no harm. | The fight must `restore` on `PLAYER_JUST_DIED`, not on `PLAYER_SPAWN`. `AbstractBoss.kt:174` uses `PLAYER_SPAWN`. That is too late here, because `P2.onSpawn` runs in the same update as `P1.onSpawn`, and `P1.onSpawn` only then submits `PLAYER_SPAWN`. |
| P1 and P2 die in one frame | `MegaLevelScreen.kt:758,787` ignore defeat if P1 is dead. | A loss wins over a win. |
| Room change | `Megaman.kt:391-405` moves each `Megaman` with no role test. `LevelRoomHandler.kt:72-76` destroys enemies and non-player shots. A fight entity survives unless it listens. | The fight listens to `BEGIN_ROOM_TRANS`, as `GauntletMaster` does, and restores. Or the fight room stays locked. |
| `Megaman.kt:486-490` fall | A direct `destroy()`. Health does not change, so a health listener such as the one at `AbstractBoss.kt:194` never fires. | The defeat must come from the death route, not from health. |
| Pause | Pause turns off systems except sprites (`LevelStateHandler.pause`). The fight's update stops. `engine.update` queues and `events.run` still run. | A listener can call `restore` in a pause, and this is safe. `turnHostile` refuses in a pause. |
| Forced resume | `MegaLevelScreen.kt:595,902` resume on P1 death. | No extra rule. |
| Event order | An event sent during `events.run` arrives on the next run. | Writes are direct calls. Events only report. |
| Write in a contact pass | `BEGIN` and `END` pairs would see 2 roles. | Never call the routine from a contact rule. |
| `runQueue` | It pops from the end (`MegamanMaverickGame.kt:516-517`), so the order is reversed. | Do not use it for the routine. |
| `BOSS_DEFEATED` locks | `MegaLevelScreen.kt:774,800`. Lock reasons are a set, so a spare unlock does no harm. | Keep the raw loop. |

#### 5. "Anything that kills the playable character also kills the boss or enemy"

| Kill path | Reads role? | Holds? |
|---|---|---|
| Health to 0 (`AbstractHealthEntity`) | No | Yes, for hazards. See the conflict below for enemy damage. |
| `DeathRule` on a `DEATH` fixture | No | Yes, if the routine does not copy `AbstractBoss.kt:125,148`. Those lines make a boss immune. Do not copy them to P2. |
| `CeilingCrusher` | The trigger reads `asPlayer` (`:160`), the kill does not | Yes. P2 does not start the crush. The fixture still kills it. |
| `PlayerDeath.kt:46` | `players.forEach` | Only with the raw loop |
| `GravityBlock.kt:214`, `CameraEdgeBlock.kt:42` | `players.forEach` | Only with the raw loop |
| `Megaman.kt:487-490` fall | No | Yes |
| Level reset (`MegaLevelScreen.kt:586`) | `players.forEach` | Only with the raw loop |
| Camera edge kill (`MegaLevelScreen.kt:333`) | Slot 0 only | Not a change. P2 has no such kill today. |

So the rule holds if 4 specific lines use the raw loop. If they take the filtered default, 3 kills go silent with no crash. A test must cover these.

**Conflict in the rule.** Enemy shots kill a playable character. If hostile P2 is an ally of enemies, then enemy shots do not hurt it, and a literal reading of the rule fails. I read the rule as "world kills": pit, crusher, death fixture, hazard, level reset. The owner must confirm (question 3).

#### 6. The damage layer

**Name:** `DamagePolicy`, with `enum class Relation { SELF, ALLY, FOE, NONE }`. No such names exist today. `IDamageNegotiator` stays as "how much". This layer says "may it".

**What it asks:** `relation(source, target): Relation`.

**Who owns it:** `MegaServices`, built at `MegamanMaverickGame.kt:438` from `launch.mode.rules`. It holds `SessionRules`. `SessionRules` stays a read-only `val`, and hostile state never goes in it.

**How it decides:**

| Source root \ target root | Player | Held P2 | Other |
|---|---|---|---|
| Same entity | `SELF` | `SELF` | `NONE` |
| Other player | **`SessionRules`**: `FOE` or `ALLY` | `FOE` | `NONE` |
| Held P2 | `FOE` | `ALLY` or `FOE` per mode | `ALLY` for the enemy side, else `NONE` |
| Enemy side | `NONE` | `ALLY` (see question 3) | `NONE` |
| Hazard or no owner | `NONE` | `NONE` | `NONE` |

`SessionRules` answers exactly one cell. `NONE` means "no opinion", so all other logic stays as it is, for example `ReactorManProjectile`.

**Call sites:**

| Site | Today | With the layer |
|---|---|---|
| `ProjectileRules.kt:29` | `isFriendlyFire` | `relation == ALLY` then return |
| `DamageRule.kt:27,45` | none | Refuse on `ALLY` or `SELF`, after the `HIT_BY_DAMAGEABLE` receiver |
| `Megaman.kt:435-436` | owner walk | Removed |
| `PreciousGem.kt:208` | `isFriendlyFire` | Removed |
| `SessionRules.isFriendlyFire` | 2 callers | Moves into the policy |

The rule sites give the choke points. The `canBeDamagedBy` callers outside `DamageRule` are `Bullet.kt:142` and `ChargedShot.kt:147`, and they run after the `ProjectileRule` return. A smaller form keeps the 3 current sites and changes each to `relation`, with no `DamageRule` change. It needs only those 3 sites, but it leaves 2 entity-level tests. Both forms keep today's co-op and PvP results by my reading (inference).

**Root walk:** `rootPlayer` reads `asPlayer`, so it goes null for held P2. The policy needs a raw walk to the slot owner. It is limited to 2 levels, as today.

**`FactionComponent` (`030`):** it does not exist yet. The policy has one internal read, `factionOf(entity)`.

- Before `030`: it derives from the slot owner and from `is AbstractEnemy`.
- After `030`: it reads the component, and shots read the copy from their spawn.
- For a slot entity, the faction derives from `heldBy`. Do not write it as a second value, or "both" comes back (inference).
- PvP teams stay in the policy's slot comparison until `109` decides.
- `030` then lets a shot in flight keep its side, so step 3 of the routine can become optional.

#### 7. New APIs and their first callers

| API | First caller | Production caller today? |
|---|---|---|
| `PlayerComponent.heldBy` | `isPlayer` | Reader yes. No writer until the fight entity. |
| `Players.hold`, `release` | `turnHostile`, `restore` | **No** |
| `MegaPlayers.turnHostile`, `restore` | Fight entity | **No.** No fight entity exists. |
| `IPlayers.forEachSlot` | The 17 raw sites | Yes, but no result changes until a hold exists |
| Filter in `forEach`, `any`, `all`, `isLive` | The other sites | Yes |
| `Players` cached component array | The filter | Yes |
| `DamagePolicy`, `Relation` | `ProjectileRules.kt:29` | Yes |
| Raw slot owner walk | `DamagePolicy` | Yes |
| Shot cleanup by root owner | `turnHostile`, `restore` | **No** |
| Fight `onHostGone` | `Megaman.onDestroy` | **No** |
| AI `InputSource` | Only if an AI drives P2 | **No** |

Under `O1-R5`, the marked APIs can only go in the branch that adds the fight entity. A test alone is not a caller. A `Test*` entity in a test map is the nearest fit, but the rule does not clearly accept it.

Order:

1. `DamagePolicy` with the existing behavior.
2. `forEachSlot` at the 17 sites.
3. The fixes at the "flag is wrong" sites.
4. The role field, the routine and the fight entity together.

Dependencies: `047c` for per-slot respawn, `047e` and `112` for boss targeting, `048` step 6 for the `AbstractBoss` payload. `030` is optional.

#### 8. What I did not read in full

- `029` `NOTES.md`: I read `:1150-1324` only. It has 1798 lines.
- `084` and `061` `NOTES.md`: not read. I read their `ITEM.md`.
- The objective files: only `README.md` and the `O2-R5`, `O2-R9`, `O8-R2` rules.
- The earlier fight-entity design report: I had only your summary.
- `MegaContactFilter.kt`, `GauntletMaster.kt`, `MegamanWeaponsHandler.kt`, `PlayerStatsHandler.kt`, `Gate.kt`, `WorldSystem.kt`: parts only.
- The group split of `asPlayer` is by reading each grep line. I opened about 30 sites. The others I classified from the line text.
- No build or run. All behavior claims come from reading code.

#### Direct answer

No, one active flag does not give the right result for all groups. It is right for player-or-not questions and wrong for slot questions and slot loops. A per-frame recompute is not needed, and it would break "never both" for up to 1 frame. One write at the transition, with reads that go through to that value, has no delay. `BossComponent` does not need the same setup, because no code asks "is this a boss?".

#### Recommendation

- Keep `PlayerComponent(slot)` on P2 for the full fight.
- Add the single field `heldBy`, written only by `Players.hold` and `release`.
- `isPlayer` reads that field. `playerSlot` and `get(slot)` stay raw.
- `forEach`, `any`, `all` filter by default. Add `forEachSlot` for the 17 sites.
- One routine in `MegaPlayers` for both directions, called only by the fight entity.
- `DamagePolicy` decides relations and asks `SessionRules` for one cell only.
- Fix the 8 listed sites before the first hold exists.

#### Risks

- **Silent kill loss.** A raw site left on the filtered default loses a kill with no crash. This affects `PlayerDeath.kt:46`, `GravityBlock.kt:214`, `CameraEdgeBlock.kt:42`, `MegaLevelScreen.kt:586`.
- **Default change.** About 40 sites change meaning at the first hold, with no edit at those sites. Each is right by my reading, but only for a hostile P2.
- **Pooled fight entity.** A destroyed fight that does not call `restore` leaves a stale `heldBy`. The fix is one line in `onDestroy` and the spawn assert.
- **Hot path.** `DamageRule` runs on `BEGIN` and `CONTINUE`. The added check needs a before and after measure (`O5-R2`). An early exit on "neither side has a slot" limits it. The size of the cost is not known.
- **Player-only state at the turn.** Step 2 lists charge, cart and web. Other states may exist. I did not read all behaviors.
- **`Megaman.direction` setter.** It remains a camera write from an entity (`Megaman.kt:138-152`). The guard at `BodyGravityChangeRule.kt:24` treats the symptom. `058` owns the cause.
- **Unbuilt callers.** Most new APIs have no production caller until the fight entity exists.

#### Open questions for the owner

1. **Who drives hostile P2: the human in that slot, or an AI?** Recommendation: the human, for the first pass. It needs no input write and no AI source. An AI source has no caller today.
2. **Can slot 0 turn hostile?** Recommendation: no. The 7 screen getters, the camera and the death flow all assume slot 0. `turnHostile` refuses it.
3. **Do enemy shots hurt hostile P2?** This decides the "Enemy side" cell and the reading of the rule in section 5. Recommendation: no. The rule covers world kills only.
4. **Does hostile P2 press world devices (`Lift`, `FloorButton`, `PushableBlock`, stomp)?** Recommendation: no, as the filtered default gives. "Stops acting as a player" then has one meaning.
5. **What happens to shots in flight at the turn?** Recommendation: destroy them. After `030`, the owner can choose to let them keep their spawn side.
6. **Does a room change end the fight, or is the room locked?** Recommendation: keep the room locked by content, and restore on `BEGIN_ROOM_TRANS` as a safety path.
7. **Does the gate at `DamageRule` go in, or do the 3 sites stay?** Recommendation: the gate in `DamageRule`. It removes 2 entity-level tests and covers explosions.
8. **After a P2 death as a boss: does P2 wait for the next reset, or respawn at once?** Recommendation: wait for the next reset until `047c` exists.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/players/PlayerComponent.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/players/Players.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/state/SessionRules.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules/ProjectileRules.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules/DamageRule.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt

## Agent report: Virus recovery: same instance or respawn (2026-11-02)

Note: Replaced later: `BossRoleComponent` is replaced by the capture value in `ITEM.md`. The `PLAYER_JUST_DIED` risk at entry and at defeat goes away with `119`, because the fight filters on its own P2 and owned entities filter on their owner.

### Virus P2: same instance or die and respawn

I read and grepped only. I changed no file and ran no build. Line numbers are from `master` at `44a4c7e61`.

**Short answer:** use die and respawn. Let P2 die through the normal death path, so that one signal covers all deaths. A stand-in on the fight entity plays the defeat. `spawn` restores P2 at the end.

Unmarked claims come from code that I read. "(inference)" marks my reasoning.

#### 0. Corrections to the brief

- **`PLAYER_JUST_DIED`:** the screen ignores slot 1 (`MegaLevelScreen.kt:583-584`), but the event is not free. `GauntletMaster.kt:168`, `PreciousGem.kt:215`, `PreciousGemCluster.kt:109` and `SpiderWeb.kt:138` react to any slot.
- **Stacking:** `Megaman.kt:390-395` is the start of the room transition handler. The stacking write is `body.setCenter(position)` at `:396`.
- **Conflict in the earlier design:** "no death from death fixtures" conflicts with the owner's rule that anything that kills a player also kills it as a boss.
- **Stats handler:** only `PlayerStatsHandler` exists. `MegamanStatsHandler` does not exist.

#### 1. The two paths

##### The real difference

`Megaman` is not pooled, so both paths use the same object. The real differences are these:

| Point | (a) in place | (b) destroy and spawn |
|---|---|---|
| Object | same | same |
| `destroyNow` and `spawnNow` | do not run | run |
| Component `reset()` (13 of 27 components have a real reset) | P2 needs a new call for each state that it can change | runs automatically |
| `onSpawn` (25 writes in `Megaman.kt:320-360`, 4 in `AbstractHealthEntity.kt:63-68`) | needs a second copy or an extraction | runs automatically |
| `PLAYER_JUST_DIED` and `runnablesOnDestroy` | do not fire | fire |
| `PLAYER_SPAWN` | not sent | not sent: slot 1 sends none (`Megaman.kt:364`) |

**Main finding (inference):** path (b) is not optional. P2 goes through it in two cases that path (a) cannot avoid:

- `P1` dies: `MegaLevelScreen.kt:586` destroys P2, and `:550-553` spawns it.
- P2 falls out of the level: `Megaman.kt:487-489` calls `destroy()` directly, so health stays above 0 and a health route never runs.

So all role data must already survive `reset()` plus `onSpawn`. Path (a) adds a second restore path. It does not replace the first.

##### Restore table

| State | (a) who undoes it | (b) do stock `reset()` and `onSpawn` restore it? |
|---|---|---|
| Health | new `setHealth` call | yes, `Megaman.kt:335` |
| `wasHealthDepleted` | **new API needed.** The field is private (`AbstractHealthEntity.kt:51`), and only `onSpawn` clears it (`:68`). | yes |
| Side (hostile) | scoped list | **no** |
| Damage lock | `unlock` | yes, `LocksComponent.kt:39`, `Megaman.kt:342` |
| `BOSS_DEFEATED` lock | `:800` runs only on `INTERMEDIATE_BOSS_DEAD` | yes |
| Depletion route | scoped list | **no**, and not needed in the recommended design |
| `ENTITY_KILLED_BY_DEATH_FIXTURE` | scoped list | **no**. Nothing clears the property. Remove the write under the owner's rule. |
| AI input source | new rebind | **no.** `ControllerComponent` has no `reset()`, and `input` is set only at `MegaPlayers.kt:31`. |
| Weapons, ammo | new calls | yes, `Megaman.kt:337-338`. Pre-virus ammo and the selected weapon are lost. |
| Charge | `stopCharging()`. The attack actuator has no release-continued action, so an AI hold can leave a stale charge. | yes, `WeaponChargeComponent` reset |
| Cooldowns | no stock call | **no.** `MegamanWeaponsHandler.reset()` (`:156`) has no production caller. Cooldowns run out while the weapons handler updates, so the stale time ends by itself (inference). |
| Behaviors | public `BehaviorsComponent.reset()` | yes, with the `087` order risk; `Megaman.kt:357` covers momentum |
| Stun, freeze, recovery, run, momentum, carry, teleporter | one call for each | yes |
| Sprite tint | see the note | see the note |
| Animation | supplier reads state each frame | `Animator.reset()` clears the key |
| Position | nothing to do | **must pass `BOUNDS`.** `Megaman.kt:320` reads it with `!!`. |
| Velocity, gravity, collision, fixtures | undo each write | yes, `:323-326` |
| Target | no stored state today (`MegaPlayers.kt:39`; `112` is planned) | same |
| `PlayPhase` | explicit write | **no.** `PlayPhaseComponent.reset()` is empty. P2 stays `IN_PLAY` if nothing else writes it. |

**Tint note:** `SpritesComponent.kt:135` writes the slot 1 tint each frame. A virus tint must read scoped state in that function. Nothing clears a stored colour, so a virus tint on slot 0 would stay. Alpha and `hidden` are also written each frame at `:136-137`.

##### Can the writes be one scoped list, undone by one call?

Yes. Use one component on P2 with a `reset()` that clears the role data. `destroyNow` then undoes it on every route: kill, fall, `P1` death and `engine.reset()`.

Two limits apply:

- `reset()` runs before `onDestroy`. A `runnablesOnDestroy` watcher therefore cannot read the component. The fight entity must keep its own state.
- The watcher must not remove itself inside the runnable, because that changes the map during iteration (`GauntletMaster.kt:73-75`).

##### What (b) does to the named items

- **Lives:** no change. `removeLife` runs only for slot 0 (`MegaLevelScreen.kt:584,591`).
- **`PLAYER_JUST_DIED`:** P1's `PreciousGuard` is destroyed. Webs are destroyed. A live `GauntletMaster` resets. `Megaman.kt:376` also stops the shared charge sound, which can cut P1's sound.
- **Slot 1 respawn:** no mid-level path exists. The only spawn is in `PLAYER_SPAWN_INIT`. The planned `047c` branch covers this.
- **Spawn point:** `slotSpawnProps` uses the checkpoint, which can be in another room. So the fight must pass its own `BOUNDS`.
- **Required props:**
  - `FACING` defaults to right.
  - `DIRECTION` defaults to up, and `Megaman.kt:329` writes it to all players (`WorldGravity.kt:14`). A missing value can turn P1 and the camera.
  - The gravity and movement scalars default to 1 (`:345-347`).
- **Stacking:** a dead P2 is not a listener (`:373`), so `:396` does not move it. A live P2 in a fight is moved. A boss room with a closed `Gate` prevents this (inference).
- **Frame gap:**
  - `destroy()` and `spawn()` in one call give 0 system passes. The state stays live (`GameEngine.kt:90-95,224-238`), and `EntityExistenceStateTest.kt:271-287` covers this.
  - `spawn()` from inside `runnablesOnDestroy` gives 1 pass without P2 (`GameEngine.kt:111-115,195`).
- **Timers:** `Megaman.kt:351-352` starts `shootAnimTimer` (0.3 s) and `wallJumpTimer` (0.1 s). The beam hides this today. A P2 respawn with no beam shows a shoot pose for 0.3 s.
- **Locks:** `LocksComponent.reset()` does not call the hook. A `BEHAVIORS` lock held at destroy leaves `allowedBehaviors` false, and `Megaman.onSpawn` does not clear that group.
- **Listener order:** P2 moves to the end of the event listeners (inference: no effect).

#### 2. Defeat presentation

Three options exist:

| Option | Defeat plays on | Exposure |
|---|---|---|
| A: in place | live P2 at 0 health | P2 can be crushed, can fall, and is a target. A stale depletion flag is possible. |
| B1: destroy at the end | live P2, like `AbstractBoss.kt:213-217` | same exposure, but the restore is stock code |
| **B2: destroy at the kill** | a stand-in sprite on the fight entity | none: P2 is out of the world |

B2 fits best:

- P2 leaves the world at the kill, so nothing can hit it during the defeat.
- Flash, explosions, collapse and recovery are all on the fight entity. `spawnDefeatExplosion` is existing code (`AbstractBoss.kt:293`).
- `Megaman.kt:383` already spawns the orbs on a normal death.
- The precedent is the beam: `PlayerSpawnEventHandler` draws an effect while the player sprite is hidden (`SpritesComponent` `shouldHideSprite`).
- `PlayerSpawnEventHandler` itself cannot be reused, because it locks all players and starts the level reset (`:126-134`).

The limit of B2: the stand-in does not use P2's animator. A collapse that needs P2's real physics fits B1 better.

#### 3. Scenarios

| Case | (a) | (b) with B2 |
|---|---|---|
| `P1` dies in the same frame | The undo list and the level reset can overlap. | The watcher sees slot 0 not `liveAfterUpdate` and ends the fight. `:759,:788` drop the boss events. The engine also handles a queued respawn (`GameEngine.kt:139-146`). |
| Fall during defeat | P2 is destroyed at 0 health, so (a) needs the (b) path. | Not possible: P2 is already gone. A fall during the fight is the same kill signal. |
| Crusher in the intro | `CrusherComponent.kt:31` ignores `invincible`, so the health route must handle the intro state. | Normal death, same signal. One guard is needed: `AbstractBoss.kt:195-201` still runs `isReady` while defeated, so `isReady` must return false after a defeat. |
| Room exit | A cull of the fight entity leaves P2 with role data, so the undo must run from there. | Same risk. `AbstractBoss` does not turn off `CULL_EVENTS`, so `BEGIN_ROOM_TRANS` culls it (`AbstractEnemy.kt:238-240`). Spawn the fight with `CULL_EVENTS` false, in a room with a closed `Gate`. |
| Pause | No difference. | No difference. Pause reads only slot 0. Non-mini defeat blocks pause through `inputLocked` (`MegaLevelScreen.kt:769`, `MegamanMaverickGame.kt:229,691`). Systems stop except sprites, and queues still drain (`:930`). |
| Fight repeated after `P1` death | Depends on the undo running on the failure route. | `:586` destroys P2, `reset()` clears the role, `PLAYER_SPAWN` destroys the fight (`AbstractBoss.kt:174`), and `:552` spawns P2. |
| Health at restore | Any value, with a stale flag risk. | Max health (`:335`): `START_HEALTH` 14 plus heart tanks. A lower value is one `setHealth` call after the spawn. |

#### 4. Restore code and stale values

- **Least restore code:** (b). It reuses 13 resets and 29 `onSpawn` writes.
- **Most stale risk:** (a). The worst case: `wasHealthDepleted` stays true after `setHealth(max)`, so `AbstractHealthEntity.kt:125` never fires again. P2 then lives at 0 health and cannot die by health.
- **Stale risks in (b):** the input source, side, `PlayPhase`, stored tint, the `BEHAVIORS` table and wrong spawn prop defaults. The scoped component and one props helper cover these, except the `BEHAVIORS` table. For that table, do not use a `BEHAVIORS` lock on P2.
- **Generic snapshot:** does not fit.
  - `reset()` and `onSpawn` already define the known state.
  - Each role write has a known default, so no stored memory is needed.
  - A snapshot cannot reach private fields such as `wasHealthDepleted` or a behavior's running state.
  - A snapshot can capture a state in the middle of an event. `037` rejects this for the same reason (`037` `NOTES.md`, section 3b).

#### 5. Effect on the earlier design

**Keep:** the separate fight entity, P2 as a reference, the intro damage lock, the hostile side at ready, and the watcher.

**Change:**

1. Remove the health route. Normal death reaches the watcher, so damage, crusher, death fixture and fall all give one signal. `Megaman.onHealthDepleted` needs no edit, and the `wasHealthDepleted` risk goes away.
2. Remove the `ENTITY_KILLED_BY_DEATH_FIXTURE` write.
3. Add a slot 0 check in the watcher.
4. Put the role data in one scoped component.
5. Use the same gate at entry (proposal, inference): `destroy()` and `spawn()` at the takeover. This ends a mid-jump, a charge, a stun, a teleport and a web in one step, and `onSpawn` gives full health. So the "full health" write goes too. Entry also fires `PLAYER_JUST_DIED`.

**"No edge cases":** this design removes most cases, but it does not reach zero. These guards remain:

- the slot 0 check
- the `isReady` guard
- one state check, so that the entry destroy does not fire the watcher
- one `end()` function for a fight that ends with P2 alive
- the required `CULL_EVENTS` value and closed room as level rules

#### New API

No item below has a production caller today, because the fight entity does not exist. The first caller of each is that fight entity. Names are working names.

| API | First caller | Note |
|---|---|---|
| `BossRoleComponent` with `reset()` | fight entity | holds side and the input flag |
| `MegaLockReason` entry for the intro | fight entity | 7 of 32 reasons are in use |
| Slot input bind, extracted from `MegaPlayers.kt:31` | `register` (production today), then the component `reset()` | only if an AI source is used |
| Respawn props helper (bounds, facing, direction, scalars) | fight entity | `047c` is a second caller |
| AI `InputSource` and its update step (`031` `NOTES.md`) | fight entity | needed in both paths |
| Read of the side in `isFriendlyFire` and `canBeDamagedBy` | P1 shots at P2 | edits, not new types |
| Path (a) only: `clearHealthDepleted()`, `restoreInPlace()`, depletion guard | fight entity | not needed for (b) |

**Dependencies:** `047c` (slot 1 respawn), `030` (side), `112` (targeting), `087` (reset order), `048` (`BossComponent`). The screen reads the boss as `AbstractBoss` at `:717,:764,:793,:802` and in `Gate.kt:198`, so today the fight entity must extend it.

#### Not read in full

- `029` `NOTES.md` (grep only), `084` `NOTES.md`, and the `038` and `087` `NOTES.md`.
- Branch docs.
- `MegamanWeaponsHandler`, the `Megaman` body and behavior components, `Gate`, `WorldSystem`, `AbstractEnemy`, `LevelRoomHandler`.
- I did not check every entity that holds a P2 reference, for example carts and portals. A P2 death already takes this path today, so this risk is not new.

#### Direct answer

Yes, it is feasible. "Die and respawn" and "same instance" are the same object here. The real choice is whether P2 goes through the engine's destroy and spawn passes. Choose yes. Path (a) still needs those passes for a fall and for `P1` death, so it only adds a second restore.

#### Recommendation

Use option B2:

1. Any P2 death reaches the watcher.
2. The watcher checks slot 0.
3. The fight runs the normal boss defeat with a stand-in.
4. At `BOSS_DEAD`, the fight spawns P2 at one fixed anchor with full health.

Use the same `destroy()` and `spawn()` gate at entry.

#### Risks

1. `PLAYER_JUST_DIED` fires at entry and at defeat, and it removes P1's `PreciousGuard`. Filter the listeners by slot. `047` already asks this question, and `SpiderWeb.kt:136` has a `TODO`. A skip flag on the scoped component does not work, because `reset()` runs first.
2. A respawn at the kill position can kill P2 again on the same hazard. A fixed anchor prevents this.
3. A missing `DIRECTION` turns the world. The props helper must always set it.
4. The shoot pose shows for 0.3 s after the spawn. Fix `:351-352`, or hold P2 hidden for that time.
5. `LocksComponent.reset()` skips the hook. This is a separate bug candidate.
6. A P2 death with no fight has no respawn until `047c` exists.
7. Audio at the destroy can cut P1's charge sound (`Megaman.kt:376`).

#### Open questions for the owner

1. **Restore place:** fixed anchor in the fight room, near P1, or the kill position? I recommend the fixed anchor.
2. **Restore health:** full or a lower value? I recommend full. It needs no new code.
3. **Weapon state:** accept full ammo and `MEGA_BUSTER` after the restore? I recommend yes, because no snapshot is then needed.
4. **Presentation:** does the collapse need P2's real physics and animations? I recommend no, so that B2 stays valid. If yes, use B1.
5. **Death by a level hazard:** does a fall play the full defeat and count as a boss win? Your rule implies yes, and I recommend yes.
6. **Entry gate:** use `destroy()` and `spawn()` at the takeover too? I recommend yes, with the listener slot filter first.
7. **Order:** must `047c` merge first? I recommend yes, so that the fight is not the first owner of slot 1 respawn.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/GameEngine.kt
