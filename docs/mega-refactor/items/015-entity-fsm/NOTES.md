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
