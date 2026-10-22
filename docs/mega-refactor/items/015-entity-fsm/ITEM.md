# 015: One entity state machine (enter/update/exit, state-owned timers)

- Status: planned
- Type: refactor
- Depends on: none
- Related: `006`, `010`, `013`, `029`, `042`
- Branches: none yet

## Goal

Give entities one state machine primitive, so that logic in sequential `if` statements and hand-run timers becomes declared states.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Add `Fsm<S>` in `engine/state` with guards, `any`, event transitions, state timers, an animation key and a debug log.
- Keep today's frame timing (same-frame change, no update on the entry frame, overflow discarded). Document it and test it.
- An adapter for `EnumStateMachineBuilder`. Migrate the `Loop`, `StateLoopHandler` and `StateMachineHandler` users. Delete the helpers.
- Optional one-level parent states (for the WilyFinalBoss phases).
- Do not force Megaman's movement into one machine. His behaviors are parallel.

## Owner's input

Answer to review item 12 (2026-10-22, verbatim):

> Yes, one mistake I made is not utilizes step machines enough. A lot of logic buried in sequential if statements, for loops, and in-code branches that would be much easier to change and maintain if done through state machines.

Answer to review item 14 (2026-10-22, verbatim):

> yes

## Open questions

- Is one level of hierarchy enough, or arbitrary nesting for future bosses?
- Must state machine data be in TMX or JSON soon, or code only for now?

## Sources

- `NOTES.md` in this folder.
