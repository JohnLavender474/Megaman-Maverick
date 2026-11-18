# Notes: 015 One entity state machine (enter/update/exit, state-owned timers)

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEMS 12 + 14: STATE MACHINES (one entity primitive and the level flow)".

````text
1. Current state
- `E/state/StateMachine.kt` supports guarded transitions where the first match wins, plus one callback, `onChangeState(current, previous)`. It has no enter, update or exit per state, no timers and no debug name. `E/common/objects/Loop.kt` is a cyclic list.
- Timers are kept beside the machine. Robbit (`C/entities/enemies/Robbit.kt:59-150`) has an enum, a `Loop` of states, a map from state to `Timer`, and a hand-written "timer just finished -> next -> apply jump impulse -> reset".
- SniperJoe (`C/entities/enemies/SniperJoe.kt:231-268, 391-440`) has an external `stateTimers` map and calls `stateMachine.next()` from 4 places in one update. Its `onChangeState` does exit work (`when (previous)`) and enter work (`when (current)`) in one function.
- WilyFinalBoss is hierarchical in practice. It has 3 phases (`:147`), one `StateMachine` per phase, stored as `StateMachine<*>` and cast at use (`:183, :406, :547`), and sub-phase enums inside states (`DivePhase`, `ChargePhase`, `LowSwayPhase`, `:2068-2074`).
- Megaman does not use an FSM. `BehaviorsComponent` runs 8 behaviors in parallel in insertion order (`entities/megaman/components/BehaviorsComponent.kt:778-785`), each with evaluate/init/act/end (`E/behaviors/AbstractBehaviorImpl.kt`). The animation key is a 20-branch priority `when` (`entities/megaman/sprites/MegamanAnimationKeySupplier.kt`).
- Level flow (item 12): `C/screens/levels/MegaLevelScreen.kt:906-913` runs a first-unfinished-wins `when` over 4 handlers (boss health, end level, player spawn, player death). The comment there says the order must not change. Pause is the `game.paused` flag, checked at `:886, :892, :896, :921, :963, :1016`. A 30-branch event `when` at `:529-873` drives the handlers.
- Timing fact: `Timer.update` clamps time to the duration (`E/common/time/Timer.kt:55`), so the overflow is lost. A state change happens in the same frame that the timer ends, and the new state does no work in that frame.

2. Options
A. Extend the engine `StateMachine` in place: add state objects with enter/update/exit and timers. Pro: the 25 users keep compiling. Con: the old `(Array<Any?>) -> Boolean` guard API and `onChangeState` stay.
B. Add a new `Fsm<S : Enum<S>>` in `E/state/` and keep the old API as a thin adapter. Then migrate users one at a time and delete `Loop`-as-FSM, `StateLoopHandler` and `StateMachineHandler`. Pro: one clean model. Con: a period with two APIs.
C. Use gdx-ai `DefaultStateMachine` or behavior trees. Pro: a known library. Con: no guards or timers, a new dependency, and behavior trees are too much here.

3. Recommendation
Option B. The primitive:
- `state(S) { onEnter {}; onUpdate { dt -> }; onExit {}; timer(dur) { timeout -> goTo(NEXT) }; anim = "key" or { key }; parent = P? }`
- `transition(from, to) { guard }`, checked in declared order with first match (as today).
- `any(to) { guard }` for global states such as FROZEN.
- `on(eventKey, from, to)` so events are inputs.
- `goTo(S)` runs exit then enter.
- A debug name. It logs "Robbit: STANDING -> CROUCHING (timeout)" through `GameLogger` with a tag.
- Frame order (must match today): (1) update the state timer, (2) check timeout, then guards, (3) if the state changed: exit old, enter new, and do not run the new state's `onUpdate` in this frame, (4) else run `onUpdate`.
- Timer overflow is discarded by default, which is today's behavior. Allow N transitions per frame with a cap, because SniperJoe chains them.
- Hierarchy: optional `parent`, one level. That is enough for Wily phase -> state -> sub-phase. Keep it flat by default.
- Do not force Megaman's movement into one FSM. His behaviors are parallel regions. The first step is only to move the animation key to `anim` on behaviors and states.

Sketch, Robbit:
Before (`Robbit.kt:133-150`):
```
robbitTimer.update(it)
if (robbitTimer.isJustFinished()) {
    val next = robbitLoop.next()
    if (next == JUMPING) { velocity.x = ...; velocity.y = ... }
    robbitTimer.reset()
}
```
After:
```
fsm = Fsm<RobbitState>("Robbit") {
    state(STANDING)  { timer(STAND_DUR)  { goTo(CROUCHING) }; anim = "stand"; onUpdate { faceMegaman() } }
    state(CROUCHING) { timer(CROUCH_DUR) { goTo(JUMPING) };   anim = "crouch"; onUpdate { faceMegaman() } }
    state(JUMPING)   { timer(JUMP_DUR)   { goTo(STANDING) };  anim = "jump";   onEnter { jump() } }
}
```
Note: today `facing` updates before the timer (`:96-97`). `onUpdate` runs after the transition check, so the face-update must be a pre-step, or the order changes by one frame. This is the kind of detail that a replay must check.

Sketch, level flow (item 12):
- States: `SPAWNING` (PlayerSpawnEventHandler) -> `PLAYING` <-> `ROOM_TRANSITION`.
- `PLAYING` -> `BOSS_INTRO` (BossSpawnEventHandler) -> `BOSS_HEALTH_FILL` -> `BOSS_FIGHT` -> `BOSS_DEFEATED` -> `VICTORY` (EndLevelEventHandler) -> `EXIT`.
- Any play state -> `DYING` (PlayerDeathEventHandler) -> `SPAWNING` or `GAME_OVER`.
- Events (`PLAYER_JUST_DIED`, `BOSS_READY`, `VICTORY_EVENT`) become `on(...)` transitions.
- Pause is an overlay stack above the flow, not a state. While an overlay is open, the flow's `update` is skipped, but UI update and draw continue. This replaces the `game.paused` checks.
- The exclusive `when` at `:910-913` becomes "only the current state's handler updates". The handlers do not change at first.

Data later: a per-entity table such as `STANDING: {duration: 0.75, next: CROUCHING, anim: stand}` and `JUMPING: {..., onEnter: jump}`. Action names map to code registered by the entity. Simple timed loops (Robbit, Shotman, most `Loop` users) fit this. Guards with logic stay in code.

Pitfalls:
- Over-engineering: no full UML statecharts, no history states, no parallel regions in the primitive.
- Timing: same-frame transition, no update on the entry frame, discarded overflow, update order relative to physics. Prove each migration with a replay trace (item 5).
- `onChangeState` users that do exit and enter work in one function must be split carefully.

4. Open questions for the owner
- Is one level of hierarchy enough, or do you want arbitrary nesting for future bosses?
- Must FSM data be in TMX/JSON soon, or is it code-only for now?
- In the level flow, must a boss fight and a room transition ever overlap, for example X-style camera zones during a boss?

5. Backlog wording
Item 14: "One entity state machine (enter/update/exit, state-owned timers)"
- Add `Fsm<S>` in `engine/state` with guards, `any`, event transitions, state timers, anim key and a debug log.
- Keep today's frame timing (same-frame change, no update on the entry frame, overflow discarded). Document it and test it.
- Adapter for `EnumStateMachineBuilder`. Migrate `Loop`/`StateLoopHandler`/`StateMachineHandler` users. Delete the helpers.
- Optional one-level parent states (for the WilyFinalBoss phases).

Item 12: "Level flow FSM; pause as overlay"
- `LevelFlow` states replace the ordered handler `when` at `MegaLevelScreen.kt:906-913`.
- Level events become FSM inputs.
- Pause, and later other menus, as an overlay stack. Remove the `game.paused` checks.

6. Dependencies
Item 5 (replay or trace to prove equality), 9 (tick definition), 11 (freezes set by flow states), 36/37 (respawn routine becomes the `DYING` -> `SPAWNING` path), 41 (Wily split uses this primitive), 28 (player states per player).
````

## 2026-11-18 review, `master` `cf4a1ab49`

A check of the 2026-10-22 design against `master` at `cf4a1ab49` (2026-11-18), and against the `Met` of branch
`mega-refactor-079a-smarter-met` at `655a6c182` (not merged). Counts are `grep` results, so they can include a few
comments. Paths without a module are under `core/src/main/kotlin/com/megaman/maverick/game/`. Recommendations are
inference unless a source is named.

### Recount of the old patterns

| Pattern | 2026-10-22 | 2026-11-18 | Files |
|---|---|---|---|
| Engine `StateMachine` users (`core/src/main`) | 24 or 25 | 24 | 14 with `EnumStateMachineBuilder`, 10 with `StateMachineBuilder` |
| `.next()` lines in these 24 files | not counted | 127 | most in `WilyFinalBoss` (19), `DesertMan` (15), `TimberWoman` (12) |
| `Loop` of an entity state | not counted | 30 | 7 more files use `Loop` for a non-state cycle (directions, colors, sprite index); 3 more are commented out (`Bospider`, `SphinxMiniBoss`, `MechaDragon_OLD`) |
| `StateLoopHandler` users | not counted | 4 | `CannoHoney`, `ChompFlower`, `FireFlower`, `SubmarineJoe` |
| `StateMachineHandler` users | not counted | 0 | added in `6ee7d998f` (2025-05-18) with no user; dead code |
| Enemy and boss files with `Timer(` and no machine or `Loop` | 82 (`O4-R3`, 2026-11-01) | 82 | of 164 files in `entities/enemies/` and `entities/bosses/` |
| Hand-made `goTo`/`setState` | 0 named | 11 functions in 9 files | see "New since the review" |

The engine package `engine/.../state/` changed once since the review: `118a` (`8ce479f1d`, 2026-11-02) added
`Routine` and `Step`. `StateMachine`, `StateMachineBuilder`, `EnumStateMachineBuilder`, `IState`, `DefaultStateImpl`
and `Transition` did not change.

### The old facts, checked again

- Still true: `StateMachine` has guarded transitions (first match wins) and one `onChangeState` callback, and no
  enter, update or exit per state (`engine/.../state/StateMachine.kt:6-40`, `IState.kt:5-12`).
- Still true: Robbit keeps a `Loop`, a timer map and a hand-written "timer done, next, jump, reset"
  (`entities/enemies/Robbit.kt:64-72`, `:133-150`). The face update before the timer is at `:134-135`.
- Still true: SniperJoe has an external `stateTimers` map (`SniperJoe.kt:157`) and calls `stateMachine.next()` from
  4 places in one update (`:249`, `:256`, `:265`, `:269`); `onChangeState` does exit and enter work (`:420`).
- Still true: `WilyFinalBoss` (3379 lines) keeps `StateMachine<*>` per phase (`WilyFinalBoss.kt:192`), with the
  enums at `:156-166` and handler objects per phase (`:195-199`).
- Still true: `Timer.update` clamps the time to the duration (`engine/.../common/time/Timer.kt:55`), so the
  overflow is lost.
- New fact, `O5-R1`: `StateMachine.next(params: Array<Any?> = Array())` (`StateMachine.kt:21`) and
  `IState.getNextState(params: Array<Any?> = Array())` (`IState.kt:11`) make a new LibGDX `Array` on each call
  with no argument. All 127 `.next()` lines use the default.
- Changed: the level flow `when` over 4 handlers (old `MegaLevelScreen.kt:906-913`) is gone. `render` now runs
  `respawn.update` (the `038` `RespawnRoutine`), `fights.update` (`118`) and `endLevelEventHandler.update`
  (`screens/levels/MegaLevelScreen.kt:855-866`). The `013` line numbers are stale.
- Still true: the players do not use one machine. `Megaman` and `MegamanX` both use the engine `BehaviorsComponent`
  with parallel behaviors; X builds its jump, wall slide and air dash from shared `players/` builders
  (`entities/megamanx/MegamanXMoves.kt:18-20`, `:170`, `:202`).

### New since the review

**1. New code does not use the engine machine.** Every entity with many states that was written or rewritten after
2026-11-05 has its own state code. Each one has the same shape: an enum, a `goTo(next)` or `setState(next)` that
sets the state and resets a time, a `when (next)` for the enter work, and a `when (state)` in the update.

| Entity | State code | Shape |
|---|---|---|
| `BigAssMaverickRobotHand` (`077a`) | `goTo` at `entities/bosses/BigAssMaverickRobotHand.kt:334` | 17 states in `bigassmaverickrobot/HandState.kt`, each with flags (`damager`, `platform`, `rocket`); `stateTime = 0f` |
| `D1000` (`081`) | `setState` at `entities/bosses/D1000.kt:466`, `goTo(step)` at `:484` | 10 states; 7 step enums (`:187-201`) for the steps inside a state; `step: Enum<*>` is untyped |
| `D1000Ram`, `D1000Crouch`, `D1000Ceiling`, `D1000Defeat`, `D1000Volley` | `goTo(Step)` in `bosses/d1000/` | each move owns a linear step enum and a clock |
| `D1000Core` | `setState` at `entities/bosses/D1000Core.kt:470` | 11 states; an `Int` `phase` inside a state |
| `HoverGunner` (`081`) | `setState` at `entities/enemies/HoverGunner.kt:253` | enter work resets one timer per state |
| `WavyTentacleOfJoints` | `setState` at `entities/special/WavyTentacleOfJoints.kt:107` | older (added 2026-03-18, last changed 2026-11-16) |
| `Met` (`079a`, branch only) | `requestState` and `enterState` | see "The `Met` of `079a`" |

`DesertMan` (`077b`) kept the engine `StateMachineBuilder` and grew to 1521 lines. Its guards draw random numbers in
declared order (`entities/bosses/DesertMan.kt:869-913`), so each guard that the machine tests uses one draw.

So the gap that `015` must close is now larger than the review saw. `O4-R3` ("new enemy and boss behavior uses the
common state machine") waits for `015`, and in the meantime each new entity makes its own machine. The `O4` current
state says that 0 enemy or boss files were added since 2026-11-01; that is no longer true (`D1000`, `D1000Core`,
`HoverGunner` and the `d1000/` moves).

**2. `Routine` and `Step` (`118a`).** `Routine<C>` runs a list of `Step<C>` in order (`begin`, `isFinished`,
`end(interrupted)`), and it moves through each step that finishes in the same tick (`engine/.../state/Routine.kt:13-28`).
Users: `fights/Fight.kt:33-34` and `screens/levels/respawn/RespawnRoutine.kt:61`. It is a linear sequence with no
branch and no loop. The `fights/` README rule says: "A loop is a state machine in a step". `118` `ITEM.md:45` names
`015` for this. So `Routine` does not replace `Fsm<S>`; the two compose. A linear step chain inside a state (for
example `D1000Crouch`: `START`, `DROP`, `HOLD`, `RISE`, `bosses/d1000/D1000Slams.kt:143-160`) is a `Routine` shape.

**3. Boss health phases (`077a`, `077b`).** `AbstractBoss.healthPhases`, `healthPhase` and `onHealthPhaseStart`
(`entities/contracts/AbstractBoss.kt:99-101`, `:236-248`) roll the bar to the next phase. The bosses use the phase
to pick a tuning row (`BigAssMaverickRobotTuning.of(hard, phase)`), not as a parent state. The `077` decision
(2026-11-13) has no rest between phases. So health phases do not need parent states. Only `WilyFinalBoss` has a
scripted transition between phases (`WilyPhaseTransState`, `WilyFinalBoss.kt:166`).

**4. The `BossComponent` phase (`118i`).** `ENTRANCE`, `INTRO`, `FIGHT` (`fights/README.md`, "The boss phase") is
moved by the boss and by a fight step. It is a separate, small machine with 2 writers. It is not a candidate for
`Fsm<S>` in the first branch.

**5. Tuning rows.** `BigAssMaverickRobotTuning`, `D1000Tuning` and `DesertManTuning` hold the values per difficulty
and health phase in one row object. The `bigassmaverickrobot/README.md` and `d1000/README.md` rules say: "A tuning
value is a field read. Do not add an `if (hard)`". This is the content-from-data path that exists today for state
durations and chances.

**6. Test rigs.** `BossRig`, `D1000Rig` and `DesertManRig` (`core/src/test/.../entities/bosses/`) drive the public
flow of a boss and read its state. The `079a` `TEST-DEBT.md` lists `Met` rules in terms of states (`STAGGERED`,
`DESPERATE_RUN`, `SHIELDING`). An entity test needs a read-only current state.

### The `Met` of `079a`

Source: `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/Met.kt` on
`mega-refactor-079a-smarter-met` at `655a6c182` (776 lines). Branch docs:
`items/079-smarter-enemies/branches/mega-refactor-079a-smarter-met/docs/SCOPE.md` and `TEST-DEBT.md`.

| Part | Where | What it shows for `015` |
|---|---|---|
| 7 states | `Met.kt:81` | `SHIELDING`, `POP_UP`, `RUNNING`, `JUMPING`, `STAGGERED`, `DESPERATE_RUN`, `DESPERATE_PAUSE` |
| Allowed-edge table | `TRANSITIONS`, `:142-158` | the edges are data; a guard is not needed for most of them |
| Request pattern | `requestState`, `:364-368`; guards `requestedState == to`, `:358-359` | a commanded `goTo` built on the guard API; an edge that is not in the table is a silent no-op |
| Enter work | `enterState`, `:370-403`, called from `onChangeState`, `:357` | `onEnter` per state |
| Re-entry | `stagger`, `:319-322`: `enterState(STAGGERED)` direct, because the table has no self edge | `goTo(current)` must run enter again |
| One shared timer | `stateTimer.resetDuration(...)` in each enter | per-state duration, read at enter; it depends on difficulty and on flags (`staggerFull`) |
| Held timer | `updateShielding`, `:449`: the timer ticks only on the ground and when the player does not shoot | a state timer that the state can hold |
| First-update work | `updatePopUp`, `:460`: `if (stateTimer.isAtBeginning()) shoot()` | work in the first update, after enter (it needs `facePlayer()` first) |
| Change during update | `:468`, `:491`, `:530`: `if (state != ...) return` after a helper that can request a state | an immediate `goTo` inside a helper; the caller checks whether the state changed |
| Global override | `:416-421`: frozen requests `SHIELDING` and skips the state update; `runOnly`, `:411-414`, skips the machine | `any(to)` plus a guard; a mode that bypasses the machine |
| Animation key | `:748-761`: `when (state)`, with flags (`helmetOn`, `staggerFull`, `blockedAhead`) | `anim` per state as a supplier, not only a fixed string |
| Fixture flags per state | `:699-700`: `SHIELD` on only in `SHIELDING` | data per state (as `HandState` flags) |
| Enter data | `jumpPlan` (`:384`) and `staggerFull` (`:389`) are set before the request | arguments to enter go in fields; no payload type is needed |

Weak points that `Fsm<S>` fixes:

- A request for an edge that is not in the table does nothing and logs nothing. This breaks the fail-fast rule
  (`docs/CODE_STYLE.md:53`).
- The engine guard API is bypassed: each guard is `requestedState == to`, so the machine is a checked set-state with
  a lambda per edge (22 lambdas).
- The self edge is missing, so `stagger` calls `enterState` around the machine, and `onChangeState` does not fire.
- `MetState` timers, the anim key and the fixture flags are in 3 separate `when` blocks that must stay in step.

### X6 source

`~/IdeaProjects/mmx-demake-tools/docs/engine.md`, "The standard main-object update": an X6 object update calls the
routine for `state` (`+0x04`); the main routine calls the routine for `step` (`+0x05`), and a step can have sub-steps
in `step2` (`+0x06`). Step 0 of every step table is the shared `HitReactionDispatch`. Facts only: X6 objects have
state, step and sub-step indices, and one shared reaction ahead of the steps.

### Each part of the proposed `Fsm<S>`, checked

| Part | Verdict | Reason |
|---|---|---|
| `onEnter`, `onUpdate`, `onExit` per state | keep | every new state code has enter work (`goTo` `when (next)`); exit work is rarer (`D1000.setState` drops a charge mark when it leaves `BOMB`, `D1000.kt:468`) |
| `goTo(S)` | keep, make it the main call | all new code commands the change; it does not poll guards |
| Allowed-edge table, fail fast | add | the `Met` table; a missing edge throws |
| Re-entry with `goTo(current)` | add | `Met.stagger`; `triggerChangeWhenSameElement(true)` in `RodentMan.kt:684`, `PreciousWoman.kt:737`, `MockingByte.kt:484` |
| `transition(from, to) { guard }`, first match | keep, second form | 24 old users and `DesertMan` poll guards; a guard edge is an edge of the same table |
| `any(to) { guard }` | keep | `Met` frozen; `D1000` stun from any action; X6 step 0 |
| `on(eventKey, from, to)` | drop for now | no new caller; entities call methods (`Met.onHitByProjectile`, `D1000.onCoreSignal`), and `118` keeps events as output only (`O1-R5`, `O1-R3`) |
| State timer | keep, change | duration as a supplier read at enter; a time-in-state value (`D1000` uses `time`); a way for the state to hold the timer (`Met` `SHIELDING`) |
| `anim` | keep, as string or supplier | the `Met` key reads flags; `D1000` sets the anim from steps |
| Debug log | keep | `Met`, `D1000`, `DesertMan` and `SniperJoe` each log the change by hand |
| One-level `parent` | replace with composition | the common shape is state plus step (`D1000` 7 step enums, `DesertMan` `PunchPhase`, X6 `step`); a state can own a child `Fsm` or a `Routine`, and a `Step` can own an `Fsm` (`fights/` rule). Health phases are tuning rows, not parents |
| Frame order: no update on the entry frame, overflow discarded | keep | `Met` and `D1000` do the same (the time resets to 0 at the change; the new state updates next frame) |
| N changes per frame with a cap | keep | `SniperJoe` chains; a cap catches a loop |
| Adapter for `EnumStateMachineBuilder` | drop | an adapter is a second API with one use; keep the old class unchanged until its last user moves, then delete it |
| Migrate `StateMachineHandler` | not needed | 0 users; delete it |
| Migrate `StateLoopHandler` and `Loop` as FSM | keep | 4 and 30 users; `Loop` stays for the 7 non-state cycles |
| Megaman out of scope | keep | true for `Megaman` and `MegamanX` |

### Missing from the 2026-10-22 design

- **No allocation per frame (`O5-R1`).** No default `Array()` argument, no lambda made per call, no boxing of the
  state. Per-state data in arrays by `ordinal` or an `ObjectMap` built once.
- **Random draws (`O2-R2`).** A guard that draws a random number uses one draw each time it is tested, so a new
  transition changes the random stream (`DesertMan`). Recommendation: the choice of the next attack is a weighted
  pick outside the guards, as `BigAssMaverickRobotTuning.attackFor` and `D1000Director` do. The machine only applies
  the result with `goTo`.
- **Fixed tick (`010`, `O2-R3`).** The state time counts `delta`. With a fixed tick it is deterministic. Ticks in
  place of seconds can wait for `010` and `140`.
- **Snapshot (`O2`, rollback or netcode later).** The machine state is the current state, the time in state and the
  timer. Keep it in plain fields, so that a later snapshot can copy it. The closures hold no state of their own.
- **Read for tests.** A read-only `state` for the rigs and for `testServices()` entity tests. `Fsm<S>` itself is an
  engine class with `engine:test` specs (as `RoutineTest` and `StateMachineBuilderTest`).
- **Data per state.** The state enum can carry flags (`HandState`); the machine does not need to own them.
- **Immediate or deferred change.** `Met` and `D1000` change at once inside a helper, and the caller checks after.
  The first branch must choose and document one rule (open question in `ITEM.md`).

### Recommendations (inference)

1. Keep option B (a new `Fsm<S>` in `engine/.../state/`), with `goTo` and the edge table first, guards second.
2. Use composition in place of `parent`: a state can run a child `Fsm` or a `Routine`.
3. Leave out `on(eventKey, ...)` and the `EnumStateMachineBuilder` adapter until a real caller needs them.
4. First caller: the `Met` of `079a` (after its merge). Second: one hand-made machine (`HoverGunner`, or
   `BigAssMaverickRobotHand`). Then the `Loop` users with a timer per state (`Robbit`, `Shotman` style).
5. Delete `StateMachineHandler` (0 users) in the first branch, or in `039`.
6. Until `015` merges, a new entity with states can copy the `Met` shape (enum, edge table, one `enterState`), so the
   later move is mechanical.
