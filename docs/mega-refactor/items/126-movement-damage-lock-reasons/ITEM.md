# 126: Movement and damage blocks as lock reasons

- Status: idea
- Type: refactor
- Objectives: `O1`, `O8`
- Depends on: `048` (the damage timer)
- Related: `081`, `084`
- Branches: none yet

## Goal

Make `LocksComponent` the one source of truth for the reasons that stop a player from moving. Stun, freeze and damage
take a `MOVEMENT` lock reason while they last, and `stunned` and `frozen` read the lock
(`getLockGroup(MOVEMENT).isLockedBy(STUN)`). Then the player rule `playerCanMove` (`081a`) becomes
`!isLocked(MOVEMENT)`, and a debug view can list why a player is blocked. The player class connects each mechanism to
its lock reason, so a character can differ (for example an X6 freeze that does not stop movement).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Start and end hooks on every path where a state ends: the stun timer end, `setToEnd`, `reset`, and the thaw of
  `FreezeComponent`. Today `StunComponent` has no end callback, and `reset()` and `setToEnd()` end a state with no
  write.
- The damage timer is in `AbstractHealthEntity`, so damage as a lock reason waits for `048`.
- `LocksComponent.reset()` does not run the change hook. A reason that a reset clears must run the hook, or the hook
  must not depend on it.
- `invincible` cannot be a lock only: "not live" and "not in play" are not locks. Only the damage and recovery terms
  can be lock reasons.
- `SmallIceCube.kt:228` shatters on a `DAMAGE` lock. A damage or recovery reason changes what it sees.
- The `048` decision that invincibility is never stored was made because the timers end with no write. With end
  hooks, the owner reviews it.

## Owner's input

2026-11-05: if hooks or callbacks come later, is `LocksComponent` the right place, and should the rules consolidate
there? The owner chose to keep this out of `081a` and agreed to this item.

## Open questions

1. Does `invincible` also move to the `DAMAGE` group for the damage and recovery terms, or only `canMove`?

## Sources

- `NOTES.md` in this folder.
- `081a` branch `docs/NOTES.md`, "Decisions" (the scan of the lock writers and readers).
