# Notes: 118 Boss fights as routines, rules and leases

Research for this item. Each report is kept in full, with its own date and `master` commit. A note above each report says what later research or an owner decision replaced. If a report and `ITEM.md` disagree, `ITEM.md` wins.

## Agent report: Boss setup booleans (2026-11-02)

Note: Replaced later: the owner rejected this result, because the setup forced a narrow set of fights. The facts stand: the step list, the coupling table, the map counts and the `mini` list. The recommendation "add no boolean" and "keep `mini`" is replaced by the plan steps in `ITEM.md`.

### Boss setup booleans: feasibility report

**Source state:** `master` at `44a4c7e61`. I changed no file, and I did not run the game or a build.

**Paths:** short file names are in these roots.
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/`

**Labels:** "Inference" marks a claim that I did not read directly in the code.

#### 1. Setup steps from spawn to fight start, in order

Measured set: 18 live direct `AbstractBoss` subclasses. `SphinxMiniBoss`, `ReactorMan_OLD` and `MechaDragon_OLD` are commented out and are not counted.

| # | Step | `file:line` | Opt-out or variation today |
|---|---|---|---|
| 1 | Controller off at the room transition | `LevelRoomHandler.kt:54` | None. |
| 2 | Room `event=boss` submits `ENTER_BOSS_ROOM` | `LevelRoomHandler.kt:124-132` | Map data, on the room. |
| 3 | Read `mini` and `end` from the map object | `MegaLevelScreen.kt:699,701` | Map only. This read does not see code-forced values. |
| 4 | Skip the fight if the level is already defeated | `MegaLevelScreen.kt:704-710` | `!mini && end`. |
| 5 | Fetch the boss, set `ready = false`, queue the spawn | `BossSpawnEventHandler.kt:21-24` | None. |
| 6 | Submit `BEGIN_BOSS_SPAWN` | `BossSpawnEventHandler.kt:26` | None. `grep` finds 0 listeners for this event. |
| 7 | Subclass writes spawn props before `super.onSpawn` | see the list below | 8 of 18 classes. |
| 8 | No death from death fixtures, fixture part | `AbstractBoss.kt:125` | 0 opt-outs. |
| 9 | Add event listener and damage listener | `AbstractBoss.kt:127-128` | 0. |
| 10 | Read `bossKey`, `mini`, `end` | `AbstractBoss.kt:130-132` | Spawn prop. |
| 11 | Force no item drop and no cull | `AbstractBoss.kt:134-135` | 0 opt-outs. |
| 12 | Reset `ready`, `defeated`, the between-flag, the defeat timer | `AbstractBoss.kt:137-141` | 0. |
| 13 | Read `spawnDefeatOrbs` (default `!mini`) | `AbstractBoss.kt:142` | Spawn prop; 4 classes force `false`. |
| 14 | Store the music prop | `AbstractBoss.kt:144` | Spawn prop; 4 classes force it. |
| 15 | Health to maximum | `AbstractHealthEntity.kt:63` | 0 opt-outs. It is not boss code. |
| 16 | No death from death fixtures, entity part | `AbstractBoss.kt:148` | 0 opt-outs. `MechaDragon.kt:210` writes it again. |
| 17 | Intro: `preReady` and `isReady` each frame | `AbstractBoss.kt:195-200,242` | `isReady` abstract: 18 of 18 implement it. `preReady`: 1 override (`BigAssMaverickRobot`). |
| 18 | Boss cannot deal damage before `ready` | `AbstractBoss.kt:178` | 1 class narrows `canDamage` further (`InfernoMan.kt:257`). |
| 19 | `onReady` submits `BOSS_READY` and sets the between-flag | `AbstractBoss.kt:246-258` | 9 overrides. All 9 call `super`, so 0 skip this step. |
| 20 | Bar or no bar | `MegaLevelScreen.kt:719-720` | `mini` only. |
| 21 | Fill type | `AbstractBoss.kt:240`, `MegaLevelScreen.kt:722-726` | 0 overrides. |
| 22 | Pause music in the fill | `MegaLevelScreen.kt:728-731` | Follows the fill type. |
| 23 | Bar build and fill | `BossHealthHandler.kt:38-81`, `MegaLevelScreen.kt:921` | Display only. |
| 24 | Freeze all systems but sprites and animations | `MegaLevelScreen.kt:743-745` | Same branch as the bar, so `mini` only. |
| 25 | `END_BOSS_SPAWN`: controller on, all systems on | `MegaLevelScreen.kt:720,737,750-754` | 0. Every path submits it. |
| 26 | Boss clears the between-flag and starts music | `AbstractBoss.kt:173,260-274` | `playBossMusic`: 1 override (`WilyFinalBoss.kt:333`, a run-time predicate). `onEndBossSpawnEvent`: 1 override (`WilyFinalBoss.kt:327`). |

**Spawn props forced in code (step 7), with lines**
- `mini`: 3 classes (`PenguinMiniBoss.kt:84`, `MoonHead.kt:133`, `MechaDragon.kt:206`).
- `ORB`: 4 classes (`MoonHead.kt:132`, `MechaDragon.kt:207`, `ReactorMonkey.kt:120`, `WilyFinalBoss.kt:238`).
- `MUSIC`: 4 classes (`ElecDevil.kt:139`, `BigAssMaverickRobot.kt:211`, `GutsTank.kt:204`, `WilyFinalBoss.kt:237`).
- `END`: 1 class (`WilyFinalBoss.kt:239`).

**`isReady` by kind (18 total)**
- 10 use a timer.
- 4 are the constant `true` (`PenguinMiniBoss`, `SigmaRat`, `MoonHead`, `ReactorMonkey`).
- 1 is `mini ||` a timer (`Bospider.kt:197`).
- 1 uses a flag (`WilyFinalBoss.kt:380`).
- 1 uses a timer and a check on all players (`GutsTank.kt:279-280`).
- 1 uses a body and hands condition (`BigAssMaverickRobot.kt:397`).

**`onReady` by kind (9 total)**
- 5 turn gravity on.
- 2 only log.
- 1 activates body pieces (`ElecDevil`).
- 1 plays music (`Bospider`).

**What `mini` controls**
`mini` is the only true opt-out in the flow, and it is one bundled boolean. It controls these:
- the bar and the freeze (`MegaLevelScreen.kt:720`)
- music (`AbstractBoss.kt:238`)
- orbs (`AbstractBoss.kt:142`)
- the defeat event (`AbstractBoss.kt:236`)
- the defeat lock and controller (`MegaLevelScreen.kt:766-775`)
- music after the defeat (`MegaLevelScreen.kt:803`)
- the level skip (`MegaLevelScreen.kt:704`)

**Map data (22 live maps from `LevelDefinition`, parsed read-only)**
- 25 boss objects, in 15 of the 18 classes.
- `SigmaRat`, `PenguinMiniBoss` and `Bospider` are in no live map.
- Props: `boss_key` on 13, `end=false` on 8, `mini=true` on 5, `music` on 1 (`GlacierMan`, equal to the default), `orb` on 0.

#### 2. Where a refill really happens

**The real refill.** It happens once, at `AbstractHealthEntity.kt:63`, inside `onSpawn`. Every health entity runs this line. The bar fill changes no health. It only counts a local `temp` value (`BossHealthHandler.kt:23,28,66`).

**The one other write.** `WilyFinalBoss.kt:3354` calls `setHealthToMax()` at a phase change, with no bar fill.

**Why a boolean is not meaningful for an ordinary boss.** For an ordinary boss, step 15 is a pool reset, not a choice. These facts show it:
- `PointsComponent` has no `reset()` (`IGameComponent.kt:7` is empty), so a pooled boss keeps its old health.
- `PointsSystem.kt:17-25` calls the health listener each frame with no change check.
- `AbstractBoss.definePointsComponent` triggers a defeat at health 0 (`AbstractBoss.kt:229-231`).

Result (inference from these lines): with refill off, a boss that was defeated earlier in the session starts its next fight at 0 and is defeated at once. A boss that was damaged before a player death keeps that damage.

So "refill" has meaning only for a fighter that exists before the fight, such as `Megaman` P2. `Megaman` also sets full health in its own `onSpawn` (`Megaman.kt:335`), so the fight-start write is a different event from a spawn.

#### 3. The steps are not independent

| Combination | Result | Evidence |
|---|---|---|
| Refill off, `BIT_BY_BIT`, health below maximum | Bar counts to maximum, then drops to the real value when the timer ends. The display is false for the whole fill. | `BossHealthHandler.kt:23,59,63-66` |
| Refill off, `ALL_AT_ONCE` | Correct: the bar reads real health at once. But no production path runs this branch today. | `BossHealthHandler.kt:23,74-78`; 0 overrides of `getHealthFillType` |
| Refill on, no damage lock in the intro | Fighter can lose health after the refill and before the fight. The intro runs with the world on (`AbstractBoss.kt:195-200`). The freeze starts only at step 24. | Inference from the order. |
| Damage lock, instant death fixture | Lock does not protect. `DeathRule` kills when `instant` is true, whatever the `invincible` value is. | `DeathRule.kt:26-29`, `Megaman.kt:88-89` |
| Damage lock, fight ended by `PLAYER_SPAWN` in the intro | Boss destroys itself (`AbstractBoss.kt:174`). `Megaman` clears the lock only in its own `onSpawn` (`Megaman.kt:342`). P2 stays locked unless the fight entity clears the lock in its destroy path. | Inference. |
| Bar off or fill off without `END_BOSS_SPAWN` | Controller stays off (`LevelRoomHandler.kt:54` is undone at `MegaLevelScreen.kt:751`). Boss logic stays stopped in 10 files that read the between-flag, and music does not start. | `MegaLevelScreen.kt:750-754`; 11 live lines |
| Freeze on, bar off | Systems stay off until `END_BOSS_SPAWN`. Today these two share one branch, so this combination cannot occur. | `MegaLevelScreen.kt:741-745` |
| Intro off (`isReady` forced true) | Only 4 classes already do this. The others hold real work behind the intro, for example gravity on in `onReady` (5 classes) and the `WilyFinalBoss` init sequence. | Section 1 lists |
| Death exemption off on an ordinary boss | Needs 2 writes changed, because `DeathRule` checks 2 keys (`DeathRule.kt:20-24`). `MechaDragon.kt:210` is a third write that would override the boolean. | Section 1 |

**Fixture write and rebuilt bodies.** `WilyFinalBoss` builds its body after `super.onSpawn` (`WilyFinalBoss.kt:246-247`). So the fixture write at `AbstractBoss.kt:125` may miss its fixtures. Only the entity write at line 148 protects them (inference).

**Two groups.**
- These steps are bound to each other: bar, fill type, music pause, freeze and the end event.
- These steps form a pre-existing-fighter set: refill, damage lock, health route and watcher.

The only step that seems independent in the data is the death exemption. Even there, 2 keys must change together.

#### 4. Options

| | (a) One boolean per step | (b) `BossSetup` data class | (c) Spawn props, typed once | (d) Hooks only |
|---|---|---|---|---|
| Fit today | 0 steps have a second value to set, except what `mini` already bundles. | Same limit. It has one precedent: `SessionRules` (`SessionRules.kt:7`), which has 1 field. | Route exists: map object to `bossSpawnProps` to `onSpawn` (`MegaLevelScreen.kt:696`). | This is today's form: `isReady`, `onReady`, `playBossMusic`, `getHealthFillType`. |
| Can express run-time logic | No | No | No | Yes. Needed for `isReady` (18 implementations) and `WilyFinalBoss.kt:333`. |
| Bad combinations | All are possible | All are possible, but one place can validate them | All are possible, and a map author can set them | Limited by each class |
| `O1-R4` | Holds (typed field) | Holds | A new `ConstKeys` key is allowed only for map data | Holds |
| `O1-R5` | Fails for any field with no non-default caller | Fails if it has no caller | Needs a real map object that sets the value | Holds |
| `O1-R3` | Adds skip branches in the ordered event chain | Same | Same | No new order |
| `050` | Fields decode to JSON keys, one by one | Best decode target (inference) | Same names can be reused | Hooks cannot be data |
| `052` | No constraint: `052` is an `idea` with no value list | Same | Same | Same |

**Facts about the related items**
- `050` has no format, and it depends on `021` and `022`. Its first entity kinds are an open question, and bosses are not among the examples.
- `048` keeps the Kotlin classes (`048` `ITEM.md:53`).
- `015` `NOTES.md:65` keeps guards with logic in code, so `isReady` stays in code.
- `O7-R2` waits for `022`. Until `022`, a new prop is `waiting`, and the branch repeats the `getOrDefault` pattern.

**The prop value has two sources today, which is a real defect in the pattern.** `WilyFinalBoss.kt:239` forces `END=false`, but its map object has no `end`, so `MegaLevelScreen.kt:701` reads `true`. `PenguinMiniBoss.kt:84` has the same split for `mini`. Any prop that the screen and the boss both read must be resolved once, before step 3.

**Code or map?** The answer follows from who chooses the value.
- Boss nature (`isReady`, gravity on ready): code.
- Fight placement (`end`, `boss_key`, `mini`): map.
- P2 refill: this is a mode rule or a level rule. See the open questions.

#### 5. Which fields have a real caller

"Live non-default caller" means production code that sets a value other than the default.

| New API | First caller | Live non-default caller today? |
|---|---|---|
| `BossComponent.refill` | P2 fight entity | **None.** The owner default is refill, so `false` has no caller. |
| `BossComponent.healthBar` | `MegaLevelScreen.kt:720` reader | Redundant: it equals `!mini`. |
| `BossComponent.healthFillType` | Move of `AbstractBoss.kt:240` | **None.** `ALL_AT_ONCE` has 0 callers. |
| `BossComponent.freezeInFill` | None | **None.** It also adds to the broken `O2-R10`. |
| `BossComponent.playMusic` boolean | `AbstractBoss.kt:238` | `!mini` is derived, and `WilyFinalBoss.kt:333` needs a predicate. |
| `BossComponent.deathImmune` | `AbstractBoss.kt:125,148` | **None** while P2 and bosses use separate setup paths. |
| `BossComponent.damageLockInIntro` | P2 fight entity | **None.** No ordinary boss locks a player in the intro. |
| `BossSetup` | None | **None** with fewer than 2 fields that have callers. |
| `MegaLockReason` new value for the intro | P2 fight entity | **None today.** It is needed for the lock: none of the 7 reasons fits (`MegaLockReason.kt:5-13`). |

**Real data that moves in `048` step 6** (existing values, not new setup API):
- `mini`, `end`, `bossKey`
- orbs
- music value
- `isReady`, `onReady`
- the `playBossMusic` predicate

**Which one first.** Add no boolean in `048`. The first boolean is `refill`, and only in the branch that has a real caller for `false`. A test map alone does not count as a caller under the "During the branch" rule (inference: `docs/mega-refactor/CLAUDE.md`, "A test alone is not a caller").

**Prerequisite.** That branch must change the fill target at `BossHealthHandler.kt:59` from `getMaxHealth()` to current health. This is not a new API. It changes the display only if a boss is hit before the bar, because `canBeDamagedBy` has no `ready` gate (`AbstractBoss.kt:180`).

#### Conflicts and doc gaps

- **Conflict in the brief.** "Already known" says the fight entity writes "no death from death fixtures" on P2. The owner decision says that anything that kills a playable character also kills it as a boss. Both cannot stand. I recommend that the write is removed. `Megaman` writes neither death key today (`grep`), so P2 then needs no change for this.
- **No repo record of the P2 design.** `BossComponent` appears in docs only at `048` `NOTES.md:27`. `grep` finds no hostile P2 design and no item for it. `013` `ITEM.md:43` says a PvP match has no boss. So the first caller has no item ID. I took the design from your brief and did not verify it.
- **Subclass count.** `O1` and `048` `NOTES.md` say 17. The correct count is 18 live classes. The "21 `isReady`" count in `048` includes 3 commented-out lines.

#### Not read in full

- 18 boss files: I read `grep` hits and the bodies of the flow overrides only.
- `MegaLevelScreen.kt`: lines 530-540, 660-830 and 912-930 only.
- `Megaman.kt`: parts only.
- Not read: `NOTES.md` of `022`, `102`, `030`, `112`, and items `101`, `108`, `012` beyond `grep`.
- `docs/mega-refactor/ROADMAP.md` has an uncommitted change, which I did not read.
- The worktree `.worktrees/compactor-level`.
- TMX: live maps only. I did not check that each of the 25 boss objects is linked from a boss room.

#### Direct answer

**Feasible in code, but mostly not useful.** A boolean can guard any step. Only the `refill` boolean has a meaning, and only for a fighter that exists before the fight. On an ordinary boss, `refill = false` is a defect, because it bypasses the pool reset. Today no production code wants `false`. Most steps are coupled, so independent booleans allow states that break the flow.

#### Recommendation

1. Add no boolean in `048`. Move `mini`, `end`, `bossKey`, orbs and music as typed fight data, and keep `isReady`, `onReady` and the music predicate as hooks.
2. In the P2 fight entity, do the refill, lock, health route and watcher as one fixed routine, with no flags. This obeys `O1-R3` ("an ordered sequence is one routine").
3. Treat the bar, fill, music pause, freeze and end event as one unit. Keep `mini` as its switch until `013` makes it a state.
4. When a real case wants no refill, add one `refill: Boolean = true` on `BossComponent`. Apply it only in the P2 routine, not at `AbstractHealthEntity.kt:63`. Fix the fill target in the same branch.
5. Make `BossSetup` only when a second field has a caller. Then use (b) as the typed form and (c) as its source, resolved once, before step 3.

#### Risks

- An early boolean breaks `O1-R5`, as `ComponentKey` did in `029a`.
- A refill flag placed at the wrong level gives a boss at 0 health, or a boss that carries damage from a prior attempt.
- Instant death in the intro kills a locked P2. The watcher must handle a P2 that dies before `END_BOSS_SPAWN`.
- The damage lock can stay on P2 after `PLAYER_SPAWN` ends the fight early.
- The bar shows false health until the fill target changes.
- A map-set flag that the screen and the boss read separately repeats the `WilyFinalBoss` `end` split.
- `ALL_AT_ONCE` has no production use, so it is an unproven path if P2 starts to use it (inference).
- `TURN_CONTROLLER_OFF` and `TURN_CONTROLLER_ON` act on all players (`O2-R9`), so a per-fighter intro has no per-slot control.

#### Open questions for the owner

1. **Does P2 keep any immunity to death fixtures in the fight?** Recommendation: no, as your rule says. Remove the write from the design.
2. **Does any real case want P2 to keep partial health?** Recommendation: if none exists now, add no boolean, and refill always.
3. **If such a case exists, who chooses it: the map, or the mode?** Recommendation: the map object if it varies per fight. Use `SessionRules` and `102` if it varies per mode.
4. **Which item owns the hostile P2 fight?** Recommendation: open a new item, or place it in `047e`, before any API work, so that each new API has a named branch and caller.
5. **Must P2 use the slow fill or the instant bar?** Recommendation: the slow fill with the changed fill target, because it is the only path that production runs today.
6. **Do you want the `WilyFinalBoss` `end` split fixed?** Recommendation: file it in `docs/issues/`, and do not fix it in the boolean work. Whether the current result is intended is unclear.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/stats/BossHealthHandler.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractHealthEntity.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules/DeathRule.kt

## Agent report: Boss fight redesign (2026-11-02)

### Boss fight setup: redesign research

Base: `master` at `b3e792bc4`. All work was read-only, and I changed no file. The working tree has 2 docs modified by someone else (`ROADMAP.md`, one issue file). I read `ROADMAP.md` with those edits.

Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` unless they say otherwise. Line numbers are from this commit, so older numbers in item notes are stale.

#### 0. Facts rechecked, and new facts

The known facts in the brief are correct at this commit. These additions and corrections change the design.

| # | Fact | Evidence |
|---|---|---|
| 1 | 18 live subclasses is correct. `O1` and `048` say 17, because `ReactorMonkey` has its superclass on a second line. | `entities/bosses/ReactorMonkey.kt:61` |
| 2 | `048` says 21 `isReady` overrides. 3 are in commented-out files; live count is 18. | `isReady` grep |
| 3 | `BitsBar` is already generic, and it reads 2 suppliers. `BossHealthHandler.set` already takes `IHealthEntity`. The boss tie is in the caller and in the one-bar field. | `drawables/ui/BitsBar.kt:20-21`, `screens/levels/stats/BossHealthHandler.kt:25,38` |
| 4 | **A fight entity cannot run its own freeze today.** `MegaLevelScreen.kt:743-745` turns off `UpdatablesSystem`. A `GauntletMaster`-style entity then stops updating. This is why the fill runs in the screen. | `MegaLevelScreen.kt:921` |
| 5 | **`spawn` only queues.** `onSpawn` runs later, so `boss.mini` is not set when `ENTER_BOSS_ROOM` runs. Facts must resolve before spawn. | `MegaLevelScreen.kt:713`, `BossSpawnEventHandler.kt:24`, `engine/.../GameEngine.kt` |
| 6 | A fight starts only at the end of a camera transition. `onSetToRoomNoTrans` submits no room event. This is why `docs/mega-refactor/CLAUDE.md` forbids naming a boss room in `--room`. | `LevelRoomHandler.kt:124-136,157-166` |
| 7 | A room holds 1 boss object. | `MegaLevelScreen.kt:695` |
| 8 | `canBeDamagedBy` does not check `ready`. A player shot in flight can hit a boss during its intro. | `AbstractBoss.kt:180`, `LevelRoomHandler.kt:73-76` |
| 9 | `BEGIN_BOSS_SPAWN` has no listener. | `BossSpawnEventHandler.kt:26`, `MegaLevelScreen.kt:154-160` |
| 10 | Live maps use 25 fights in 12 maps: 5 mini, 8 `end=false`, 12 default. 3 classes have 0 live fights: `Bospider`, `SigmaRat`, `PenguinMiniBoss`. | Parse of each `LevelDefinition` map |
| 11 | A boss rush already exists as rooms: `WilyStage3_v4.tmx` has 5 boss rooms, `WilyStage1_v2` and `WilyStage2_v2` have 3 each. | Same parse |
| 12 | Boss survives a room change, its children do not. `LevelRoomHandler.kt:72` destroys `ENEMY`; a boss is `BOSS`. | `AbstractBoss.kt:311` |
| 13 | Cleanup is global: a defeat reaches all `IBossListener` entities in the level. `Spike` and `PreciousGem` also react to any `BOSS_DEFEATED`. | `MegaLevelScreen.kt:778-780`, `Spike.kt:204`, `PreciousGem.kt:216` |
| 14 | `Wily`'s `end=false` divergence is real: the map has no `end`, so the screen reads `true`. It needs `isLevelDefeated(WILY_STAGE_3)`. That write is at `MegaLevelScreen.kt:829` on `END_LEVEL`, but `Wily` ends with `GAME_COMPLETE`. So normal play cannot reach it (inference). | `DrWily.kt:151` |
| 15 | Frame chain: `events.run()` runs after `screen.render`. A submit during `run` waits one more frame. Stage boss: controller on is 1 run after `END_BOSS_SPAWN`. Mini boss: controller on is 2 runs after `BOSS_READY`. | `MegamanMaverickGame.kt:626`, `EventsManager.kt:30` |

#### 1. Does an existing item cover this?

**No existing item covers this redesign.** Five items touch parts of it, and each keeps today's shape or covers one part.

| Item | Status | Quoted scope | Overlap | Why it does not cover the redesign |
|---|---|---|---|---|
| `013` | planned | "`LevelFlow` states (spawning, playing, room transition, boss intro, boss fight, victory, dying, game over)" | Who orders the flow | It puts the classic chain into level states. Section 4 shows this is the same fault. |
| `048` | planned | Step 6: "Move the boss flow and its readers (`MegaLevelScreen`, `Gate`, `IBossListener`)." Notes: "`BossComponent`, with states from `015`" | The boss flow code | It moves the flow. It does not change what a fight can do. |
| `082` | planned | "**Boss pre-fight talk:** an optional scene between `ENTER_BOSS_ROOM` and the health fill."; `082b`: "the boss pre-fight talk hook in `AbstractBoss`" | The step runner, and one intro | It adds one more hook to the base class, and `082` owns no fight logic. |
| `042` | planned | "Later: `BossPhases` / `HealthCurve` and `ShotPattern`." | Phases | Phases inside one entity only. |
| `047e` | not started | "Bosses and gauntlets" | Co-op rules | It needs a fight owner. It does not define one. |
| `053` | idea | "Remove fragile event ordering in `MegaLevelScreen`" | Umbrella | It has no concrete boss scope. |
| `012`, `057` | planned, idea | Named freezes; typed events | One concern each | They are services that a fight uses. |

`029` `NOTES.md:663` also lists a `BossFlowPolicy` idea, with no item. A search for "encounter" finds only unrelated level-design text in `074`, `075` and `080`.

**Fit:** a new item, the next free ID is `118`. An extension does not fit, because no item above owns the whole flow. Proposed objectives: `O1`, `O4`, `O2`. Linked: `O7`, `O9`.

The new item needs edits to 4 other items:
- `013`: remove the boss states.
- `048`: reduce step 6.
- `082`: replace the `082b` hook with a step.
- `042`: link `BossPhases`.

#### 2. Design faults in today's flow

| # | Fault | Evidence | Variations it blocks |
|---|---|---|---|
| F1 | **Bundled `mini`.** One boolean decides 8 behaviors: bar and fill, freeze, music pause, orbs, boss music, defeat event, defeat lock and controller, music after defeat. It also gates the level skip. | `AbstractBoss.kt:101,131,142,236,238`; `MegaLevelScreen.kt:704,720,766,803`; `Bospider.kt:197,202` | Bar with no freeze, freeze with no bar, mini becomes full, no music change, any mix |
| F2 | **Facts with 2 sources.** 12 lines in 8 subclasses overwrite map props. The screen reads the map before spawn. `mini`: `PenguinMiniBoss.kt:84`, `MoonHead.kt:133`, `MechaDragon.kt:206`. `end`: `WilyFinalBoss.kt:239`. `music`: `BigAssMaverickRobot.kt:211`, `GutsTank.kt:204`, `ElecDevil.kt:139`, `WilyFinalBoss.kt:237`. `orb`: `MoonHead.kt:132`, `ReactorMonkey.kt:120`, `MechaDragon.kt:207`, `WilyFinalBoss.kt:238`. | `MegaLevelScreen.kt:699,701` against `AbstractBoss.kt:131-132` | End-of-level by data, music by data; a map value is silently ignored |
| F3 | **Screen owns fight logic.** 114 lines in 6 event branches, plus 3 other sites. | `MegaLevelScreen.kt:691-804,537,921,1007` | All variations: a new one needs a screen edit. Two fights at once. |
| F4 | **Fixed event chain.** `ENTER_BOSS_ROOM`, `BOSS_READY`, `END_BOSS_SPAWN`, `TURN_CONTROLLER_ON`, then `BOSS_DEFEATED`, `BOSS_DEAD`, and `VICTORY_EVENT` or `INTERMEDIATE_BOSS_DEAD`. Each hop waits for `events.run`. | `AbstractBoss.kt:216,250,278`; `MegaLevelScreen.kt:720,737,751,795` | Dialog intro, skip, any new stage, order changes |
| F5 | **`betweenReadyAndEndBossSpawnEvent`.** It is a flag that mirrors event lag. 11 reads in 10 files. It stops AI in the frame after `onReady`, and 8 bosses do not read it. | `AbstractBoss.kt:109,257,263`; `DesertMan.kt:237`, `InfernoMan.kt:275`, `GlacierMan.kt:241`, `MoonMan.kt:289`, `RodentMan.kt:364`, `PreciousWoman.kt:349,677`, `TimberWoman.kt:591`, `ReactorMan.kt:293`, `BigAssMaverickRobot.kt:474`, `BigAssMaverickRobotHand.kt:306` | Fill while the fight runs, bar at any moment, start while the player moves |
| F6 | **Flow hooks on the base class.** 14 open or abstract functions. 7 sites need `AbstractBoss` by type. | `AbstractBoss.kt:190-293`; `MegaLevelScreen.kt:717,764,793,802`; `Gate.kt:198`; `IBossListener.kt:5`; `BossSpawnEventHandler.kt:21` | Hostile P2, destroy-an-object win, group as target. Conflicts with `O1-R2` and with `048`. |
| F7 | **Bar tied to one boss and to the flow.** 1 bar field, fixed position. Fill finish is the fight start, through callbacks that carry music and the end event. `temp` hides real health in the fill. | `BossHealthHandler.kt:23,25-28,47-54,66`; `MegaLevelScreen.kt:728-741` | 2 bars, group bar, other entity, partial fill, fill with no freeze |
| F8 | **Bar lifetime is by hand.** `unset` is at 2 sites. Any other boss end keeps a stale entity. The fill is a UI timer that controls a freeze (`O2-R10`). | `MegaLevelScreen.kt:537,782,921` | Boss flees, netplay |
| F9 | **Room controls the fight start.** The room handler chooses whether the controller comes back. | `LevelRoomHandler.kt:124-136` | Trigger zone, start in motion, chase, checkpoint in the room |
| F10 | **Room lock is emergent.** It comes from gate state, and it has 2 unlock channels. A `VICTORY_EVENT` or `null` exit opens no gate. | `Gate.kt:84-85,196-226` | No lock, no gates, fight in motion, gate by other win |
| F11 | **One win, no lose.** A win is "this entity reaches 0 health". `Wily` overrides 2 hooks to stop the defeat. | `AbstractBoss.kt:222,230,276`; `WilyFinalBoss.kt:341-357` | Timer, destroy an object, flee, part as weak point |
| F12 | **Global cleanup.** See fact 13. | `MegaLevelScreen.kt:778-780` | 2 fights at once; one defeat clears the other fight |
| F13 | **Special death path.** 2 `DeathRule` keys plus a replaced points listener, so `onHealthDepletedCallbacks` does not run. | `AbstractBoss.kt:125,148,224-233`; `DeathRule.kt:20,23`; `MechaDragon.kt:210` | Hostile P2 (owner: one cause kills in both roles) |
| F14 | **Slot 0 rules the end.** Early return when slot 0 is dead. `players.all` waits for a dead slot. `TURN_CONTROLLER_*` acts on all players. | `MegaLevelScreen.kt:759,788`; `GutsTank.kt:280`, `ElecDevil.kt:339`, `WilyFinalBoss.kt:448`; `O2-R9` | Co-op, PvP |
| F15 | **No fight memory.** Health always resets, and the only rematch rule is `isLevelDefeated`. Re-entry to a mid-level boss room spawns again (inference from the guard at `:704`). | `AbstractHealthEntity.kt:63`; `MegaLevelScreen.kt:704-710` | Partial health, carry-over, intro seen once |

#### 3. Candidate designs

More variations to test:
- start with no transition
- rematch rule
- cleanup per fight
- weak part as the bar
- late join in a fight
- Spec Ops objective
- reward by data
- lose with no player death

##### Design A: script only

One ordered list of steps per fight, with the win condition as a wait step.

```kotlin
interface Step<C> {
    fun begin(c: C) {}
    fun update(c: C, delta: Float) {}
    fun isFinished(c: C): Boolean
    fun end(c: C, interrupted: Boolean) {}
}
fun standardBoss(music: MusicAsset) = script<FightCtx> {
    spawnBoss(); awaitReady(); freezeWorld(); fillBar(); releaseWorld()
    controllerOn(); playMusic(music); awaitDefeated(); exitStage()
}
```

- **`mini`:** gone. Each preset lists its steps.
- **Code:** steps. **Data:** a list of step names and params.
- **Classic game:** presets list today's order.
- **Impossible:** a hidden order between listeners, and a flow step that no list shows.
- **Weak point:** the fight is a line. "Win OR timer OR lose" needs `race`, and a cross-cutting rule repeats in each script.
- **`O1-R3`:** one routine.

##### Design B: services plus a fight state machine

Each concern is one service, and one `015` `Fsm` holds fixed phases.

```kotlin
enum class FightPhase { INTRO, ACTIVE, OUTRO, DONE }
data class FightConfig(
    val bar: BarPolicy, val music: MusicPolicy, val hold: HoldPolicy,
    val lock: LockPolicy, val exit: ExitPolicy
)
sealed interface BarPolicy {
    object None : BarPolicy
    data class Fill(val source: BarSource) : BarPolicy
}
```

- **`mini`:** becomes 5 typed fields.
- **Code:** services and phases. **Data:** `FightConfig` maps 1 to 1 to fields.
- **Classic game:** a default config.
- **Impossible:** mix of flags that a sealed type cannot state.
- **Weak point:** order is closed. "Talk, then fill, then a second talk" needs a new phase or policy. This repeats the current fault, with an enum in place of a boolean.
- **Blocks `015`:** yes.
- **X6 evidence:** the boss object itself calls the parts (`BossWarning`, `BossMusicRequest`), so services are a proven base (`~/IdeaProjects/mmx-demake-tools/docs/engine.md`, "Boss entry and exit").
- **`O1-R3`:** one state machine.

##### Design C: encounter entity with conditions

An invisible entity owns the fight, as `GauntletMaster` does.

```kotlin
class Encounter(services: MegaServices) : MegaGameEntity(services), IEventListener {
    val participants = Array<MegaGameEntity>()
    lateinit var win: Condition
    lateinit var lose: Condition
}
sealed interface Condition {
    data class AllDefeated(val group: String?) : Condition
    data class Survive(val seconds: Float) : Condition
    data class AnyOf(val all: List<Condition>) : Condition
}
```

- **`mini`:** no answer alone. The intro has no owner.
- **Code:** entity. **Data:** conditions and participants.
- **Classic game:** needs an extra intro type.
- **Impossible:** a fight with no owner, and an end with no rule.
- **Weak point:** fact 4. The entity stops when the world freezes, so it needs the `012` exempt flag first.
- **`O1-R3`:** holds only if order has one owner, and this design does not name one.

##### Design D: composition (recommended)

- One `Fight` object owns lifetime.
- One `Routine` owns order.
- Outcome rules are values.
- Each concern is a service that holds a lease.
- A `FightDirector` ticks fights in the simulation.

```kotlin
class Fight(val key: String, val plan: FightPlan, val services: MegaServices) {
    val cast = Array<MegaGameEntity>()          // no type check on boss
    private val leases = Array<Lease>()         // released at close, in reverse order
    fun lease(l: Lease) { leases.add(l) }
}

class FightPlan(
    val intro: Array<Step<Fight>>,
    val win: FightRule, val lose: FightRule?,
    val outro: Array<Step<Fight>>
)

sealed interface FightRule {
    data class AllDefeated(val group: String? = null) : FightRule
    data class Survive(val seconds: Float) : FightRule
    data class AnyOf(val rules: List<FightRule>) : FightRule
}

class FightDirector(private val services: MegaServices) {
    private val fights = Array<Fight>()
    fun start(fight: Fight) { fights.add(fight) }
    fun update(delta: Float) { /* each routine, then each rule */ }
    fun cancelAll() { /* end(interrupted = true), release leases */ }
}

interface Lease { fun release() }
interface HudBars { fun show(spec: BarSpec): Lease }
fun interface BarSource { fun read(): Int }
```

Rules for this design:
- A step changes no state with no lease. So a freeze, a lock or a bar cannot stay on after a death or a skip. This is the `082` rule "End and interrupt".
- A rule ends the wait. A step never polls the win itself. So win, lose and timer are parallel values, and order stays linear.
- `mini` leaves. It stays only as a legacy map key that picks a preset.

Why `Fight` is not an entity: fact 4 stops it during a freeze, and a fight can outlive each participant. `Fight` is itself the "invisible fight entity" of the earlier design. Per-participant data stays on the participant as `BossComponent`.

**Code stays:** step classes, boss AI, rule evaluation, preset functions. **Data later (`050`, `022`):** `fight=<preset>` in Tiled, then step lists and rules in a file. Guards with logic stay in code.

**Classic game:** 3 presets copy today (section 5).

**Impossible after D:**
- a flag that mirrors event lag
- a freeze with no owner
- a bar on a dead entity
- a fact with 2 sources
- two fights that clear each other's hazards

**`O1-R3`:** order is one `Routine`, and the `Routine` receives no event for its order. Events remain as output for listeners.

**Rejected design:** a table of "when X then Y" rules. Order then comes from rule firing, which breaks `O1-R3`.

##### Stress table

All D cells assume the type is built when its caller arrives. "New" means a step or a rule value.

| Variation | A | B | C | D |
|---|---|---|---|---|
| No bar | Omit step | `BarPolicy.None` | Field needed | Omit step |
| Bar at any moment | Step at any place | Phase edges only | Condition needed | `showBar` step, or in `outro` |
| 2 bars | Two steps | New policy | Field list | Two `showBar` leases |
| Bar shows other entity | Step param | `BarSource` | Param | `BarSource` |
| Bar for a group | Step param | `BarSource` | Param | `BarSource` sums `cast` group |
| Partial health | `setHealth` step | New field | Participant field | `setHealth` step; fill target is the real value |
| Carry-over health | Needs a store | Needs a store | Needs a store | `FightMemory` keyed by `Fight.key` |
| No freeze | Omit step | `HoldPolicy` | No owner | Omit `holdWorld` |
| Start while player moves | Omit hold | New policy | Trigger needed | Omit `holdInput`; start from a trigger |
| Dialog intro | `say` step | New phase | No owner | `say` step (`082`) |
| Skippable intro | Runner skip | New logic | No owner | `Routine.skip`, then each `end` |
| No music change | Omit step | `MusicPolicy` | Field | Omit step |
| Music by phase | Steps in order | New policy | Conditions | `playMusic` step in each phase part |
| No room lock | Omit step | `LockPolicy` | Field | Omit `lockArena` |
| No gates | Omit step | `LockPolicy` | Field | `lockCamera` lease in place of gates |
| Chase | New steps | New policy | Trigger list | `followRooms` step; fight not bound to a room |
| Fight moves rooms | Keep script | Phase clash | Entity survives | `Fight` survives; room change is input to a rule |
| Multiple bosses | `await` needs a group | Phase clash | Participants | `cast` plus `AllDefeated` |
| Boss rush | Long script, or next script | Repeat config | Chain | Rooms as today, or `nextFight` step |
| Mini becomes full | New script | New config | Two entities | Same `Fight.key`, next plan |
| Phases in one entity | Boss keeps its logic | Same | Same | Same; `042` `BossPhases` |
| Phases across entities | Steps | New phase | Participants | `swapCast` step, `FightMemory` |
| Hostile P2 | Steps | Policies plus type | Participant | `cast` holds P2; needs `030`, `047` |
| Defeat restores P2 | Step | New policy | Handler | `restorePlayer` outro step (destroy and spawn) |
| Waves of enemies | Loop not linear | Service | Participants | `waves` step that holds an FSM |
| Survive a timer | `race` | New exit | Rule | `FightRule.Survive` |
| Destroy an object | Step | New exit | Rule | `FightRule.AllDefeated(group)` on the object |
| Boss flees | Step | New exit | Rule | `FightRule.Fled`; bar lease ends anyway |
| No level exit | Omit step | `ExitPolicy` | Field | Omit step |
| Defeat does not end level | Other step | `ExitPolicy` | Field | `midBoss` preset; live today |
| Exit by data | Name | Field | Field | Preset name, then data |
| Co-op | Steps use all present players | Same | Same | Steps use the present-player query (`047e`) |
| PvP | No fight, or one script | Config | Conditions | `Fight` with no boss, or none: `109` decides |
| Start with no transition | Trigger | Trigger | Trigger | Any caller of `start` |
| Rematch rule | Step | Field | Field | `FightMemory` check before start |
| Cleanup per fight | Needs membership | Same | Same | `Fight` destroys what it spawned |
| Spec Ops objective | New script | Poor fit | Conditions | `FightRule` values, `108` decides |

#### 4. Shared sequence type, and `013`

| Flow | Measured shape | Fit for a step list |
|---|---|---|
| `PlayerSpawnEventHandler` | 4 timed parts in `when` (`:147-157`), 1 parallel blink (`:159-163`), begin block `:116-140`, end block `:210-226`, own draw | Yes, with `parallel` or a step that owns the blink |
| `EndLevelEventHandler` | 5 timers; each part resets the next by hand (`:138,:184,:198,:219`) | Yes; this is a step list written by hand |
| `PlayerDeathEventHandler` | 2 parts | Yes |
| `RespawnRoutine` (`038`, not built) | 7 named steps | Yes, by its text |
| `082` scene (not built) | "Each step starts, updates each tick and says when it is done"; `parallel`; `onEnd(skipped)` | It **is** this type, with 2 named callers |
| `GauntletMaster` | 5 states, a loop, resets from 2 events (`:60,:168-176`) | **No.** Keep an FSM, and host it in one step. Its outer shape fits a `Fight`. |
| `Gate` | 5 states with timers | No need |
| `WilyFinalBoss.PhaseTransitionHandler` | 5 states, local | No need |

**Verdict: a shared type is not speculative.** `082` already specifies the same type, so two runners would be a real duplicate.

The limits:
- One type: `Step<C>` and `Routine<C>`, in `engine` next to `state`, with a generic context.
- No loop and no branch in the type. A loop is an FSM in a step (`015`). A branch is a plain `if` in a step, as `082` says.
- `parallel` waits for its first caller.
- No forced migration. `PlayerSpawnEventHandler` and `EndLevelEventHandler` move only when `038` or `013` reaches them.

**Does `013` own the fight as a state? No.** The `013` notes sketch `PLAYING -> BOSS_INTRO -> BOSS_HEALTH_FILL -> BOSS_FIGHT -> BOSS_DEFEATED -> VICTORY`. That is the fixed chain of F4 in a new form.
- It blocks a fight during a room transition. This is a `013` open question, and the stress list says yes.
- It blocks 2 fights, and a fight with no freeze.
- It adds boss states to modes with no boss. This is also a `013` open question.

`013` keeps `PLAYING`, `ROOM_TRANSITION`, `VICTORY`, `DYING`, `SPAWNING`. A fight runs inside `PLAYING` and across transitions. `013` calls `fights.cancelAll()` on the level reset. A fight calls the victory input in place of `VICTORY_EVENT`.

#### 5. Recommendation

Use **Design D** in a new item `118`.

##### Mapping of the 18 bosses

Presets, all from today's facts:
- `stageBoss` is `mini=false, end=true`: 11 fights
- `midBoss` is `mini=false, end=false`: 8 fights
- `miniBoss` is `mini=true`: 5 fights
- `finalBoss` is `WilyFinalBoss`: 1 fight

Total 25 live fights.

| Boss | Live fights | Preset | Ready wait | Music | Orbs | Steps or moves after |
|---|---|---|---|---|---|---|
| `BigAssMaverickRobot` | 1 | stage | Fall target and hands `ROTATE` | `VINNYZ_INTRO_BOSS_MUSIC_INTRO` | Yes, body center | `preReady` shake stays in the boss |
| `TimberWoman` | 2 | stage, mid | `INIT` timer | Default | Yes | None |
| `MoonMan` | 2 | stage, mid | Timer | Default | Yes | `onDefeated` cleanup |
| `RodentMan` | 2 | stage, mid | Timer | Default | Yes | Gravity on at ready |
| `DesertMan` | 2 | stage, mid | Timer | Default | Yes | Gravity on at ready |
| `InfernoMan` | 2 | stage, mid | Timer | Default | Yes | Gravity on at ready |
| `ReactorMan` | 2 | stage, mid | Timer | Default | Yes | None |
| `GlacierMan` | 2 | stage, mid | Timer | Map value equals default | Yes | Gravity on at ready |
| `PreciousWoman` | 2 | stage, mid | Timer | Default | Yes | Gravity on; `onPlayerDamaged` reaction stays |
| `ElecDevil` | 1 | stage | Timer | `JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC` | Yes, active body | `onReady` setup; position through a bar or orb source |
| `GutsTank` | 1 | stage | Players landed and timer | `MMX6_SIGMA_2_BATTLE_INTRO_MUSIC` | Yes | Landing check uses present players (`047e`) |
| `WilyFinalBoss` | 1 | final | `initSequence` flag | `OUR_BLOOD_BOILS_8_BIT`, only if no music plays | No | No exit; end scene from `onDestroy` moves to `outro` steps; phases stay |
| `MoonHead` | 1 | mini | True | None | No | Spawner cleanup |
| `MechaDragon` | 1 | mini | Timer | None | No | None |
| `ReactorMonkey` | 3 | mini | True | None | No | Heart tank reward |
| `Bospider` | 0 | mini on old maps | `mini || timer` | Forced at `onReady` too | By mode | `:202` double music on no live map; `mini` read goes |
| `SigmaRat` | 0 | stage on old maps | True (`TODO`) | Map | Yes | None |
| `PenguinMiniBoss` | 0 | mini on old maps | True (`TODO`) | None | No | None |

`Fight` holds `cast` of size 1 for all 18. Only `SigmaRat`, `GutsTank`, `Bospider` and others have child entities, and they stay owned by the boss.

Step lists:

```kotlin
fun stageBoss(music) = FightPlan(
    intro = steps(SpawnCast, AwaitReady, HoldWorld, FillBar, ReleaseWorld, ControllerOn, BossMusic),
    win = AllDefeated(), lose = null,
    outro = steps(DefeatLock, NotifyListeners, HideBar, AwaitDefeatEnd, LevelVictory)
)
// midBoss:  last step is ResumeFight: controller on, unlock, level music, gate unlock
// miniBoss: intro = SpawnCast, AwaitReady, ControllerOn
//           outro = NotifyListeners, AwaitDefeatEnd, gate unlock
```

##### First branch: `mega-refactor-118a-fight-routine`

**Scope:**
- Move the 6 branches at `MegaLevelScreen.kt:691-804` into steps and presets. The screen keeps 1 `start` call and 1 `update` call. Tick goes before the `when` at `:920`. The fill stays first in that `when`.
- `FightPlans.resolve(bossName, mapProps)` is the single source, called before spawn (fact 5). It writes resolved values to `spawnProps`.
- Remove the 12 forced lines of F2.
- Remove `BOSS_READY`, `BOSS_DEAD`, `BEGIN_BOSS_SPAWN`. No listener outside the screen and the boss uses them.
- Keep `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `INTERMEDIATE_BOSS_DEAD` for `AbstractBoss`, `Spike`, `PreciousGem` and `Gate`.
- Keep the 14 hooks, `isReady`, and `betweenReadyAndEndBossSpawnEvent`.

**Why no behavior changes:** a step in the tick of frame F+1 equals a handler in `events.run` of frame F. No system runs between them, and a submit from the tick reaches the same run as a submit during `run` (`EventsManager.kt:30`). The freeze, release and controller frames stay the same.

**Intended changes for the contract:**
- `wily_room` no longer checks `isLevelDefeated` (fact 14).
- An event in the frame just before a pause now waits for resume, because a tick does not run in a pause and `events.run` does. A pause is blocked in most of this flow (inference).

**New APIs:**

| API | First real caller | Branch |
|---|---|---|
| `Step<C>`, `Routine<C>` | `Fight` | `118a` |
| `Fight`, `FightPlan` | `FightDirector` | `118a` |
| `FightDirector` (on `MegaServices`) | `MegaLevelScreen` at `ENTER_BOSS_ROOM` and in `render` | `118a` |
| `FightPlans.resolve` and 3 presets | All 25 live fights | `118a` |
| `AwaitReady`, `HoldWorld`, `FillBar`, `ReleaseWorld`, `ControllerOn`, `DefeatLock`, `NotifyListeners`, `HideBar`, `AwaitDefeatEnd`, `LevelVictory`, `ResumeFight` | Presets | `118a` |
| `Lease` | `HoldWorld`, `FillBar` | `118a` |
| `FightRule.AllDefeated` | Presets | `118a` |
| `HudBars`, `BarSpec`, `BarSource` | `FillBar` replaces `BossHealthHandler`; delete `HealthFillType` | `118b` |
| `finalBoss` preset | `WilyFinalBoss` | `118c` |
| `BossComponent` role data | Replaces `ready`, `betweenReady...`, `onReady` | `118c` (shared with `048` step 6) |
| Key-based gate unlock | `GauntletMaster` and bosses (`Gate.kt:196-226`) | `118d` |

**No production caller today. Do not build these until a caller exists:**

| API | First caller | Status |
|---|---|---|
| `say` step, `parallel`, `Routine.skip` | `082a`, `082b` | Item exists |
| Second bar, group `BarSource` | Not named | **None** |
| `setHealth` step, `FightMemory` | Not named | **None** |
| `lockArena`, `lockCamera`, `followRooms` | Not named | **None** |
| `swapCast`, `nextFight` | Not named | **None** |
| `Survive`, `AnyOf`, `Fled` rules | Not named; maybe `108` | **None** |
| `waves` step | `GauntletMaster`, if the owner wants it | **None** |
| `restorePlayer` step | Hostile P2 fight | **None**; needs `030`, `047` |
| Present-player steps | `047e` | Item exists |

##### Order

1. `118a`. It needs none of `012`, `013`, `015`, `030`, `048`, and it can start now.
2. `012` in either order. If `012` comes first, `HoldWorld` calls `FreezeController`. If `118a` comes first, the system writes at `:743-745,:753` move to one file, and `012` changes that file only.
3. `082a` after `012`. Whichever of `082a` and `118a` comes first adds `Step`, and the other reuses it. `082b` becomes a `say` step in a plan, in place of a hook.
4. `118b`, then `118c`.
5. `013` after `015` and after `118a`, with its boss states removed.
6. `048` step 6 after `118c`. It then moves only what remains. `048` keeps the shared death path (F13).
7. `118d`.
8. `015` is not a dependency. `GauntletMaster` and boss AI use it as they do today.
9. `047e` after `118a`. Hostile P2 after `030`, `047`, `109` and the `102` decision.

#### 6. Behavior check for the classic game

No test covers boss timing. `MegaLevelScreenEventsTest` covers only `GAME_COMPLETE`. `HeadlessHarness` only builds entities and runs `init()`. A `grep` for boss flow in `core/src/test` finds no timing test.

| Layer | What | When possible | Proves |
|---|---|---|---|
| 1 | **Temporary contract suite**, by the rule in `docs/mega-refactor/CLAUDE.md`. Frozen copy of the 6 `when` branches in test sources. Run it and `Fight` on the same scripted frames, with real `EventsManager`, mock audio, a fake systems list and a mock `AbstractBoss`. Compare an ordered trace per frame: systems state, controller, audio calls, events, bar state. | `118a` | Order and frame of each action |
| 2 | **Matrix** for layer 1: 3 presets; `Wily` `null` exit; level defeated skip; slot 0 dead at defeat and at dead; `PLAYER_SPAWN` during intro; gate unlock. | `118a` | All branches at `:691-804` |
| 3 | **Permanent suite:** `Routine` order, each lease released on interrupt, one call to `resolve` per fight. | `118a` | Rules after the merge |
| 4 | **Ordered log diff.** The screen logs each event (`:509`), and `AbstractBoss` logs its steps. Compare old and new log order for one play of each preset. | `118a` | Same order in the real game; not frame-exact, because delta varies |
| 5 | **Owner play test:** `RefactorTest118.tmx` with a `Test` boss, experimental rooms first; then control fights. Controls: one fight per preset, `BigAssMaverickRobot`, `ElecDevil`, `GutsTank`, `WilyFinalBoss`. Commands name the room before the boss room. | Each branch | Look, sound, feel |
| 6 | `005` adds boss rows for `ready` and `defeated` states. This is its open question. | Before `118c` | Damage window stays |
| 7 | `006`: a real level run to a boss. `055`: frame replay. `055` needs `010`, `011`, `006`, `031`. | Later | Boss AI frames; **needed for** removing `betweenReady...` to be exact |

Do not wait for `055` for `118a`. Layers 1 to 5 are enough, because `118a` changes no boss AI.

`118c` changes AI gating by up to 1 or 2 frames for 10 files, so its contract must either keep the same window or name the change.

**Note on layer 1:** driving the real `onEvent` is weaker. `BOSS_READY` needs `bossHealthHandler`, which only `init()` sets (`:284`), and `init()` with a relaxed mock may throw (inference, not tried). So I recommend the frozen copy.

#### Not read in full

- `WilyFinalBoss.kt`: read `:225-440`, `:800-830`, `:3247-3392`, plus greps. The 3 phase handlers were not read.
- Other bosses: read the spawn, ready, defeat and update head. AI bodies were not read.
- `Megaman.kt`: only boss, event and lock lines.
- `MegamanMaverickGame.kt`: `render` and the controller events.
- Notes read by grep only: `023`, `042`, `048`, `050`, `052`, `102`, `109`, `082`.
- `017` `ITEM.md` was read; its notes were not.
- `036` `NOTES.md`, `029` `NOTES.md`, `031` `NOTES.md`: named sections only.
- I did not run anything except read commands. The frame equivalence in section 5 is from reading, so layer 1 must prove it.

#### Direct answer on the existing item

No existing item covers this redesign. `013`, `048`, `082`, `042` and `047e` overlap in parts. Add a new item `118`, and edit those scopes.

#### Recommendation

Build Design D: `Fight` plus one `Routine`, rule values, and services with leases. Ship `118a` first. All 25 live fights are its real callers. It moves 114 lines out of the screen, and it ends the 2-source facts. Everything in the "no production caller" table stays in the docs until a caller exists.

#### Risks

1. **Frame equivalence** is by reading. A wrong case changes when the controller returns. Layer 1 covers it.
2. **Tick place.** `FightDirector` runs in the screen until `010`. It must move with the simulation, or `O2-R10` stays broken.
3. **Fill in the `when`.** The fill must keep blocking the other 3 handlers until `013`.
4. **Two sequence owners** if `082a` and `118a` run in parallel. Decide one owner for `Step` first.
5. **Hooks stay in `118a`**, so the code has 2 flow shapes until `118c`. `AbstractBoss` is not simpler after the first branch.
6. **Step growth.** A step list can turn into a scripting language. The no-loop, no-branch rule must hold.
7. **Stale map key meaning.** `mini` and `end` stay as legacy keys. 58 TMX files have `event=boss`. Keep the keys until `022`.
8. **`118c` frame change** in 10 files needs `006` or a named contract change.
9. **`Wily` end scene** in `onDestroy` (`:292-311`) leaves the controller off, and no step ends it on a death path. `082` already names this path. Keep it unchanged in `118a`, and fix it with `finalBoss`.

#### Open questions for the owner

1. **Is `Fight` a plain object, not an entity?** This differs from the earlier "invisible fight entity" idea. Recommendation: plain object, because of fact 4. `Fight` still has the lifetime of the fight, and `BossComponent` stays on participants.
2. **New item `118`, and edits to `013`, `048`, `082`, `042`?** Recommendation: yes.
3. **Remove boss states from `013`?** Recommendation: yes, and treat the overlap question as answered "a fight and a transition can overlap".
4. **Who adds `Step`: `118a` or `082a`?** Recommendation: `118a` first, because it needs neither `012` nor `031`.
5. **Merge target?** Recommendation: directly into `master`, because each branch ships alone.
6. **Remove the `isLevelDefeated` check for `wily_room`?** Recommendation: yes, as a named intended change.
7. **What is a mid-level boss's rematch rule?** Today a re-entry spawns again (inference). Recommendation: keep it in `118a`, and decide when `FightMemory` gets a caller.
8. **Which variation ships first after the base?** This names the first real caller for the second table. Recommendation: the `082b` talk, because that item is already agreed.
9. **Must `GauntletMaster` become a `Fight`?** Recommendation: no. Share only the key-based gate unlock in `118d`.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/stats/BossHealthHandler.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/screens/levels/LevelRoomHandler.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/sensors/Gate.kt
