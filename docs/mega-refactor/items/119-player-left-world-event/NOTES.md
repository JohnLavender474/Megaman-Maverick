# Notes: 119 `PLAYER_DESTROYED` and `LEVEL_FAILED`

Research for this item. Each report is kept in full, with its own date and `master` commit. A note above each report says what later research or an owner decision replaced. If a report and `ITEM.md` disagree, `ITEM.md` wins.

## Agent report: `PLAYER_JUST_DIED` (2026-11-02)

Note: The report drops `heldBy` and `onHostGone`. `heldBy` is replaced by the capture value in `120`, which has other readers. `onHostGone` stays dropped.

### `PLAYER_JUST_DIED`: design research

No files changed. Base: `master` at `b3e792bc4`. Short names:
- `Megaman.kt` is `core/.../entities/megaman/Megaman.kt`.
- `Screen` is `core/.../screens/levels/MegaLevelScreen.kt`.
- `Game` is `core/.../MegamanMaverickGame.kt`.

#### Facts that change the question

- `heldBy` and `onHostGone` are not in `core` or `engine` main code. `git log --all -S` finds 0 commits for `heldBy` there.
- `PlayerComponent` has one field, `slot`, and an `internal` constructor (`PlayerComponent.kt:6`).
- `MegamanClone` does not exist on `master`: 0 lines and 0 commits. The fight entity also does not exist. No item in `ITEMS.md` names a hostile P2.
- The screen already drops a slot 1 event (`Screen:583-584`). So a held P2 death reaches no life loss and no death flow, with no skip.

#### 1. When the event fires

Code path: `destroyNow` calls `reset()` on the components (`GameEngine.kt:191`), then `onDestroy` (`:192`). `Megaman.kt:374` submits with no condition.

| Route | Site | Real death | Event | Receivers |
|---|---|---|---|---|
| Health reaches 0 | `AbstractHealthEntity.kt:91,125`, `:115` | yes | yes | all 5 kinds |
| Death fixture | `DeathRule.kt:28` (also `PlayerDeath.kt:47`, `CrusherComponent.kt:31`, `Screen:333`) | yes | yes | all 5 kinds |
| Fall | `Megaman.kt:489`, direct `destroy()` | yes | yes | all 5 kinds |
| Other slots at level reset | `Screen:586` | no | yes, 1 frame later | screen drops it; 4 entity kinds act |
| Level end | `Screen:823-832`, then `Screen:1085` | no | yes | 0 |
| `engine.reset()` or screen change | `Game:1020`, `Screen:1085` | no | yes | 0 |
| `engine.dispose()` | `Game:773` | no | yes | not delivered (inference) |

Measured results:
- **Owner's 7 routes:** 3 are deaths and 4 are not. The event fires on all 7.
- **Wrong word with an effect:** only route `:586`. With 2 live players, 1 slot 0 death gives 2 events, and the second one reports no death.
- **Why level exit has 0 receivers:** the screen leaves at `Screen:1077`, before `engine.reset()`. Entities leave in their own `onDestroy`.
- **With 1 player:** every delivered event is a real death. The name fails only with 2 players, or if a listener is added later.
- **Real death with no event:** I found none. The proposal would create the first one.
- **Fall gap:** a fall skips `onHealthDepleted`, so it has no orb ring and no Mario sound. `onHealthDepleted` is therefore not a complete death hook.

Frame trace for a slot 0 death:
- Frame N: `destroy()` is queued.
- Frame N+1, start: `destroyNow` submits the event.
- Frame N+1, end: `events.run()` delivers it (`Game:626`). `Screen:586` queues the slot 1 destroy.
- Frame N+2, start: slot 1 `destroyNow` runs.
- Frame N+2, end: the second event arrives.

#### 2. What each listener wants

| Listener | Real need | Reads slot | Wrong for slot 1 today |
|---|---|---|---|
| `Screen:574` | A death that the session rule counts | yes, `:583-584` | no; it matches the `047` decision |
| `GauntletMaster.kt:168` | The level will reset (see its comment at `:191`) | no | **yes**. It sets `INACTIVE`, keeps live enemies and loses its child list. It starts again only on a room event (`:173-180`). A gauntlet `Gate` opens only on `GAUNTLET_CLEARED` (`Gate.kt:212`). Inference: the room stays locked until slot 0 dies. |
| `PreciousGem.kt:215` | My owner left | no | **yes**: P2's death destroys P1's gems |
| `PreciousGemCluster.kt:109` | My owner left | no | **yes**, same. This file has 0 cull lines. `PreciousGem` has a cull time that I did not trace. |
| `SpiderWeb.kt:138` | The stuck player left, or the level reset | no | **yes**: the `TODO` at `:136` is correct. But `SpiderWeb` has no production spawner: 0 TMX files, and only a factory entry in Kotlin. |

The `return` at `Screen:584` is safe. It exits one `onEvent` call, and no code follows the `when`.

Similar events:
- `PLAYER_SPAWN` is slot 0 only. It acts as a level reset (`TODO` at `Megaman.kt:362`, accepted gap in `O2-R9`). It has 37 lines in 25 files.
- `PLAYER_READY` and `PLAYER_DONE_DYIN` come from handlers fixed on slot 0 (`PlayerSpawnEventHandler.kt:64`, `PlayerDeathEventHandler.kt:45`).
- So these events are level facts with player names. I grepped their listeners and read only the `Screen` handlers.

#### 3. Options

| Criterion | (a) Skip for held | (b) Always fire, with facts | (c) Separate keys | (d) Hook or watcher | (e) Screen sends a fact after its rule |
|---|---|---|---|---|---|
| Order safety | No event for held. New risk: on `engine.reset()`, P2 and fight teardown follow entity order (inference). | One key keeps submit order. No listener needs another listener first. | Safe if no listener needs 2 keys in order. `EventsManager.kt:72` reads a hash-ordered mask. | Synchronous. `runnablesOnDestroyQueued` fires inside `:586`, so it can race the fight's listener. Use `runnablesOnDestroy`. | One more link in the chain. |
| `reset()` first | Not a blocker. `PlayerComponent` has no `reset`. | Slot and flags stay readable. | Same as (b). A submit at the cause site has all data. | `runnablesOnDestroy` runs after reset. `runnablesOnDestroyQueued` runs before it, but `engine.reset()` skips it. | No effect. |
| Frame delay | 0 | Same as today | Cause-site submit comes 1 frame earlier | 0 | 1 frame more |
| Submit during `events.run()` | none | none in play | none in play | none | yes, waits for N+2 (`EventsManager.kt:30`) |
| Filter bugs | Fight still needs a slot 0 filter. | 4 listeners need a filter. The slot 0 rule gets copied. | Fewer filters. | Each watcher must remove itself. `Megaman` is not pooled, so a leak lasts the session. | Rule in 1 place. |
| `O1-R3` | Letter passes | Passes | Passes with the rule above | Passes | Partly; `038` must own it |
| Tests | Needs fight and `Megaman` | Easy: `testServices()` has a real `EventsManager` (`TestServices.kt:29`) | Easy | Easy | Hard if the rule stays in the screen |
| Co-op and PvP | Each new holder adds a branch. | Fits all modes. | Fits. | Fits one owner. It cannot give world news. | Fits the 3 death options. |
| Truth on non-death routes | **`onHostGone` fires on `:586` and at level exit** | True with a `died` fact | True | Cannot tell death from removal | True |

Other facts:
- A `Signal` is a form of (d) (`MegaPlayers.kt:80`). It is synchronous and typed, and it throws on a nested emit (`Signal.kt`). Nothing here needs it.
- `GauntletMaster.kt:227` is the existing watcher pattern. Its comment at `:72-73` gives the map-change risk.

Data at `onDestroy`:
- Readable: slot, play phase, health points, body position, `wasHealthDepleted` (private, `AbstractHealthEntity.kt:51`).
- Lost: velocity, friction, body senses, all locks (`LocksComponent.kt:39`), active behaviors (`BehaviorsComponent.kt:80`).
- I did not check the other component resets one by one.

#### 4. Plans in other items

| Item | Plan | Conflict and action |
|---|---|---|
| `038` | Sends public events from one routine. Splits `PLAYER_SPAWN`. `036` `NOTES.md:310` counts `PLAYER_JUST_DIED` in the chain. | Risk: if the routine takes over this submit, slot 1 and fight deaths lose their event. Add to its scope: the entity keeps the entity fact, and the routine sends the level fact. **Owner of level facts.** |
| `047` | Slot 0 death resets the level. 2 more options are `TODO`s. | **Owner of the rule and the listener filters.** `047a` `SCOPE.md:156` says `047c` or `047e`, but the `SpiderWeb` `TODO` says `047e`. Choose one. |
| `057` | Typed channels and payloads (`idea`, no design). | **Owner of the typed payload.** Do not wait for it. |
| `053` | Umbrella. | No conflict. Record the result there. |
| `013` | `dying` state. Asks if death is per player. | The split answers this. It later owns the transition. |
| `037` | Restores state on respawn. | No conflict. |
| `019` | Spawner notice at destroy (merged). | No conflict. `engine.reset()` skips `onDestroyQueued`. |
| `087` | Component reset order. | No conflict. |
| `097` | Stable key order. | Only (c) would benefit. Not a dependency. |
| `109` | Calls this event a level flow. Asks about lives. | Needs its own round facts later. A killer is not available at `onDestroy`. |

#### 5. Recommended routing

Names:
- `PLAYER_DESTROYED` replaces `PLAYER_JUST_DIED`. It carries the slot and a `died` flag.
- `LEVEL_FAILED` is the new level fact.

`Megaman` keeps one submit in `onDestroy`, with no held branch. It sets a private `died` field at 2 sites: `onHealthDepleted` and the fall at `:489`. `onSpawn` clears it. The submit timing does not change.

| Case | Fact | Who acts | Level reset and life |
|---|---|---|---|
| Slot 0 death | `PLAYER_DESTROYED(0, died=true)` | Screen runs the rule and sends `LEVEL_FAILED`. Gems filter by owner. A fight aborts. | yes, 1 life |
| Slot 1 co-op death | `PLAYER_DESTROYED(1, died=true)` | Owned entities clean up. Screen does nothing. `047c` adds respawn. | no, no life |
| Held P2 death | Same event, no special path | Fight filters on its own P2's slot and `died`. Fight restores the role. | no, no life |
| Level reset of other slots | `PLAYER_DESTROYED(1, died=false)` | Owned entities clean up. Fight ignores it. Screen drops it. | already running |
| Level end, screen change | `died=false` | 0 receivers | no |
| Fall (any slot) | `died=true`, same as that slot's row | Same as that slot's row. No orb ring, as today. | by slot |
| `MegamanClone` (not built) | No player event | A normal enemy. A fight watches it with `runnablesOnDestroy`. It must not extend `Megaman`: `:365` and `:374` use `playerSlot!!`. | no |

##### New API

| API | First real caller | Caller on `master` today? |
|---|---|---|
| Rename to `PLAYER_DESTROYED` (13 lines, 7 files, 0 tests) | The 5 listener kinds | yes |
| `PlayerEventKeys.DIED` | Fight listener | **no** |
| `Megaman` `died` field | The `onDestroy` submit | yes, once `DIED` exists |
| `LEVEL_FAILED` | `GauntletMaster` | yes |
| `heldBy` | None in this design | **no; drop it unless another reader appears** |
| `onHostGone` | None in this design | **no; drop it** |

#### Direct answers

1. **Is the skip with a direct call right?** No.
   - It treats level reset and level exit as a P2 death.
   - It leaves P2's gems and cluster with no cleanup.
   - It copies the `PLAYER_SPAWN` slot skip that `038` plans to remove.
   - The fight needs slot 0 news anyway, so the call saves no listener.
2. **Is the proposal's reasoning correct?** Partly. The 2-key order fact is true, but one key avoids it. `reset()` runs first, but it blocks nothing here.
3. **Should the event fire anyway?** Yes, always. Receivers filter by slot and `died`.
4. **Is the event appropriate at `onDestroy`?** Yes as a fact that the entity left the world. No as a death fact or a level reset fact. So rename it, and move the level fact out.

#### Recommendation

Take (b) now, then add (e) in `038` and `047c`. Reject (a). Use (d) only for a slot-less clone or a real synchronous need. Do not build more of (c) than these 2 facts.

Steps:
1. **Fight branch:**
   - Rename the event and add `died`.
   - Add owner filters at `PreciousGem.kt:215` and `PreciousGemCluster.kt:109`. Without them, P2's death removes P1's guard in the fight.
   - Add a fight listener.
   - Add tests. `REFACTOR_TEST_047` has the rooms `slot_0_death` and `slot_1_death` (`047a` `PLAN.md:26`).
2. **`038` then `047c`:** add `LEVEL_FAILED` and move `GauntletMaster` to it. Add the `SpiderWeb` filter with the same change, or with the `047e` work if you choose that owner.
3. **`057` and `013`:** later, take the typed payload and the state transition.

#### Risks

- A new lethal route that misses the 2 `died` sites reports a false removal. Tests must cover health, fall and `:586`.
- `LEVEL_FAILED` reaches `GauntletMaster` 1 frame later. A wave can spawn in that frame. Inference: `:544` removes those enemies later, so the effect is small.
- `LEVEL_FAILED` adds a link to the old event chain until `038` owns it.
- Until step 2, a slot 1 death still breaks a gauntlet (open since `047a`).
- If both players die in one tick, the fight sees the events in kill-queue order. It needs a double-KO rule.
- `compactor-level` does not touch the changed event files. Its diff adds no conflict.

#### Open questions for the owner

1. **Rename now, or wait for `057`?** I recommend now. The word is false on 4 routes, and the change is 13 lines.
2. **Which item owns the hostile P2?** None exists. I recommend a new item with `047` as a dependency, not a hidden part of `047c`.
3. **Does a fall count as a defeat in the fight?** I recommend yes: `died=true`.
4. **Double KO:** I recommend that the level rule wins, and the fight ends with no result.
5. **Listener filter owner, `047c` or `047e`?** I recommend `047c` for gems and gauntlet, because a death rule change needs them.
6. **Must an unstuck `SpiderWeb` still go on any slot 0 death?** I recommend yes, through `LEVEL_FAILED`. It has no live spawner, so this has low priority.

#### Not read in full

- `Megaman.kt:1-199`.
- `Screen` outside the ranges cited.
- `TiledMapLevelScreen` and its `show`.
- The `SMB3_1` level exit into `LEVEL_SCREEN` (`LevelDefinition.kt:172`). I did not trace it. `Screen:369` is the only place the screen adds itself as a listener, so a stale event needs `start()` to run first.
- `029`, `036` and `019` `NOTES.md`: grep and excerpts only.
- Every `PLAYER_SPAWN`, `PLAYER_READY` and `PLAYER_DONE_DYIN` listener body.

I ran no build and no game. Runtime claims come from reading the code.

##### Critical Files for Implementation
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/special/GauntletMaster.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/PreciousGem.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/events/EventType.kt
