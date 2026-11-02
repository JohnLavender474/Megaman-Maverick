# Notes: 122 Human-driven boss

Research for this item. Each report is kept in full, with its own date and `master` commit. A note above each report says what later research or an owner decision replaced. If a report and `ITEM.md` disagree, `ITEM.md` wins.

## Agent report: Playable bosses and swappable characters (2026-11-02)

Note: The "fight entity" here is the plain `Fight` of `118`.

### Playable bosses and swappable characters: design research

Base: `master` at `b3e792bc4`. Commit `44a4c7e61` moved to this commit during the research, with docs only, so the code did not change. I changed no files.

Paths: `core/` means `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/`. `engine/` means `/home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/`. Citations use the file name, and full paths are at the end.

Counts come from `grep` and a read-only script. They can hold a few false hits. Items marked "Inference" are my reasoning, not measured facts.

#### 0. Claims checked

| Claim | Result |
|---|---|
| 18 direct `AbstractBoss` subclasses | True. There are 21 `grep` hits, and 3 are commented out (`MechaDragon_OLD`, `ReactorMan_OLD`, `SphinxMiniBoss`). `ReactorMonkey` is in the 18. `048` `NOTES.md` says 17, which is 1 too low. |
| Only `Megaman` has a `ControllerComponent` | True. The only core uses are `Megaman.kt`, its component file and `MegaPlayers.kt`. `BehaviorsComponent` also has `Megaman` as its only entity user. |
| `ControllerComponent.input` is a `var` | True (`engine/controller/ControllerComponent.kt`). `MegaPlayers.kt:31` sets it. |
| `031` advises against converting bosses to input | True (`031` `NOTES.md:1014-1016`). The reason is sound, but it applies only to button-level input. See section 2. |
| `Megaman` reads input about 44 times per frame | Not exact. 44 is a count of read sites (`NOTES.md:712`), and line 1004 words it as "per frame". `grep` now gives 46 sites in `entities/megaman/`. A frame does not run all sites, because the conditions short-circuit. I did not measure a runtime count. It has no effect on this decision. |

#### 1. How boss attacks are driven today

The 18 classes hold 15,218 lines. 7 part files add 1,959 lines. The 18 classes have 168 `Timer(` calls, 74 random-call matches and 118 difficulty-check matches.

| Boss | Lines (code) | Driver | States | Body, tiles | Player reads | Update + `onChangeState` lines | Parts |
|---|---|---|---|---|---|---|---|
| `PreciousWoman` | 988 (776) | `StateMachine` | 9 | dynamic 1x2 | 6 | 129+99 | shield gems |
| `ReactorMan` | 846 (680) | `StateMachine` | 10 | dynamic 1.5x2 | 4 | 129+48 | map beamers |
| `RodentMan` | 966 (784) | `StateMachine` | 5 | dynamic | 5 | 136+57 | `RatRobot` children |
| `TimberWoman` | 1214 (971) | `StateMachine` | 10 | dynamic 1.5x1.75 | 9 | 114+101 | map leaves, walls, ground |
| `DesertMan` | 842 (706) | `StateMachine` | 7 | dynamic 1.5x1.75 | 9 | 122+122 | map walls |
| `GlacierMan` | 711 (580) | `StateMachine` | 9 | dynamic 1.25x1.75 | 8 | 88+39 | map walls |
| `InfernoMan` | 777 (634) | `StateMachine` | 5 | dynamic 1.5x1.75 | 7 | 84+55 | map collide object |
| `MoonMan` | 731 (610) | `StateMachine` | 5 | dynamic 1.25x1.75 | 7 | 131+37 | writes world gravity |
| `PenguinMiniBoss` | 247 (214) | enum, timers | 3 | abstract 2.5x3 | 1 | 54 | `BabyPenguin` |
| `ReactorMonkey` | 326 (276) | enum, timers | 2 | abstract 3.5x5 | 3 | 51 | balls |
| `MoonHead` | 402 (335) | `Loop` | 6 | abstract 3 | 4 | 74 | asteroid spawner |
| `Bospider` | 460 (378) | `Loop` | 5 | abstract 2.5 | 0 | 128 | webs |
| `MechaDragon` | 812 (661) | `StateMachine` | 5 | abstract 2x5 | 1 | 142+57 | 24 map properties in `InfernoMan_16x14_v3` |
| `BigAssMaverickRobot` | 654 (548) | timers only | 0, hand has 6 | abstract 8x12 | 6 | 62 | `Body` 78 lines, `Hand` 442 lines |
| `GutsTank` | 689 (579) | enums, timers | 2+5 | abstract 12x9 | 4 | 249 | `GutsTankFist` 310 lines, tank block, killer wall |
| `SigmaRat` | 469 (391) | enum, timers | 4 | abstract 7.5 | 9 | 28 | `SigmaRatClaw` 357 lines |
| `ElecDevil` | 692 (543) | `Loop` | 6 | abstract 1.5 | 2 | 148 | 3 files, 772 lines |
| `WilyFinalBoss` | 3392 (2744) | 4 `StateMachine`s in phase handlers | 3 phases, 24 states, 12 sub-states | abstract 8x4 | 14 | 129+207 | tentacles and others |

Notes on the table:

- "Player reads" counts `targetPlayer` and `players.` lines, without imports or comments. The total is 99 lines. 17 of 18 bosses read a player; `Bospider` does not. The part files in `bosses/` add 6 more lines.
- "Lines (code)" counts lines that are not blank or comments.
- `Bospider`, `PenguinMiniBoss` and `SigmaRat` have 0 objects with their name in the 24 live maps. They are not in play now.
- All other bosses sit in rooms of 16x14 tiles.

**Pattern.** The 8 robot masters all use one form:

- A guard picks the next state with target reads and random numbers. Example: `GlacierMan.kt:470-479`.
- A `when (currentState)` block in the updatable runs timers and writes exact velocities. Example: `GlacierMan.kt:254-323`.
- `onChangeState` does the entry and exit work.
- The animation key comes from the state (`GlacierMan.kt:426-432`).
- Target reads also sit inside moves (`GlacierMan.kt:269,286`). By a rough sort on function names, 27 of the 99 read lines are in act functions such as `jump` and `shootStar`. The other 72 are in decisions or state code.

**`BigAssMaverickRobot`** has no state machine. Its body moves only in the intro fall (`BigAssMaverickRobot.kt:228-230,296-299`). Its whole brain is about 40 lines at `BigAssMaverickRobot.kt:474-512`. Its acts are 2 calls: `Hand.launch()` (`BigAssMaverickRobotHand.kt:239`) and the `shootOrb` timer (`BigAssMaverickRobot.kt:607`). It reads map objects for hands, arm origins and the fall target (`BigAssMaverickRobot.kt:222,325-362`). Its room is `IntroStage_v3` `boss_room`, and the boss object has 11 properties.

**Groups, in order of input conversion.** The facts are measured. The order is inference.

1. **Ground fighters with a `RUN` state (4):** `PreciousWoman`, `ReactorMan`, `RodentMan`, `TimberWoman`. They need guards replaced, and `facing` and aim taken from input. A free move already exists.
2. **Ground fighters with no walk (4):** `DesertMan`, `GlacierMan`, `InfernoMan`, `MoonMan`. They have the same needs as group 1, plus a new walk move and art. The only free moves are `JUMP`, or `SLED` for `GlacierMan`. `MoonMan` also writes global gravity (`MoonMan.kt:526`), which acts on all players.
3. **Anchored single bodies (5):** `PenguinMiniBoss`, `ReactorMonkey`, `MoonHead`, `Bospider`, `MechaDragon`. Buttons map well to a fixed attack list. They have no free move, so there is no character to steer.
4. **Multi-entity set pieces (5):** `BigAssMaverickRobot`, `GutsTank`, `SigmaRat`, `ElecDevil`, `WilyFinalBoss`. They are bound to map objects and fill a 16x14 room. `BigAssMaverickRobot` covers 96 of 224 tiles and `GutsTank` covers 108. They have no fighter form. A button map is possible only as "attack from a fixed place". That is simplest for `BigAssMaverickRobot`, and I do not advise it for `WilyFinalBoss`.

#### 2. The real boundary

**The boundary is inside each boss, and it has 4 parts:**

| Part | Today | Needed for play |
|---|---|---|
| Intent: which move next | Guards, random calls, target reads | AI or input picks the move |
| Move: what the move does | State bodies, exact velocities, spawns | Stays. AI and player use the same move, as the owner wants |
| Steering and aim | `facing` and shot aim from `targetPlayer` inside moves | Comes from a source that is not `targetPlayer` |
| Presentation | Animation key from the state | Stays, if the state machine stays |

**Direct answers:**

- **Does moving the fight state to a parent entity make a boss more playable? No.** It adds no controls and no moves. It removes one blocker only: the fight flow. That flow is:
  - `ready` gates `canDamage` (`AbstractBoss.kt:178`).
  - `PLAYER_SPAWN` destroys the boss (`AbstractBoss.kt:174`).
  - The cull events run (`AbstractEnemy.kt:238`).
  - Defeat locks all players and ends the level (`MegaLevelScreen.kt:756-795`).
- **Does a parent swap help control? No.** The swap point exists already: one `var` on `ControllerComponent`. A brain needs a field or a source, not an entity with a pool and a lifecycle.
- **A parent cannot be the player.** `Players.register` puts `PlayerComponent` on the entity. `.asPlayer` has 155 lines in 66 files, and `targetPlayer` has 591 lines in 142 files. They need a body (`MegaPlayers.isLive`, `targetFor`). So the fighter must hold the slot.
- **The fight entity keeps its value for one case:** a hostile `Megaman` or X, because that fighter must have no boss class. This agrees with the earlier research. `GauntletMaster` is a precedent for an invisible flow entity. It has no control logic.

**Button level against move level.** There are 2 levels of "input", and the `031` advice concerns only the first.

- **Button level:** a CPU presses `LEFT` and `A`. This fits characters built on behaviors (`Megaman`, X). It does not fit existing bosses, because their moves set exact velocities.
- **Move level:** the state machine stays, and only the guard changes, from a random check to "the brain asks for this move". Existing moves stay the same. For a human, a button maps to a move request.

#### 3. What a playable boss lacks

**Two tiers.** This is the main finding.

- **Tier 1, human brain:** the boss stays a boss and is not registered as a player. It reads a slot frame. `PlayerInputs` holds frames by slot with no entity (`PlayerInputs.kt`), and slot 1 gets a frame when `launch.players > 1` (`MegamanMaverickGame.kt:321,583`). The same launch value also registers a `Megaman` in slot 1 (`:448-456`), so a mode must skip that step. With one registered player, `targetPlayer`, the camera, the HUD, the boss bar, damage and the room all stay as they are. Most of the table below does not apply to this tier (inference).
- **Tier 2, boss in a slot:** the boss is registered, and the whole table applies.

| Need | State for tier 2 | Evidence |
|---|---|---|
| Control mapping | None on bosses. 8 buttons exist. `O9-R1` limits moves to an NES-style pad | `MegaControllerButton.kt:7-15` |
| Required components | `lock` and `setPlayPhase` throw if the part is missing, and spawn calls them for all players | `Locks.kt:6`, `PlayPhases.kt:12`, `PlayerSpawnEventHandler.kt:126-131` |
| Fixture properties | 35 reader lines in 15 files. Only `Megaman` puts `FEET`, `HEAD` and `DAMAGEABLE` on its body. A boss in a slot fails at the cast (inference) | `EntityQueries.kt:45-61`, `BodyComponent.kt:140,188,222`, `BigAssMaverickRobotHand.kt:248` |
| Targeting | `targetFor` includes the seeker, so a boss in a slot targets itself at distance 0 when 2 players are live | `MegaPlayers.kt:39-50`, `112` |
| Camera | Follows slot 0 only. `focus` throws with no `FocusComponent` | `MegaLevelScreen.kt:337`, `EntityQueries.kt:16` |
| HUD | Slot 0 only. Slot 0 must be `Megaman`, or the casts throw | `PlayerStatsHandler.kt:58,106,115` |
| Weapon | 1,289-line handler typed to `Megaman`, 258 lines name it. 31 files name `MegamanWeapon` | `MegamanWeaponsHandler.kt`, `024` |
| Lifecycle | `PLAYER_SPAWN` destroys the boss. Slot 0 death destroys other slots. The boss spawn needs map objects, and the player spawn gives a `player` layer object | `AbstractBoss.kt:174`, `MegaLevelScreen.kt:549-552,588` |
| Damage rules | `friendlyFire` is read only in `Megaman.kt:436`, `ProjectileRules.kt:29` and `PreciousGem.kt:208`. `AbstractEnemy.canBeDamagedBy` accepts any player-owned shot, so in co-op a boss in a slot takes teammate damage (inference) | `AbstractEnemy.kt`, `DamageRule.kt` |
| Boss against boss | An unlisted damager deals 0. `BOSS_DMG_NEG` has 15 classes, and `BigAssMaverickRobot` has 3. Most boss shots deal 0 to another boss | `AbstractBoss.kt:62-91`, `BigAssMaverickRobot.kt:148-160` |
| Balance | Boss 30 health and 1.25 s hit time. `Megaman` 14 start health and 0.5 s hit time. Boss shots deal 2 to 4 to `Megaman`, and his shots deal 1 or 2 to most bosses. 118 difficulty matches tune the AI | `AbstractBoss.kt:57,227`, `MegamanValues.kt:8,79`, `MegamanDamageNegotiator.kt` |
| Room and body | All 16x14 rooms. `109` wants a large room. No arena exists, and map data binds most bosses to their room | Section 1 |
| Registration | `register` refuses an occupied slot. `unregister` has only test callers and does not unbind input | `Players.kt`, `MegaPlayers.kt:29-31`, `O1` audit |

**`Megaman`-typed sites:**

- 10 cast lines in 7 files: 9 hard casts and 1 safe cast (`EntityQueries.kt:43`). The hard casts are `MegamanMaverickGame.kt:822`, `EndLevelEventHandler.kt:173`, `GetWeaponScreen.kt:153,195`, `MegaLevelScreen.kt:624`, `LevelPauseScreen.kt:286,569`, `PlayerStatsHandler.kt:106,115`. The `O8` audit says 14, so its row needs a check.
- 14 `Megaman::class` lines in 10 files, for example `StandardDamageNegotiator.kt:50`, `SwinginJoe.kt:219`, `JoeBall.kt:151`.
- 0 real `is Megaman` lines. The 2 `grep` hits are comments.
- 49 `fun Megaman.` and 6 `val Megaman.` lines. The 8 behavior files (926 lines) have a `Megaman` receiver.
- 31 files outside the package import `Megaman`, and 88 import from `entities.megaman`.
- 7 getters fix a screen or handler to `PlayerSlot(0)`. 18 lines name slot 0.
- 48 lines match `getDamageToMegaman` or `isMegaman*`. Most of these are names only.

**The other direction: `Megaman` as a boss.** This is close to possible today.

- A CPU source fits `InputSource`. It can give held bits to an `InputFrameBuilder`, so the edge rules stay the same (`InputFrameBuilder.kt`, `MegamanMaverickGame.kt:582-583`).
- An unregistered `Megaman` fails at 6 `playerSlot!!` sites (`Megaman.kt:364,365,374`, `ControllerComponent.kt:115`, `AirDashBehavior.kt:38,43`) and at `asPlayer!!` (`Megaman.kt:456`).
- So a hostile `Megaman` must use a slot, or those sites must change. It also needs `030` for sides, and the fight entity with `BossComponent`.
- X follows through `CharacterDef` (`081`).

#### 4. Options

| Option | Facts | Verdict |
|---|---|---|
| (a) Refactor all bosses | 18 classes, 15,218 lines, plus 1,959 part lines. 99 player reads and 74 random matches. Groups 3 and 4 have no fighter form after the refactor. 3 bosses are not in play. No mode exists: `102` and `109` are `idea`, and there is no arena. No test checks boss timing, `006` and `055` are not done, and random is not seeded. The classic game must not change. It fails `O1-R5`, because 18 conversions have no caller | **Not advisable** |
| (b) Command layer for new characters only | No new layer is needed. `InputFrame`, `InputSource` and the `var` exist. Add a CPU source only when the first caller exists (hostile `Megaman`, Proto Man CPU). It gives "Mega and X can be player or boss" and uses the same moves. It gives no playable boss | **Advisable** |
| (c) Limited set | Tier 1 with `BigAssMaverickRobot` first. Its brain is about 40 lines, it has 2 acts and it does not walk. Its room already suits the fight. It needs no `112`, `030`, `023` or `047` work (inference). It does need a mode that skips the slot 1 `Megaman`. Group 1 is next, if the play test is good | **Advisable, with a named mode as caller** |
| (d) No change | No risk to the classic game. The owner's example is not possible | Right for each boss with no mode |

**Limit of tier 1.** It gives Mega against a human-driven boss in that boss's own room. This matches the owner's example, but it is asymmetric. It is not the team deathmatch that `109` defines. Boss against boss, or a boss in an arena, needs tier 2.

#### 5. Items and order

`ITEMS.md` has no item for a playable boss. The topic is only in `031` `NOTES.md:887-1032`, with pointers in `029` and `030`. An item for tier 1 is needed, with objectives `O8` and `O2`, and the owner must approve it.

**Tier 1 steps:**

1. The owner names the mode.
2. A new item adds a launch path: slot 1 has a frame, and no `Megaman` registers there. `BigAssMaverickRobot` splits its brain at `:474-512` into `launchHand(...)` and `startOrbs()`. AI and human use these calls.
3. The owner plays it. Stop here if it is not fun.
4. A second caller from group 1, for example `RodentMan` (5 states). A shared type starts only at this step. Before this step, no shared API has 2 callers.

**Character path (option b):** `081` → `CharacterDef` → the first hostile character adds the CPU source. This hostile case also needs `030` and `048` step 6 (`BossComponent`).

**Tier 2 order, only if the owner wants it:** `047c`, `047d`, `047e` with the first `112` branch → `030` → `023`, `024` → `081` → `048` steps 5 and 6 → `102` → `109` → the first boss in a slot.

**Overlap:**

- `048` step 7, `015` and `042` (`LimbMotion` on the hand and fist files) all edit the same boss classes. Do the intent split for a boss in the same pass as its `048` batch, to avoid a redo.
- `015` must let a guard read an intent. This fits its proposed `transition` form.

#### Direct answer

Making every boss playable by swapping a parent entity is **not advisable**, and the parent swap is **the wrong tool**.

- It is feasible in part, but the parent gives no controls. The work is a split of intent from moves inside each boss, and the fighter itself must hold the slot.
- A fight entity is correct for a hostile `Megaman` or X, and for fight data. It is not a control layer.
- Mega and X as boss or player is feasible with the input system that exists now, plus `030` and the fight entity.
- A human-driven boss is feasible today for your `BigAssMaverickRobot` example, without any player registration.

#### Recommendation

1. Reject (a).
2. Keep the `048` plan for `BossComponent`.
3. Take (b) at the first real caller.
4. Take (c) tier 1 with `BigAssMaverickRobot` as a play test.
5. Leave all other bosses as they are until a mode asks for them.

#### Risks

- **Classic game drift:** a brain split changes boss timing, and no automated check catches this. Each boss needs an owner play check.
- **Fun is unknown (inference):** bosses are balanced for one human target, not for a human pilot.
- **Redo:** `048`, `015` and `042` touch the same files.
- **Scope creep from tier 1 to tier 2:** tier 2 pulls in the full table in section 3.
- **Aim:** `Hand.launch` aims at `targetPlayer` (`BigAssMaverickRobotHand.kt:242`). Automatic aim may be too strong, and a human aim needs a new control.
- **Round end in tier 1:** slot 0 death resets the level (`MegaLevelScreen.kt:579-588`), and the boss has no round rule for "boss wins".
- **`O9-R1`:** moves must fit an NES-style pad.

#### Open questions for the owner

1. Is an asymmetric "Mega against a human-driven boss in its own room" enough at first? I recommend yes.
2. Is this a sub-mode of `109` or a new item? I recommend a new item, because `109` is team-based.
3. Does a human boss aim, or does it keep automatic aim? I recommend automatic aim first, then a test.
4. Must a playable boss walk freely? This decides group 2, and it means new art. I recommend no, and to start only with group 1 if needed.
5. Is boss against boss a goal? It needs `023`. I recommend not now.
6. Must a hostile `Megaman` use a player slot? I recommend yes for the first version, because 6 sites and the cast need a slot.

#### Not read in full

- Read in full: `AbstractBoss.kt`, `Megaman.kt`, the engine controller, input and players files, `MegaPlayers.kt`, `SessionRules.kt`.
- Read in part: `BigAssMaverickRobot.kt` (lines 78-557), `GlacierMan.kt` (lines 66-485), `MegaLevelScreen.kt`, `GauntletMaster.kt`, `AbstractEnemy.kt`.
- Measured by script or `grep` only: the other 16 bosses. This includes `WilyFinalBoss.kt`. `MegamanWeaponsHandler.kt` was also only measured.
- `029` `NOTES.md` and `031` `NOTES.md`: only the sections I cited.
- `RodentMan` body size is set at runtime (`RodentMan.kt:543`), so I give no value.
- The act and decision split of the 99 reads is a name-based sort.

##### Critical Files for Implementation

- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/BigAssMaverickRobot.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/BigAssMaverickRobotHand.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt
