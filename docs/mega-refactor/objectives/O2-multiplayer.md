# `O2`: Multiplayer

- Status: active

## Problem

- `P1`: The game was built for one player. Two people cannot play together, and logic that assumes one player makes
  each multiplayer step harder.
- `P2`: Split screen needs more than one view of a session, but gameplay code reads one game camera. Spawn and cull
  would act only for one player's view.
- `P3`: Lockstep netplay sends only inputs, and each machine runs its own simulation. Today the same inputs can give
  different results, because the physics step, the frame delta, random values and async pathfinding can differ. The
  two games then separate: for example, an enemy is dead on one machine and alive on the other.
- `P4`: The owner cannot see if current work helps netplay, or if a later phase will force a redo.

## Goal

Two dedicated multiplayer modes, separate from the main game: Spec Ops co-op missions (`108`) and PvP (`109`). These are
the crux of this objective, and they are the only two named multiplayer modes (owner, 2026-11-01). Players play them on
one machine or on the net. The technology under them is local play where two or more players share one screen, split
screen and netplay. From the owner: "Our design should be able to handle all of these."

Co-op in the main campaign is optional, and it comes after these modes (`110`). From the owner: "Redesigning the
campaign with co-op in mind is a huge undertaking, while having dedicated modes for multiplayer makes the scope much
more manageable".

## Phases

Each phase ships alone (owner, 2026-11-01). The phases are technical steps, and the two modes are their content.

1. **Phase 1:** two players on one machine, on one shared screen, with no split screen. Item `047`. This is the first
   target. Each player has their own input device.
2. **Phase 2:** split screen. Both players stay in the same room.
3. **Phase 3:** netplay with lockstep. Players are fixed at session start. Settings that change gameplay go to all
   peers at session start. Pause and pause-menu actions are inputs in the frames, so all peers stop together.

The first PvP target is one large room. Multi-room PvP comes after it (`111`).

The design must scale to 4 or more players. In local play, players can join and leave during play. Saves do not
depend on the player count.

## Done means

- Phase 1: two players finish a Spec Ops mission and play a PvP match on one machine, and a second player can join and
  leave during play.
- Phase 2: the same with two views.
- Phase 3: two machines finish a Spec Ops mission and a PvP match with no desync. A check compares the state of both
  machines for each tick.

Co-op in the main campaign (`110`) is not part of "done".

## Rules

A rule's phase is the first phase that needs it. A phase 3 rule still applies to a phase 1 branch, because it keeps
phase 3 possible, but it is never a reason to build phase 3 work early.

### `O2-R1`: Input through frames

- Rule: gameplay reads input only through the `InputFrame` of a slot.
- Problem: `P1`, `P3`
- Phase: 1
- Check: search gameplay code for `Gdx.input`, `controllerPoller` and direct device reads.
- Applies: `always-on`
- Active: `now`

### `O2-R2`: Seeded random

- Rule: gameplay random values come only from the seeded source.
- Problem: `P3`
- Phase: 3
- Check: search `core/src/main` and `engine/src/main` for `MathUtils.random`, `UtilMethods.random`, `Random(`,
  `.random()` and `getRandom`.
- Applies: `always-on`
- Active: after `011`

### `O2-R3`: One fixed tick

- Rule: gameplay runs on one fixed tick in all play, not only in a netplay session. The `MEDIUM` tier is the reference
  for feel. Tiers and auto-performance change only the frame rate and cosmetic budgets. An async result, for example a
  path, applies on a set tick.
- Problem: `P3`
- Phase: 3
- Check: search for `Gdx.graphics.deltaTime`, `fixedStep`, `performance.fps` and `settings.performance` in gameplay
  code. `010` must measure the cost split of simulation and render on a weak machine before it decides the tick.
- Applies: `always-on`
- Active: after `010`

### `O2-R4`: Stable order

- Rule: gameplay does not iterate a hash-ordered collection where the order can change the result. Players iterate in
  slot order. A distance tie goes to the lower slot.
- Problem: `P3`
- Phase: 3
- Check: review the iteration of `ObjectSet`, `ObjectMap`, `HashMap` and `HashSet` in gameplay code.
- Applies: `always-on`
- Active: `now`

### `O2-R5`: No single-player logic

- Rule: no logic assumes one player. Identity is the slot, not the entity. A rule that acts on one player names that
  player.
- Problem: `P1`
- Phase: 1
- Check: search for `first()` and index `0` on players, and for `targetPlayer` where the rule must act on each player.
- Applies: `always-on`
- Active: `now`

### `O2-R6`: No single-view logic

- Rule: gameplay does not depend on one camera. Spawn and cull ask about a set of views. All views of a session are in
  the same room, until `111` lifts this limit for PvP.
- Problem: `P2`
- Phase: 2
- Check: count the direct `gameCamera` reads and the camera overlap helpers in entity code.
- Applies: `always-on`
- Active: after `094`

### `O2-R7`: Simulation is apart from presentation

- Rule: gameplay does not depend on rendering or audio. After `010`, camera state that gameplay reads is a function of
  the simulation.
- Problem: `P3`
- Phase: 3
- Check: search gameplay code for reads of render state, sprite animation state and audio state.
- Applies: `always-on`
- Active: `now`

### `O2-R8`: Local data stays local

- Rule: local-only data (bindings) never enters the simulation. A setting that gameplay reads, for example
  `selectButtonAction`, is shared at session start. A pause-menu action is an input.
- Problem: `P3`
- Phase: 3
- Check: list the settings that gameplay reads, and confirm that each can come from session data.
- Applies: `always-on`
- Active: `now`

### `O2-R9`: Events and progress name a slot

- Rule: an event about one player names its slot. Per-player progress, for example lives, does not live on the entity.
- Problem: `P1`
- Phase: 1
- Check: review the `EventType` values whose payload is about a player.
- Applies: `always-on`
- Active: `now`

### `O2-R10`: Freezes are simulation state

- Rule: a freeze or a respawn delay is simulation state on the tick, not a UI timer that turns systems off.
- Problem: `P3`
- Phase: 3
- Check: search for `system.on` writes and for timers in level screen handlers.
- Applies: `linked items`
- Active: after `012`

Guidance, not a rule: avoid hidden state where that is cheap, so that rollback and late join stay possible upgrades.
Hidden state does not block a branch.

## Current state

Audit of `master` at `1fa0be319`, 2026-11-01. Counts are `grep` line counts, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O2-R1` | `holds` for gameplay | `Gdx.input` is only in `KeyboardInput.kt:35`, `ControllerUtils.kt:10`, `ScreenController.kt:96` and `ControllerSettingsScreen.kt:284`. 0 reads in entities. Entities read `player.input` (`PipePortal.kt:159`). `031f` removed `controllerPoller`, so menus and skip screens read `MegaPlayers.menuInput`. The frame is built once per render, with one `InputFrameBuilder` per slot, and slot 1 has a frame only with `--players 2` (`MegamanMaverickGame.kt:581-582`). | `010`, `047` |
| `O2-R2` | `broken` | The source is clock-seeded (`UtilMethods.kt:22`). `superRandom` makes a new clock-seeded `Random` on each call (`ArrayExtensions.kt:112`, `ObjectSetExtensions.kt:84`). 169 random-call lines in 56 entity files. | `011` |
| `O2-R3` | `broken` | Physics has an accumulator, but the step comes from the tier (`Performance.kt`, `WorldSystem.kt:106-127`), auto-performance changes it during play (`MegamanMaverickGame.kt:559`), and leftover time is dropped (`WorldSystem.kt`, the `maxIterations` branch). Frame delta drives timers (`update(delta)` in 278 entity files). 7 entity lines read `performance.fixedStep`. `Gdx.graphics.deltaTime` is in `GameEntityUtils.kt:171`, `BikerKibbo.kt:143`, `SubmarineJoe.kt:277` and `MegaLevelScreen.kt:286`. Async paths apply when `future.isDone` is true, with a wall-clock timeout (`AsyncPathfindingSystem.kt`). `timeSpawned` is wall-clock time and it sorts weapons (`MegaGameEntity.kt:57`, `MegamanWeaponsHandler.kt:338`). | `010` |
| `O2-R4` | `partly` | The core is ordered: `GameSystem.kt:16`, `GameEntity.kt:13`, `EventsManager.kt:16`, `MegaGameEntities.kt:13-15`, `WorldSystem.kt:91-92`. Players iterate by slot (`MegaPlayers.kt:35`). Hash order remains: `getOfTags` and `getOfTypes` give an `ObjectSet` (`MegaGameEntities.kt:44,54`), used at `TimberWoman.kt:408`, `DarknessV2.kt:345` and `DarknessV4.kt:527`. Each listener reads its events through `eventKeyMask`, a hash-ordered `ObjectSet` (`EventsManager.kt:72`, `IEventListener.kt:7`), so 2 event keys in one frame can arrive in a different order on each run. 34 `ObjectSet` or `ObjectMap` declarations over entity-like types are not reviewed one by one. | `095` |
| `O2-R5` | `partly` | All 616 `targetPlayer` lines (147 files) go through one function, `MegaPlayers.targetFor`. Since `047b`, it gives the nearest live player, and a target can flip between 2 players at almost equal distance (`112`). `047b` made 8 sites act on any live player through `anyLive` and `forEachLive`. 74 `players.any` lines in entities still count a dead slot 1 (`docs/issues/2026-11-02-players-any-counts-dead-slot.md`). Since `047a`, `MegaPlayers` has capacity `MAX_PLAYERS`, which is 2 (`MegamanMaverickGame.kt:238`), and input is built per slot. Most level rules act on slot 0 (`TODO` at `MegaLevelScreen.kt:177`), and the death of slot 0 resets the level (`TODO` at `MegaLevelScreen.kt:579`). The spawn and level-end locks act on all players. `FeetRiseSinkBlock` has one shared state, so two players on one `QuickSand` area cannot get out (`docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`). Known sites have `TODO` comments, for example `AbstractEnemy.kt:99`, `GutsTank.kt:277`. 16 lines use `is Megaman` or `as Megaman`. | `047` |
| `O2-R6` | `broken` (waiting) | One `gameCamera` in `MegaServices.kt:34`. 178 entity files read it directly or through a helper. The helpers carry much of it: `overlapsGameCamera` has 162 entity lines and one body (`IMegaGameEntity.kt:13`), and `getGameCameraCullingLogic` has 2 bodies (`GameEntityUtils.kt:57,60`). 115 lines in 87 entity files read `gameCamera` directly. | `094` |
| `O2-R7` | `partly` | Gameplay reads the camera: cull, spawn and `CameraScroll` deaths. The camera moves in the render path, after `engine.update` (`MegaLevelScreen.kt:908`, `:921`), with a speed from `Gdx.graphics.deltaTime` (`MegaLevelScreen.kt:286`). 17 lines read an animation `isFinished`; most are cosmetic (`SmokePuff.kt:93`), and they are not reviewed one by one. | `010`, `095` |
| `O2-R8` | `partly` | Bindings stay out of the simulation: the device adapters turn them into held masks. `selectButtonAction` is read per slot (`ControllerComponent.kt:114`, `AirDashBehavior.kt:38,43`), so session data can supply it. The pause screen changes gameplay directly, not through inputs: weapon (`LevelPauseScreen.kt:562`) and health tanks (`LevelPauseScreen.kt:578`). | `013`, `095` |
| `O2-R9` | `partly` | Player life-cycle events carry `PlayerEventKeys.SLOT` (`Megaman.kt:377,386`). Lives are one shared count; the calls take a slot, but the value is not per slot (`GameState.kt:39,270`). `TURN_CONTROLLER_OFF` and `TURN_CONTROLLER_ON` have no slot, and act on all players (`LevelRoomHandler.kt:54`). Accepted gap (owner, 2026-11-01): in `047a`, only slot 0 submits `PLAYER_SPAWN`, because that event acts as a level reset until `038`. | `047`, `038` |
| `O2-R10` | `broken` (waiting) | 22 lines write a system `on` flag: 8 in `MegaLevelScreen.kt`, 6 in `PlayerStatsHandler.kt`, 2 in `LevelStateHandler.kt`, 2 in `MegamanMaverickGame.kt`, 3 in `WilyFinalBoss.kt`, 1 in `Laser.kt`. | `012` |

## Redo risk

**Short answer:** the merged player and input work (`029`, `031`, `061`, `084`, `016`, `014`, `062`) was the correct direction, and nothing there needs a rewrite. The real rework is in the camera (`051`, `016`), and it comes with phases 2 and 3. Most of the netplay work is new work that was already planned (`010`, `011`), not a redo of merged work.

| Merged work | What must change | Phase | Size |
|-------------|------------------|-------|------|
| `051` camera parts | The camera moves in the render path with a frame delta (`MegaLevelScreen.kt:286,921`). Gameplay reads the camera, so it must move on the tick, in the simulation. | 3 | One call site for the order and one for the speed, but a contract check of all `051` behavior, because cull and scroll timing can change. |
| `051` camera parts | `CameraController`, `CameraFollow.focus`, `CameraScroll` and `CameraEdgeBlocks` assume one view. Two views need one controller per view. Scroll deaths, edge blocks and crush surfaces need a rule for two views of one room. | 2 | This is the largest redo: 7 files in `screens/levels/camera/` and the part classes in `engine/.../camera/`. |
| `016` `MegaServices.gameCamera` | One view in the services holder must become a set of views. | 2 | 1 field. The helpers cover 162 `overlapsGameCamera` lines and the cull logic. 115 direct lines in 87 entity files need a review. |
| `031` input build | The frame is built once per render (`MegamanMaverickGame.kt:581-582`). It must be built once per tick, or `JUST_PRESSED` can be seen twice or lost. | 3 | 1 call site, plus the tick loop of `010`. `InputFrame` itself needs no change, and it is already the lockstep payload. |
| `031` input build | `MegaPlayers.menuInput` is the one place that chooses the slot for menus. `047a` added the slot builders. | 1 | Planned in `047`. 1 line for `menuInput`. This is the next step, not a redo. |
| `031f` system frame | `Esc` is in a system frame outside the slot frames (`game.systemInput`). A replay or a lockstep run must carry it too, or an `Esc` pause is lost. | 3 | 1 more `Long` per frame, in `055` and `010`. |
| Pause screen (not from a merged item) | Weapon and tank choices must become inputs. | 3 | 2 sites in `LevelPauseScreen.kt`. |

No redo found:

- `014`: contact order is ordered, and it stays stable.
- `062`, `063`, `064`: no effect on these rules.
- `061`, `084`: the player state is per entity, and `selectButtonAction` is per slot.
- `031e`: bindings stay out of the simulation.

Not a redo, but new work that is larger than the table shows:

- `010`: with a constant delta, the 278 entity files with `update(delta)` need no edit (inference). The work is in the 7 `fixedStep` reads, the 4 direct `deltaTime` reads, the event, spawn and input order, and the feel check against `MEDIUM`.
- `011`: 169 random-call lines in 56 entity files change to the new source.

## Items

| Item | Status | Note |
|------|--------|------|
| `010` | idea | [One simulation clock: fixed tick for gameplay, separate present phase](../items/010-fixed-tick/ITEM.md) (main) |
| `011` | planned | [One seedable, interceptable random source with named streams](../items/011-game-random/ITEM.md) (main) |
| `029` | done | [`IPlayer` contract, players service and `CharacterDef`](../items/029-player-contract/ITEM.md) (main) |
| `030` | planned | [Faction and root owner instead of `owner == megaman`](../items/030-faction/ITEM.md) (main) |
| `031` | done | [Input frames per player (`InputFrame`), devices and bindings](../items/031-input-frames/ITEM.md) (main) |
| `047` | active | [Co-op with multiple controllers](../items/047-co-op/ITEM.md) (main) |
| `059` | idea | [Max health read through from the profile](../items/059-max-health-read-through/ITEM.md) (main) |
| `061` | done | [Thin player view; locks and signals as components and functions](../items/061-thin-player/ITEM.md) (main) |
| `051` | done | [X-style camera bounds, auto-scroll and other camera behaviors](../items/051-camera-behaviors/ITEM.md) |
| `012` | planned | [Named, composable freezes with a reason table](../items/012-named-freezes/ITEM.md) |
| `013` | planned | [Level flow state machine; pause as an overlay](../items/013-level-flow-fsm/ITEM.md) |
| `016` | done | [Narrow services instead of `game` everywhere](../items/016-narrow-services/ITEM.md) |
| `019` | active | [One spawn phase per tick with destroy notifications](../items/019-spawn-phase/ITEM.md) |
| `020` | planned | [Cheaper broad phase with stable contact order](../items/020-broad-phase/ITEM.md) |
| `038` | planned | [One ordered respawn routine instead of an event chain](../items/038-respawn-routine/ITEM.md) |
| `055` | planned | [Record and replay for contract checks](../items/055-record-and-replay/ITEM.md) |
| `094` | idea | [Split screen (`O2` phase 2)](../items/094-split-screen/ITEM.md) (main) |
| `095` | idea | [Netplay with lockstep (`O2` phase 3)](../items/095-netplay/ITEM.md) (main) |
| `097` | idea | [Stable order for event keys in `EventsManager`](../items/097-stable-event-key-order/ITEM.md) (main) |
| `102` | idea | [A game mode concept](../items/102-game-modes/ITEM.md) |
| `108` | idea | [Spec Ops co-op missions](../items/108-spec-ops-mode/ITEM.md) (main) |
| `109` | idea | [PvP modes](../items/109-pvp-mode/ITEM.md) (main) |
| `110` | idea | [Co-op in the main campaign](../items/110-co-op-campaign/ITEM.md) (main) |
| `111` | idea | [PvP across multiple rooms](../items/111-pvp-multi-room/ITEM.md) (main) |
| `112` | planned | [Targeting as a policy that each entity owns](../items/112-targeting-policies/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-01: netplay targets lockstep. Rollback is a possible later upgrade.
- 2026-11-01: one fixed tick in all play (`O2-R3`), because the sources make a fixed step the base of the simulation,
  not a netplay mode.
- 2026-11-01: netplay players are fixed at session start.
- 2026-11-01: split-screen players stay in the same room.
- 2026-11-01: settings that change gameplay go to all peers at session start, and pause-menu actions are inputs.
- 2026-11-01: the first netplay target is the same build on the same platform.
- 2026-11-01: Spec Ops (`108`) and PvP (`109`) are the crux of this objective. They are separate from the main game,
  and they run on one machine or on the net.
- 2026-11-01: co-op in the main campaign is optional, and it comes after these modes (`110`). This replaces the goal of
  phase 1 that two players finish the main campaign.
- 2026-11-01: the first PvP target is one large room. Multi-room PvP comes after it (`111`).

## Open questions

- Does PvP change the netplay plan? `095` asks if PvP needs rollback.
- Does a lobby replace "players can join and leave during play" in the modes? See `102`.
- Does PvP need `094` from its start? A room larger than one view cannot show all players on one shared screen. See
  `109`.

- Phase 1 rules from `047` and `029`: the camera rule, the off-screen rule, who triggers a room transition, and what
  happens when one player dies. The owner leans to "the camera follows the lead player".
- Cross-platform netplay: is desktop against web a target? Sources warn that determinism across platforms is not
  guaranteed, and web needs its own transport.

## Sources

No libGDX guide to netplay was found on 2026-11-01. The libGDX sources below cover only sockets and fixed steps.

Read in full:

- [Fix Your Timestep!](https://gafferongames.com/post/fix_your_timestep/): constant steps, and rendering at any rate.
- [Deterministic Lockstep](https://gafferongames.com/post/deterministic_lockstep/): only inputs are sent, both sides
  need the same delta time, and results must match to the bit.
- [GGPO](https://www.ggpo.net/): rollback needs full determinism and also save and load of state.
- [libGDX networking wiki](https://libgdx.com/wiki/networking): TCP sockets do not work on GWT.

Search summaries only, not read in full:

- [libGDX Box2D wiki](https://libgdx.com/wiki/extensions/physics/box2d)
- [libGDX cookbook: fixed timestep](https://www.oreilly.com/library/view/libgdx-cross-platform-game/9781783287291/ch10s11.html)
- [gamedev.net: lockstep with libGDX and KryoNet](https://gamedev.net/forums/topic/708508-lockstep-protocol-for-multiplayer-reducing-latency/)
- [Lightyear deterministic replication](https://docs.rs/crate/lightyear_deterministic_replication/latest)
- [Deterministic Lockstep in Networked Games (thesis)](https://hdms.bsz-bw.de/files/7106/Deterministic_Lockstep_in_Networked_Games.pdf)
- [Chaotix Recompiled v0.13.0](https://github.com/YuutaTsubasa/ChaotixRecompiled/releases/tag/v0.13.0)
- [pinch-points `net.rs`](https://docs.rs/pinch-points/latest/src/pinch_points/sim/net.rs.html)

Project notes: `029` `NOTES.md` ("Co-op gaps and ideas", "Readiness for split screen and online play"), `031`
`NOTES.md`, `010` `NOTES.md`, `030` `NOTES.md`.
