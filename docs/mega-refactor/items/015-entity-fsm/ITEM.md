# 015: One entity state machine (enter/update/exit, state-owned timers)

- Status: planned
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `006`, `010`, `013`, `029`, `042`, `077`, `079`, `118`
- Branches: none yet

## Goal

Give entities one state machine primitive, so that logic in sequential `if` statements and hand-run timers becomes declared states.

## Proposed scope

From the review, updated by the review of 2026-11-18 (`NOTES.md`). Not approved. The branch `SCOPE.md` decides the
final scope.

- Add `Fsm<S : Enum<S>>` in `engine/.../state/`. Each state has `onEnter`, `onUpdate` and `onExit`.
- `goTo(S)` is the main call. A table of allowed edges is part of the definition. A `goTo` on an edge that is not in
  the table throws (fail fast). `goTo(current)` runs exit and enter again (re-entry).
- Guarded transitions (`transition(from, to) { guard }`, first match wins) are a second form, on edges of the same
  table, for the old users that poll guards.
- `any(to) { guard }` for a global override, for example frozen or stunned.
- A state timer per state: the duration is a supplier read at enter (difficulty, flags), the state can hold the
  timer, and a time-in-state value is available. An animation key per state, as a string or a supplier. A debug log
  of each change, with a tag.
- Keep today's frame timing: the change happens in the same frame, the new state does not update in its entry frame,
  and the overflow is discarded. Allow a few changes in one frame, with a cap. Document it and test it.
- No allocation per frame (`O5-R1`). A read-only current state for tests.
- Composition in place of parent states: a state can run a child `Fsm` or a `Routine` (`118a`), and a `Step` can run
  an `Fsm`.
- Migrate the `Loop`-as-state users (30), the `StateLoopHandler` users (4) and the engine `StateMachine` users (24),
  one entity at a time. Keep the old `StateMachine` unchanged until its last user moves; no adapter. Then delete
  `StateLoopHandler` and the old classes. Delete `StateMachineHandler` at once (0 users).
- Leave out event transitions (`on(eventKey, ...)`) until a real caller needs them.
- Do not force the players' movement into one machine. `Megaman` and `MegamanX` run parallel behaviors.

## Owner's input

Answer to review item 12 (2026-10-22, verbatim):

> Yes, one mistake I made is not utilizes step machines enough. A lot of logic buried in sequential if statements, for loops, and in-code branches that would be much easier to change and maintain if done through state machines.

Answer to review item 14 (2026-10-22, verbatim):

> yes

## Trial candidate

The review of 2026-11-18 found a good first entity to move to `Fsm<S>`: the `Met` of branch
`mega-refactor-079a-smarter-met` (`entities/enemies/Met.kt` on that branch; on `master` after its merge). This is a
recommendation, not an owner decision.

Why:

- It has 7 states (`SHIELDING`, `POP_UP`, `RUNNING`, `JUMPING`, `STAGGERED`, `DESPERATE_RUN`, `DESPERATE_PAUSE`) on
  `EnumStateMachineBuilder`, and it is small (776 lines).
- It already has the shape of `Fsm<S>` by hand: an allowed-edge table (`TRANSITIONS`), a commanded change
  (`requestState`), one enter function (`enterState`, called from `onChangeState`), re-entry of `STAGGERED`, a frozen
  override at the top of the update, one timer reset per state, and an animation key per state.
- Its weak points are the ones that `Fsm<S>` fixes: a request for an edge that is not in the table is a silent no-op
  (not fail fast); the guard API is bypassed (each guard is `requestedState == to`, so the machine is a checked
  set-state); re-entry goes around the machine; timers, animation key and fixture flags are in separate `when` blocks.
- `079a` `TEST-DEBT.md` lists its rules in terms of states, so the move can be proved by tests that read the state.

The full table is in `NOTES.md`, "The `Met` of `079a`".

## Open questions

- Does a `goTo` inside `onUpdate` change the state at once (as `Met` and `D1000` do today, so the caller checks and
  returns), or at the end of the update?
- Composition (a state runs a child `Fsm` or a `Routine`) in place of `parent` states: is this enough for future
  bosses, also for the `WilyFinalBoss` phases?
- Old users: no adapter for `EnumStateMachineBuilder`; the old `StateMachine` stays until its last user moves. Is
  this period with two APIs acceptable?
- Is the state graph code only, with the values (durations, chances) in tuning rows as `BigAssMaverickRobotTuning`
  and `D1000Tuning` do? Or must the graph come from TMX or JSON (`O7`)?
- Which entity is the second caller: a hand-made machine (`HoverGunner`, `BigAssMaverickRobotHand`) or a `Loop`
  user (`Robbit`)?

## Sources

- `NOTES.md` in this folder.
